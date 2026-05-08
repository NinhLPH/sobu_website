package com.vn.sodu.request.normalize;

import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;
import com.vn.sodu.request.dto.UpdateRequestDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class RequestNormalizer {

    public CreateRequestDto normalize(CreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return CreateRequestDto.builder()
                .customerPhone(normalizeText(dto.getCustomerPhone()))
                .type(dto.getType())
                .items(normalizeItems(dto.getItems()))
                .customRequirements(normalizeRichText(dto.getCustomRequirements()))
                .uploadedImageUrls(normalizeAttachmentUrls(dto.getUploadedImageUrls()))
                .build();
    }

    public UpdateRequestDto normalize(UpdateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return UpdateRequestDto.builder()
                .customerPhone(normalizeText(dto.getCustomerPhone()))
                .type(dto.getType())
                .items(dto.getItems() == null ? null : normalizeItems(dto.getItems()))
                .customRequirements(normalizeRichText(dto.getCustomRequirements()))
                .uploadedImageUrls(dto.getUploadedImageUrls() == null ? null : normalizeAttachmentUrls(dto.getUploadedImageUrls()))
                .build();
    }

    public List<RequestItemDto> normalizeItems(List<RequestItemDto> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<String, RequestItemDto> merged = new LinkedHashMap<>();

        for (RequestItemDto item : items) {
            if (item == null) {
                continue;
            }

            String name = normalizeText(item.getName());
            Integer quantity = item.getQuantity();
            if (name == null || quantity == null || quantity <= 0) {
                continue;
            }

            String productId = normalizeText(item.getNhanhProductId());
            String note = normalizeText(item.getNote());
            String metadataJson = normalizeText(item.getMetadataJson());
            String key = buildItemKey(productId, name, note, metadataJson);

            RequestItemDto normalized = RequestItemDto.builder()
                    .nhanhProductId(productId)
                    .name(name)
                    .note(note)
                    .metadataJson(metadataJson)
                    .price(item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) >= 0 ? item.getPrice() : null)
                    .quantity(quantity)
                    .build();

            RequestItemDto existing = merged.get(key);
            if (existing == null) {
                merged.put(key, normalized);
                continue;
            }

            existing.setQuantity(existing.getQuantity() + normalized.getQuantity());
            if (existing.getPrice() == null && normalized.getPrice() != null) {
                existing.setPrice(normalized.getPrice());
            }
        }

        return new ArrayList<>(merged.values());
    }

    public List<String> normalizeAttachmentUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String url : urls) {
            String normalized = normalizeText(url);
            if (normalized == null || result.contains(normalized)) {
                continue;
            }
            result.add(normalized);
        }
        return result;
    }

    private String buildItemKey(String productId, String name, String note, String metadataJson) {
        if (productId != null) {
            return "pid:" + productId;
        }
        return String.join("|",
                Objects.toString(name, ""),
                Objects.toString(note, ""),
                Objects.toString(metadataJson, ""));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String normalizeRichText(String value) {
        String cleaned = normalizeText(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.replaceAll("(?is)<script.*?>.*?</script>", "");
        cleaned = cleaned.replaceAll("(?is)<style.*?>.*?</style>", "");
        cleaned = cleaned.replaceAll("<[^>]+>", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
