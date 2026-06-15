package com.vn.sodu.request.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestAttachment;
import com.vn.sodu.request.RequestItem;
import com.vn.sodu.request.RequestSnapshot;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.RequestTimeline;
import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;
import com.vn.sodu.request.dto.UpdateRequestDto;
import com.vn.sodu.request.normalize.RequestNormalizer;
import com.vn.sodu.request.policy.RequestEditPolicy;
import com.vn.sodu.request.policy.RequestTransitionPolicy;
import com.vn.sodu.request.repo.RequestRepo;
import com.vn.sodu.request.repo.RequestSnapshotRepo;
import com.vn.sodu.request.repo.RequestTimelineRepo;
import com.vn.sodu.request.strategy.RequestStrategy;
import com.vn.sodu.request.strategy.RequestStrategyFactory;
import com.vn.sodu.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestWorkflowService {

    private static final DateTimeFormatter REQUEST_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RequestRepo requestRepo;
    private final RequestSnapshotRepo requestSnapshotRepo;
    private final RequestTimelineRepo requestTimelineRepo;
    private final RequestStrategyFactory requestStrategyFactory;
    private final RequestNormalizer requestNormalizer;
    private final RequestTransitionPolicy requestTransitionPolicy;
    private final RequestEditPolicy requestEditPolicy;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @Transactional
    public Request createRequest(CreateRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Create request payload is required");
        }
        CreateRequestDto normalized = requestNormalizer.normalize(dto);
        if (normalized == null || normalized.getType() == null) {
            throw new IllegalArgumentException("Request type is required");
        }
        if (normalized.getType() == OrderType.NORMAL) {
            throw new IllegalArgumentException("NORMAL requests are no longer created through the request flow. Create an order instead.");
        }
        RequestStrategy strategy = requestStrategyFactory.getStrategy(normalized.getType());
        strategy.validate(normalized);

        Request request = Request.builder()
                .requestCode(generateUniqueRequestCode())
                .customerPhone(normalized.getCustomerPhone())
                .type(normalized.getType())
                .status(strategy.initialStatus())
                .totalAmount(strategy.calculateTotal(normalized))
                .depositAmount(strategy.calculateDeposit(normalized))
                .customRequirements(normalized.getCustomRequirements())
                .build();

        applyItems(request, normalized.getItems());
        applyAttachments(request, normalized.getUploadedImageUrls());

        Request saved = requestRepo.save(request);
        appendTimeline(saved, "CREATE", null, saved.getStatus(), "system", "Request created");
        appendSnapshot(saved, "CREATE");
        return saved;
    }

    @Transactional
    public Request updateRequest(Long requestId, UpdateRequestDto dto) {
        return updateRequestInternal(requestId, dto, false);
    }

    @Transactional
    public Request updateRequestAsAdmin(Long requestId, UpdateRequestDto dto) {
        return updateRequestInternal(requestId, dto, true);
    }

    private Request updateRequestInternal(Long requestId, UpdateRequestDto dto, boolean adminOverrideAmounts) {
        if (dto == null) {
            throw new IllegalArgumentException("Update request payload is required");
        }
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        UpdateRequestDto normalized = requestNormalizer.normalize(dto);
        RequestStatus currentStatus = request.getStatus();

        if (normalized.getCustomerPhone() != null && !requestEditPolicy.canEditCustomerPhone(currentStatus)) {
            throw new IllegalStateException("Customer phone cannot be edited in status " + currentStatus);
        }
        if (normalized.getType() != null && !requestEditPolicy.canEditType(currentStatus)) {
            throw new IllegalStateException("Request type cannot be edited in status " + currentStatus);
        }
        if (normalized.getCustomRequirements() != null && !requestEditPolicy.canEditRequirements(currentStatus)) {
            throw new IllegalStateException("Custom requirements cannot be edited in status " + currentStatus);
        }
        if (normalized.getUploadedImageUrls() != null && !requestEditPolicy.canEditImages(currentStatus)) {
            throw new IllegalStateException("Images cannot be edited in status " + currentStatus);
        }
        if (normalized.getItems() != null && !requestEditPolicy.canEditItems(currentStatus)) {
            throw new IllegalStateException("Items cannot be edited in status " + currentStatus);
        }
        if (adminOverrideAmounts
                && (normalized.getTotalAmount() != null || normalized.getDepositAmount() != null)
                && requestEditPolicy.editableFields(currentStatus).isEmpty()) {
            throw new IllegalStateException("Amounts cannot be edited in status " + currentStatus);
        }

        OrderType effectiveType = normalized.getType() != null ? normalized.getType() : request.getType();
        RequestStrategy strategy = requestStrategyFactory.getStrategy(effectiveType);

        if (normalized.getCustomerPhone() != null) {
            request.setCustomerPhone(normalized.getCustomerPhone());
        }
        if (normalized.getType() != null) {
            request.setType(normalized.getType());
        }
        if (normalized.getCustomRequirements() != null) {
            request.setCustomRequirements(normalized.getCustomRequirements());
        }
        if (normalized.getItems() != null) {
            if (normalized.getItems().isEmpty()) {
                throw new IllegalArgumentException("At least one request item is required");
            }
            replaceItems(request, normalized.getItems());
        }
        if (normalized.getUploadedImageUrls() != null) {
            replaceAttachments(request, normalized.getUploadedImageUrls());
        }

        strategy.validate(toCreateRequestDto(request));
        recalculate(request, strategy);
        applyAdminAmountOverrides(request, normalized, adminOverrideAmounts);

        Request saved = requestRepo.save(request);
        appendTimeline(saved, "UPDATE", currentStatus, saved.getStatus(), "system", "Request updated");
        appendSnapshot(saved, "UPDATE");
        return saved;
    }

    @Transactional
    public Request processRequest(Long requestId, RequestStatus targetStatus, String actor, String note, BigDecimal depositAmount) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status is required");
        }
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        RequestStatus from = request.getStatus();
        requestTransitionPolicy.validateTransition(from, targetStatus);

        if (targetStatus == RequestStatus.APPROVED && depositAmount != null) {
            request.setDepositAmount(scaleMoney(depositAmount));
            BigDecimal totalAmount = scaleMoney(request.getTotalAmount());
            if (request.getDepositAmount().compareTo(totalAmount) > 0) {
                throw new IllegalArgumentException("Deposit amount must not exceed total amount");
            }
        }

        request.setStatus(targetStatus);
        Request saved = requestRepo.save(request);

        if (targetStatus == RequestStatus.APPROVED) {
            orderService.createFromApprovedRequest(saved);
        }

        appendTimeline(saved, "STATUS_CHANGE", from, targetStatus, actor, note);
        appendSnapshot(saved, "STATUS_CHANGE");
        return saved;
    }

    @Transactional
    public Request recalculate(Long requestId) {
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        RequestStrategy strategy = requestStrategyFactory.getStrategy(request.getType());
        recalculate(request, strategy);

        Request saved = requestRepo.save(request);
        appendTimeline(saved, "RECALCULATE", saved.getStatus(), saved.getStatus(), "system", "Request recalculated");
        appendSnapshot(saved, "RECALCULATE");
        return saved;
    }

    private void recalculate(Request request, RequestStrategy strategy) {
        CreateRequestDto currentDto = toCreateRequestDto(request);
        request.setTotalAmount(strategy.calculateTotal(currentDto));
        request.setDepositAmount(strategy.calculateDeposit(currentDto));
    }

    private void applyAdminAmountOverrides(Request request, UpdateRequestDto dto, boolean adminOverrideAmounts) {
        if (!adminOverrideAmounts || dto == null) {
            return;
        }

        if (dto.getTotalAmount() != null) {
            request.setTotalAmount(scaleMoney(dto.getTotalAmount()));
        }
        if (dto.getDepositAmount() != null) {
            request.setDepositAmount(scaleMoney(dto.getDepositAmount()));
        }

        BigDecimal totalAmount = scaleMoney(request.getTotalAmount());
        BigDecimal depositAmount = scaleMoney(request.getDepositAmount());
        if (depositAmount.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("Deposit amount must not exceed total amount");
        }
    }

    private void applyItems(Request request, List<RequestItemDto> itemDtos) {
        if (request.getItems() == null) {
            request.setItems(new ArrayList<>());
        }
        request.getItems().clear();
        for (RequestItemDto dto : itemDtos) {
            RequestItem item = RequestItem.builder()
                    .request(request)
                    .nhanhProductId(dto.getNhanhProductId())
                    .name(dto.getName())
                    .note(dto.getNote())
                    .metadataJson(dto.getMetadataJson())
                    .price(dto.getPrice())
                    .quantity(dto.getQuantity())
                    .build();
            request.getItems().add(item);
        }
    }

    private void replaceItems(Request request, List<RequestItemDto> itemDtos) {
        applyItems(request, itemDtos);
    }

    private void applyAttachments(Request request, List<String> urls) {
        if (request.getAttachments() == null) {
            request.setAttachments(new ArrayList<>());
        }
        request.getAttachments().clear();
        if (urls == null) {
            return;
        }

        int index = 0;
        for (String url : urls) {
            RequestAttachment attachment = RequestAttachment.builder()
                    .request(request)
                    .url(url)
                    .type("IMAGE")
                    .sortOrder(index++)
                    .build();
            request.getAttachments().add(attachment);
        }
    }

    private void replaceAttachments(Request request, List<String> urls) {
        request.getAttachments().clear();
        applyAttachments(request, urls);
    }

    private void appendSnapshot(Request request, String snapshotType) {
        RequestSnapshot snapshot = RequestSnapshot.builder()
                .request(request)
                .snapshotType(snapshotType)
                .snapshotJson(toSnapshotJson(request))
                .build();
        requestSnapshotRepo.save(snapshot);
    }

    private void appendTimeline(Request request, String action, RequestStatus from, RequestStatus to, String actor, String note) {
        RequestTimeline timeline = RequestTimeline.builder()
                .request(request)
                .action(action)
                .fromStatus(from)
                .toStatus(to)
                .actor(normalizeActor(actor))
                .note(note)
                .build();
        requestTimelineRepo.save(timeline);
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateUniqueRequestCode() {
        for (int i = 0; i < 20; i++) {
            String code = "SOBU-REQ-" + LocalDateTime.now().format(REQUEST_CODE_FORMATTER) + "-" + String.format("%04d", RANDOM.nextInt(10_000));
            Optional<Request> existing = requestRepo.findByRequestCode(code);
            if (existing.isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique request code");
    }

    private CreateRequestDto toCreateRequestDto(Request request) {
        List<RequestItemDto> itemDtos = request.getItems() == null
                ? List.of()
                : request.getItems().stream().map(this::toRequestItemDto).collect(Collectors.toList());

        List<String> urls = request.getAttachments() == null
                ? List.of()
                : request.getAttachments().stream()
                .map(RequestAttachment::getUrl)
                .collect(Collectors.toList());

        return CreateRequestDto.builder()
                .customerPhone(request.getCustomerPhone())
                .type(request.getType())
                .items(itemDtos)
                .customRequirements(request.getCustomRequirements())
                .uploadedImageUrls(urls)
                .build();
    }

    private RequestItemDto toRequestItemDto(RequestItem item) {
        return RequestItemDto.builder()
                .nhanhProductId(item.getNhanhProductId())
                .name(item.getName())
                .note(item.getNote())
                .metadataJson(item.getMetadataJson())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }

    private String toSnapshotJson(Request request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", request.getId());
        snapshot.put("requestCode", request.getRequestCode());
        snapshot.put("customerPhone", request.getCustomerPhone());
        snapshot.put("status", request.getStatus() == null ? null : request.getStatus().name());
        snapshot.put("type", request.getType() == null ? null : request.getType().name());
        snapshot.put("totalAmount", request.getTotalAmount());
        snapshot.put("depositAmount", request.getDepositAmount());
        snapshot.put("customRequirements", request.getCustomRequirements());
        snapshot.put("items", request.getItems() == null ? List.of() : request.getItems().stream().map(this::toItemSnapshot).toList());
        snapshot.put("attachments", request.getAttachments() == null ? List.of() : request.getAttachments().stream().map(this::toAttachmentSnapshot).toList());

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request snapshot", ex);
        }
    }

    private Map<String, Object> toItemSnapshot(RequestItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", item.getId());
        data.put("nhanhProductId", item.getNhanhProductId());
        data.put("name", item.getName());
        data.put("note", item.getNote());
        data.put("metadataJson", item.getMetadataJson());
        data.put("price", item.getPrice());
        data.put("quantity", item.getQuantity());
        return data;
    }

    private Map<String, Object> toAttachmentSnapshot(RequestAttachment attachment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", attachment.getId());
        data.put("url", attachment.getUrl());
        data.put("type", attachment.getType());
        data.put("mimeType", attachment.getMimeType());
        data.put("size", attachment.getSize());
        data.put("sortOrder", attachment.getSortOrder());
        return data;
    }
}
