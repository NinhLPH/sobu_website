package com.vn.sodu.nhanh.webhook.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.service.ProductSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.INVENTORY_CHANGE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_ADD;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_DELETE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductWebhookHandlerTest {

    @Mock
    private ProductSyncService productSyncService;

    @Mock
    private ProductRepo productRepo;

    private ObjectMapper objectMapper;
    private ProductWebhookHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new ProductWebhookHandler(productSyncService, productRepo, objectMapper);
    }

    @Test
    void supportsProductEvents() {
        assertThat(handler.supports(NhanhWebhookEvent.PRODUCT_ADD)).isTrue();
        assertThat(handler.supports(NhanhWebhookEvent.PRODUCT_UPDATE)).isTrue();
        assertThat(handler.supports(NhanhWebhookEvent.PRODUCT_DELETE)).isTrue();
        assertThat(handler.supports(NhanhWebhookEvent.INVENTORY_CHANGE)).isTrue();
    }

    @Test
    void doesNotSupportOtherEvents() {
        assertThat(handler.supports(NhanhWebhookEvent.ORDER_ADD)).isFalse();
        assertThat(handler.supports(NhanhWebhookEvent.ORDER_UPDATE)).isFalse();
        assertThat(handler.supports(NhanhWebhookEvent.ORDER_DELETE)).isFalse();
        assertThat(handler.supports(NhanhWebhookEvent.ORDER_PARTIAL_RETURN)).isFalse();
        assertThat(handler.supports(NhanhWebhookEvent.PAYMENT_RECEIVED)).isFalse();
        assertThat(handler.supports(NhanhWebhookEvent.WEBHOOKS_ENABLED)).isFalse();
        assertThat(handler.supports(NhanhWebhookEvent.APP_UNINSTALLED)).isFalse();
    }

    @Test
    void handlesProductAdd() throws Exception {
        String json = """
                {"id":12345,"code":"SP001","name":"Test Product","status":"Active",
                "category":{"id":1,"code":"DM1","name":"Danh muc"},
                "prices":{"retail":100000,"import":50000},
                "inventory":{"remain":10.0,"available":8.0}}
                """;
        var data = objectMapper.readTree(json);

        handler.handle(eventLog(PRODUCT_ADD), data);

        ArgumentCaptor<NhanhProductDTO> captor = ArgumentCaptor.forClass(NhanhProductDTO.class);
        verify(productSyncService).syncOne(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(12345L);
        assertThat(captor.getValue().getCode()).isEqualTo("SP001");
    }

    @Test
    void handlesProductUpdate() throws Exception {
        String json = """
                {"id":54321,"code":"SP002","name":"Updated Product","status":"Active",
                "prices":{"retail":200000},
                "inventory":{"remain":5.0,"available":3.0}}
                """;
        var data = objectMapper.readTree(json);

        handler.handle(eventLog(PRODUCT_UPDATE), data);

        ArgumentCaptor<NhanhProductDTO> captor = ArgumentCaptor.forClass(NhanhProductDTO.class);
        verify(productSyncService).syncOne(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(54321L);
    }

    @Test
    void handlesProductDelete() {
        String json = "[100, 101, 102]";
        var data = objectMapper.createArrayNode();
        data.add(100);
        data.add(101);
        data.add(102);

        Product product100 = Product.builder().id(100L).externalId(100L).active(true).build();
        Product product101 = Product.builder().id(101L).externalId(101L).active(true).build();

        when(productRepo.findByExternalId(100L)).thenReturn(Optional.of(product100));
        when(productRepo.findByExternalId(101L)).thenReturn(Optional.of(product101));
        when(productRepo.findByExternalId(102L)).thenReturn(Optional.empty());

        handler.handle(eventLog(PRODUCT_DELETE), data);

        verify(productRepo).save(product100);
        verify(productRepo).save(product101);
        assertThat(product100.getActive()).isFalse();
        assertThat(product101.getActive()).isFalse();
    }

    @Test
    void handlesInventoryChange() {
        var data = objectMapper.createArrayNode();
        var item1 = data.addObject();
        item1.put("id", 100L);
        item1.put("remain", 50.0);
        item1.put("available", 45.0);
        var item2 = data.addObject();
        item2.put("id", 200L);
        item2.put("remain", 10.0);
        item2.put("available", 8.0);

        Product product100 = Product.builder().id(100L).externalId(100L).stockRemain(0.0).stockAvailable(0.0).build();
        Product product200 = Product.builder().id(200L).externalId(200L).stockRemain(0.0).stockAvailable(0.0).build();

        when(productRepo.findByExternalId(100L)).thenReturn(Optional.of(product100));
        when(productRepo.findByExternalId(200L)).thenReturn(Optional.of(product200));

        handler.handle(eventLog(INVENTORY_CHANGE), data);

        verify(productRepo).save(product100);
        verify(productRepo).save(product200);
        assertThat(product100.getStockRemain()).isEqualTo(50.0);
        assertThat(product100.getStockAvailable()).isEqualTo(45.0);
        assertThat(product200.getStockRemain()).isEqualTo(10.0);
        assertThat(product200.getStockAvailable()).isEqualTo(8.0);
    }

    @Test
    void skipsProductAddWhenDataIsNull() {
        handler.handle(eventLog(PRODUCT_ADD), null);

        verify(productSyncService, never()).syncOne(any());
    }

    @Test
    void skipsProductDeleteWhenDataIsNotArray() {
        var data = objectMapper.createObjectNode();
        data.put("id", 100);

        handler.handle(eventLog(PRODUCT_DELETE), data);

        verify(productRepo, never()).findByExternalId(any());
    }

    @Test
    void skipsInventoryChangeWhenDataIsNotArray() {
        var data = objectMapper.createObjectNode();
        data.put("id", 100);

        handler.handle(eventLog(INVENTORY_CHANGE), data);

        verify(productRepo, never()).findByExternalId(any());
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
