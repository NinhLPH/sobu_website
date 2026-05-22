package com.vn.sodu.request.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.order.OrderService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestAttachment;
import com.vn.sodu.request.RequestItem;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;
import com.vn.sodu.request.dto.UpdateRequestDto;
import com.vn.sodu.request.normalize.RequestNormalizer;
import com.vn.sodu.request.policy.RequestEditPolicy;
import com.vn.sodu.request.policy.RequestTransitionPolicy;
import com.vn.sodu.request.repo.RequestRepo;
import com.vn.sodu.request.repo.RequestSnapshotRepo;
import com.vn.sodu.request.repo.RequestTimelineRepo;
import com.vn.sodu.request.strategy.CustomRequestStrategy;
import com.vn.sodu.request.strategy.FindingRequestStrategy;
import com.vn.sodu.request.strategy.NormalRequestStrategy;
import com.vn.sodu.request.strategy.PreOrderRequestStrategy;
import com.vn.sodu.request.strategy.RequestStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestWorkflowServiceTest {

        @Mock
        private RequestRepo requestRepo;

        @Mock
        private RequestSnapshotRepo requestSnapshotRepo;

        @Mock
        private RequestTimelineRepo requestTimelineRepo;

        @Mock
        private OrderService orderService;

        private RequestWorkflowService service;

        @BeforeEach
        void setUp() {
                RequestStrategyFactory factory = new RequestStrategyFactory(
                                new NormalRequestStrategy(),
                                new PreOrderRequestStrategy(),
                                new FindingRequestStrategy(),
                                new CustomRequestStrategy());
                service = new RequestWorkflowService(
                                requestRepo,
                                requestSnapshotRepo,
                                requestTimelineRepo,
                                factory,
                                 new RequestNormalizer(),
                                 new RequestTransitionPolicy(),
                                 new RequestEditPolicy(),
                                 new ObjectMapper(), orderService);
                when(requestRepo.findByRequestCode(anyString())).thenReturn(Optional.empty());
                when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void createRequestNormalizesAndPersistsData() {
                CreateRequestDto dto = CreateRequestDto.builder()
                                .customerPhone(" 0123456789 ")
                                .type(OrderType.NORMAL)
                                .customRequirements("  <p>Need fast delivery</p> ")
                                .uploadedImageUrls(List.of(" https://img/1 ", "https://img/1", "https://img/2"))
                                .items(List.of(
                                                RequestItemDto.builder()
                                                                .nhanhProductId(" P1 ")
                                                                .name(" Item A ")
                                                                .note(" note ")
                                                                .price(new BigDecimal("100"))
                                                                .quantity(1)
                                                                .build(),
                                                RequestItemDto.builder()
                                                                .nhanhProductId("P1")
                                                                .name("Item A")
                                                                .note("note")
                                                                .price(new BigDecimal("100"))
                                                                .quantity(1)
                                                                .build()))
                                .build();

                Request result = service.createRequest(dto);

                assertThat(result.getStatus()).isEqualTo(RequestStatus.REVIEWING);
                assertThat(result.getCustomerPhone()).isEqualTo("0123456789");
                assertThat(result.getCustomRequirements()).isEqualTo("Need fast delivery");
                assertThat(result.getTotalAmount()).isEqualByComparingTo("200.00");
                assertThat(result.getDepositAmount()).isEqualByComparingTo("0.00");
                assertThat(result.getItems()).hasSize(1);
                assertThat(result.getAttachments()).hasSize(2);
                verify(requestSnapshotRepo).save(any());
                verify(requestTimelineRepo).save(any());
        }

        @Test
        void createRequestRejectsNullPayload() {
                assertThrows(IllegalArgumentException.class, () -> service.createRequest(null));
        }

        @Test
        void updateRequestAllowsEditableFieldsAndRecalculates() {
                Request existing = Request.builder()
                                .id(10L)
                                .requestCode("SOBU-REQ-20260507210000-0001")
                                .customerPhone("0123456789")
                                .type(OrderType.NORMAL)
                                .status(RequestStatus.REVIEWING)
                                .items(new java.util.ArrayList<>(List.of(
                                                RequestItem.builder()
                                                                .name("Item A")
                                                                .note("note")
                                                                .metadataJson(null)
                                                                .price(new BigDecimal("100"))
                                                                .quantity(1)
                                                                .build())))
                                .attachments(new java.util.ArrayList<>())
                                .build();

                attachBackReferences(existing);
                when(requestRepo.findById(10L)).thenReturn(Optional.of(existing));

                UpdateRequestDto dto = UpdateRequestDto.builder()
                                .customerPhone(" 0123456780 ")
                                .customRequirements("  <div>Updated requirements</div> ")
                                .uploadedImageUrls(List.of(" https://img/3 "))
                                .items(List.of(
                                                RequestItemDto.builder()
                                                                .name("Item A")
                                                                .note("note")
                                                                .price(new BigDecimal("150"))
                                                                .quantity(2)
                                                                .build()))
                                .build();

                Request result = service.updateRequest(10L, dto);

                assertThat(result.getCustomerPhone()).isEqualTo("0123456780");
                assertThat(result.getCustomRequirements()).isEqualTo("Updated requirements");
                assertThat(result.getTotalAmount()).isEqualByComparingTo("300.00");
                assertThat(result.getItems()).hasSize(1);
                assertThat(result.getAttachments()).hasSize(1);
                verify(requestSnapshotRepo).save(any());
                verify(requestTimelineRepo).save(any());
        }

        @Test
        void updateRequestRejectsApprovedRequests() {
                Request existing = Request.builder()
                                .id(14L)
                                .requestCode("SOBU-REQ-20260507210000-0005")
                                .customerPhone("0123456789")
                                .type(OrderType.NORMAL)
                                .status(RequestStatus.APPROVED)
                                .items(new java.util.ArrayList<>(List.of(
                                                RequestItem.builder()
                                                                .name("Item A")
                                                                .note("note")
                                                                .price(new BigDecimal("100"))
                                                                .quantity(1)
                                                                .build())))
                                .attachments(new java.util.ArrayList<>())
                                .build();

                attachBackReferences(existing);
                when(requestRepo.findById(14L)).thenReturn(Optional.of(existing));

                UpdateRequestDto dto = UpdateRequestDto.builder()
                                .customerPhone("0123456780")
                                .build();

                assertThrows(IllegalStateException.class, () -> service.updateRequest(14L, dto));
        }

        @Test
        void processRequestChangesStatusAndWritesTimeline() {
                Request existing = Request.builder()
                                .id(11L)
                                .requestCode("SOBU-REQ-20260507210000-0002")
                                .customerPhone("0123456789")
                                .type(OrderType.FINDING)
                                .status(RequestStatus.REVIEWING)
                                .items(new java.util.ArrayList<>(List.of(
                                                RequestItem.builder()
                                                                .name("Item A")
                                                                .note("note")
                                                                .price(null)
                                                                .quantity(1)
                                                                .build())))
                                .attachments(new java.util.ArrayList<>())
                                .build();

                attachBackReferences(existing);
                when(requestRepo.findById(11L)).thenReturn(Optional.of(existing));

                Request result = service.processRequest(11L, RequestStatus.WAITING_CUSTOMER, "admin",
                                "Need more detail");

                assertThat(result.getStatus()).isEqualTo(RequestStatus.WAITING_CUSTOMER);
                 verify(requestTimelineRepo).save(any());
                 verify(requestSnapshotRepo).save(any());
        }

        @Test
        void processRequestCreatesOrderWhenApproved() {
                Request existing = Request.builder()
                                .id(15L)
                                .requestCode("SOBU-REQ-20260507210000-0006")
                                .customerPhone("0123456789")
                                .type(OrderType.NORMAL)
                                .status(RequestStatus.REVIEWING)
                                .items(new java.util.ArrayList<>(List.of(
                                                RequestItem.builder()
                                                                .nhanhProductId("P1")
                                                                .name("Item A")
                                                                .note("note")
                                                                .price(new BigDecimal("100"))
                                                                .quantity(1)
                                                                .build())))
                                .attachments(new java.util.ArrayList<>())
                                .build();

                attachBackReferences(existing);
                when(requestRepo.findById(15L)).thenReturn(Optional.of(existing));

                Request result = service.processRequest(15L, RequestStatus.APPROVED, "admin", "Approved");

                assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
                verify(orderService).createFromApprovedRequest(result);
                verify(requestTimelineRepo).save(any());
                verify(requestSnapshotRepo).save(any());
        }

        @Test
        void processRequestRejectsNullTargetStatus() {
                assertThrows(IllegalArgumentException.class, () -> service.processRequest(11L, null, "admin", "note"));
        }

        @Test
        void recalculateUsesCurrentStrategy() {
                Request existing = Request.builder()
                                .id(12L)
                                .requestCode("SOBU-REQ-20260507210000-0003")
                                .customerPhone("0123456789")
                                .type(OrderType.PREORDER)
                                .status(RequestStatus.SOURCING)
                                .items(new java.util.ArrayList<>(List.of(
                                                RequestItem.builder()
                                                                .name("Item A")
                                                                .note("note")
                                                                .price(new BigDecimal("100"))
                                                                .quantity(2)
                                                                .build(),
                                                RequestItem.builder()
                                                                .name("Item B")
                                                                .note("note")
                                                                .price(new BigDecimal("50"))
                                                                .quantity(1)
                                                                .build())))
                                .attachments(new java.util.ArrayList<>())
                                .build();

                attachBackReferences(existing);
                when(requestRepo.findById(12L)).thenReturn(Optional.of(existing));

                Request result = service.recalculate(12L);

                assertThat(result.getTotalAmount()).isEqualByComparingTo("250.00");
                assertThat(result.getDepositAmount()).isEqualByComparingTo("75.00");
                verify(requestSnapshotRepo).save(any());
                verify(requestTimelineRepo).save(any());
        }

        @Test
        void processRequestRejectsInvalidTransition() {
                Request existing = Request.builder()
                                .id(13L)
                                .requestCode("SOBU-REQ-20260507210000-0004")
                                .customerPhone("0123456789")
                                .type(OrderType.CUSTOM)
                                .status(RequestStatus.APPROVED)
                                .items(new java.util.ArrayList<>(List.of(
                                                RequestItem.builder()
                                                                .name("Item A")
                                                                .note("note")
                                                                .quantity(1)
                                                                .build())))
                                .attachments(new java.util.ArrayList<>())
                                .build();

                attachBackReferences(existing);
                when(requestRepo.findById(13L)).thenReturn(Optional.of(existing));

                assertThrows(IllegalStateException.class,
                                () -> service.processRequest(13L, RequestStatus.REVIEWING, "admin", "reopen"));
        }

        private void attachBackReferences(Request request) {
                if (request.getItems() != null) {
                        for (RequestItem item : request.getItems()) {
                                item.setRequest(request);
                        }
                }
                if (request.getAttachments() != null) {
                        for (RequestAttachment attachment : request.getAttachments()) {
                                attachment.setRequest(request);
                        }
                }
        }
}
