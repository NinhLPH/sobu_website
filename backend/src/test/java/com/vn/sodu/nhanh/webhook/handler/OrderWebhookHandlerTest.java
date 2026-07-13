package com.vn.sodu.nhanh.webhook.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus;
import com.vn.sodu.nhanh.webhook.NhanhOrderStatusMapper;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_ADD;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_DELETE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_UPDATE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_PARTIAL_RETURN;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_ADD;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.WEBHOOKS_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderWebhookHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Captor
    private ArgumentCaptor<Order> captor;

    private ObjectMapper objectMapper;
    private OrderWebhookHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new OrderWebhookHandler(orderRepository, objectMapper);
    }

    @Test
    void supportsOrderEvents() {
        assertThat(handler.supports(ORDER_ADD)).isTrue();
        assertThat(handler.supports(ORDER_UPDATE)).isTrue();
        assertThat(handler.supports(ORDER_DELETE)).isTrue();
    }

    @Test
    void doesNotSupportOtherEvents() {
        assertThat(handler.supports(ORDER_PARTIAL_RETURN)).isFalse();
        assertThat(handler.supports(PRODUCT_ADD)).isFalse();
        assertThat(handler.supports(WEBHOOKS_ENABLED)).isFalse();
    }

    @Test
    void handlesOrderAdd() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .appOrderId("SOBU-ORD-001")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.PENDING)
                .build();

        when(orderRepository.findByAppOrderId("SOBU-ORD-001")).thenReturn(Optional.of(existingOrder));

        String json = """
                {"info":{"id":12345},"channel":{"appOrderId":"SOBU-ORD-001"}}
                """;
        handler.handle(eventLog(ORDER_ADD), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getNhanhOrderId()).isEqualTo("12345");
        assertThat(saved.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
    }

    @Test
    void handlesOrderAddSkippedWhenAlreadySynced() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .appOrderId("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findByAppOrderId("SOBU-ORD-001")).thenReturn(Optional.of(existingOrder));

        String json = """
                {"info":{"id":12345},"channel":{"appOrderId":"SOBU-ORD-001"}}
                """;
        handler.handle(eventLog(ORDER_ADD), objectMapper.readTree(json));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlesOrderUpdateStatus() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        // Nhanh status 60 = Success → DELIVERED
        String json = """
                {"info":{"id":12345,"status":60}}
                """;
        handler.handle(eventLog(ORDER_UPDATE), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void handlesOrderUpdateShippingStatus() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        // Nhanh status 59 = Shipping → SHIPPED
        String json = """
                {"info":{"id":12345,"status":59}}
                """;
        handler.handle(eventLog(ORDER_UPDATE), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void handlesOrderUpdateCancelledStatus() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        // Nhanh status 63 = CustomerCancel → CANCELLED
        String json = """
                {"info":{"id":12345,"status":63}}
                """;
        handler.handle(eventLog(ORDER_UPDATE), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void handlesOrderUpdateFailedStatus() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        // Nhanh status 61 = Failed → FAILED
        String json = """
                {"info":{"id":12345,"status":61}}
                """;
        handler.handle(eventLog(ORDER_UPDATE), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void handlesOrderUpdateReturnedStatus() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        // Nhanh status 72 = Returned → RETURNED
        String json = """
                {"info":{"id":12345,"status":72}}
                """;
        handler.handle(eventLog(ORDER_UPDATE), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    @Test
    void handlesOrderDelete() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        var data = objectMapper.createArrayNode();
        data.add(12345);

        handler.handle(eventLog(ORDER_DELETE), data);

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void handlesOrderDeleteMultiple() throws Exception {
        Order order1 = Order.builder().id(1L).orderCode("ORD-1").nhanhOrderId("100").status(OrderStatus.PROCESSING).build();
        Order order2 = Order.builder().id(2L).orderCode("ORD-2").nhanhOrderId("101").status(OrderStatus.PROCESSING).build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("100")).thenReturn(Optional.of(order1));
        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("101")).thenReturn(Optional.of(order2));

        var data = objectMapper.createArrayNode();
        data.add(100);
        data.add(101);

        handler.handle(eventLog(ORDER_DELETE), data);

        verify(orderRepository).save(order1);
        verify(orderRepository).save(order2);
        assertThat(order1.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order2.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void orderUpdateWithCarrierInfo() throws Exception {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-001")
                .nhanhOrderId("12345")
                .status(OrderStatus.PROCESSING)
                .syncStatus(OrderSyncStatus.SYNCED)
                .build();

        when(orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode("12345"))
                .thenReturn(Optional.of(existingOrder));

        String json = """
                {"info":{"id":12345,"status":59},"carrier":{"id":29,"trackingUrl":"https://track.example.com/abc"}}
                """;
        handler.handle(eventLog(ORDER_UPDATE), objectMapper.readTree(json));

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getCarrierId()).isEqualTo(29);
        assertThat(captor.getValue().getTrackingUrl()).isEqualTo("https://track.example.com/abc");
    }

    @Test
    void skipsOrderAddWhenAppOrderIdMissing() throws Exception {
        String json = """
                {"info":{"id":12345},"channel":{}}
                """;
        handler.handle(eventLog(ORDER_ADD), objectMapper.readTree(json));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void skipsOrderDeleteWhenDataIsNotArray() throws Exception {
        var data = objectMapper.createObjectNode();
        data.put("id", 100);

        handler.handle(eventLog(ORDER_DELETE), data);

        verify(orderRepository, never()).findWithItemsAndRequestByNhanhOrderIdOrCode(anyString());
    }

    @Test
    void mapToLocalReturnsAllKnownStatuses() {
        assertThat(NhanhOrderStatusMapper.mapToLocal(54)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(NhanhOrderStatusMapper.mapToLocal(55)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(NhanhOrderStatusMapper.mapToLocal(56)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(NhanhOrderStatusMapper.mapToLocal(57)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(NhanhOrderStatusMapper.mapToLocal(58)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(59)).isEqualTo(OrderStatus.SHIPPED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(60)).isEqualTo(OrderStatus.DELIVERED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(61)).isEqualTo(OrderStatus.FAILED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(63)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(64)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(68)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(71)).isEqualTo(OrderStatus.SHIPPED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(72)).isEqualTo(OrderStatus.RETURNED);
        assertThat(NhanhOrderStatusMapper.mapToLocal(73)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(NhanhOrderStatusMapper.mapToLocal(74)).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void mapToLocalReturnsNullForUnknownCode() {
        assertThat(NhanhOrderStatusMapper.mapToLocal(999)).isNull();
    }

    private NhanhWebhookEventLog eventLog(NhanhWebhookEvent event) {
        return NhanhWebhookEventLog.builder()
                .eventName(event.eventName())
                .eventType(event.name())
                .businessId("10000")
                .rawPayload("{}")
                .status(NhanhWebhookEventLogStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
