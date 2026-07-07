package com.vn.sodu.order.nhanh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.product.dto.NhanhResponse;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.payment.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NhanhOrderGatewayTest {

    @Mock
    private NhanhClient nhanhClient;

    @Test
    void mapsOrderToNhanhOrderAddPayload() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());

        NhanhOrderAddRequest request = gateway.buildAddRequest(normalOrder(), normalPayment());

        assertThat(request.getInfo().getType()).isEqualTo(2);
        assertThat(request.getInfo().getDepotId()).isEqualTo(12345L);
        assertThat(request.getInfo().getDescription()).isEqualTo("Handle with care");
        assertThat(request.getChannel().getAppOrderId()).isEqualTo("SOBU-REQ-1");
        assertThat(request.getChannel().getSourceName()).isEqualTo("Sodu Website");
        assertThat(request.getShippingAddress().getName()).isEqualTo("Nguyen Van A");
        assertThat(request.getShippingAddress().getMobile()).isEqualTo("0900000001");
        assertThat(request.getShippingAddress().getCityId()).isEqualTo(79L);
        assertThat(request.getCarrier().getId()).isEqualTo(8L);
        assertThat(request.getCarrier().getServiceId()).isEqualTo(1L);
        assertThat(request.getCarrier().getCustomerShipFee()).isEqualByComparingTo("35000");
        assertThat(request.getPayment().getDepositAmount()).isNull();
        assertThat(request.getPayment().getTransferAmount()).isEqualByComparingTo("335000");
        assertThat(request.getPayment().getTransferAccountId()).isEqualTo(266363L);
        assertThat(request.getProducts()).hasSize(1);
        assertThat(request.getProducts().get(0).getId()).isEqualTo(12345L);
        assertThat(request.getProducts().get(0).getPrice()).isEqualByComparingTo("150000");
        assertThat(request.getProducts().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void omitsTransferAmountForCodPendingPayment() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());

        Order order = normalOrder();
        OrderPayment codPayment = OrderPayment.builder()
                .id(66L)
                .paymentCode("SOBU-PAY-66")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .amount(new BigDecimal("335000"))
                .build();

        NhanhOrderAddRequest request = gateway.buildAddRequest(order, codPayment);

        assertThat(request.getPayment()).isNull();
    }

    @Test
    void invokesNhanhClientWithOrderAddEndpoint() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());
        NhanhOrderAddResult data = new NhanhOrderAddResult();
        data.setId(987L);
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(new NhanhResponse<>(1, data, null));

        NhanhOrderAddRequest request = gateway.buildAddRequest(normalOrder(), normalPayment());
        NhanhOrderAddResult result = gateway.createOrder(request, "access-token");

        assertThat(result.getId()).isEqualTo(987L);
        ArgumentCaptor<NhanhOrderAddRequest> requestCaptor = ArgumentCaptor.forClass(NhanhOrderAddRequest.class);
        verify(nhanhClient).post(eq("/v3.0/order/add"), eq("access-token"), requestCaptor.capture(), anyResponseType());
        assertThat(requestCaptor.getValue().getChannel().getAppOrderId()).isEqualTo("SOBU-REQ-1");
    }

    @Test
    void parsesNhanhOrderAddDataObjectShape() throws Exception {
        String json = """
                {
                  "code": 1,
                  "data": {
                    "id": 987,
                    "trackingUrl": "https://track.example/order/987"
                  }
                }
                """;

        NhanhResponse<NhanhOrderAddResult> response = new ObjectMapper().readValue(
                json,
                new TypeReference<NhanhResponse<NhanhOrderAddResult>>() {}
        );

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(987L);
        assertThat(response.getData().getTrackingUrl()).isEqualTo("https://track.example/order/987");
    }

    @Test
    void parsesNhanhOrderAddAlternateOrderIdField() throws Exception {
        String json = """
                {
                  "code": 1,
                  "data": {
                    "orderId": 654321,
                    "trackingUrl": "https://track.example/order/654321"
                  }
                }
                """;

        NhanhResponse<NhanhOrderAddResult> response = new ObjectMapper().readValue(
                json,
                new TypeReference<NhanhResponse<NhanhOrderAddResult>>() {}
        );

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getOrderId()).isEqualTo(654321L);
        assertThat(response.getData().resolveNhanhOrderId()).isEqualTo("654321");
    }

    @Test
    void parsesNhanhMessagesArrayOfObjectsShape() throws Exception {
        String json = """
                {
                  "code": 0,
                  "messages": [
                    {
                      "code": "duplicate",
                      "message": "Duplicate appOrderId"
                    }
                  ]
                }
                """;

        NhanhResponse<NhanhOrderAddResult> response = new ObjectMapper().readValue(
                json,
                new TypeReference<NhanhResponse<NhanhOrderAddResult>>() {}
        );

        assertThat(response.getMessages()).containsExactly("duplicate: Duplicate appOrderId");
    }

    @Test
    void parsesNhanhSingleMessageObjectShape() throws Exception {
        String json = """
                {
                  "code": 0,
                  "messages": {
                    "code": "duplicate",
                    "message": "Duplicate appOrderId"
                  }
                }
                """;

        NhanhResponse<NhanhOrderAddResult> response = new ObjectMapper().readValue(
                json,
                new TypeReference<NhanhResponse<NhanhOrderAddResult>>() {}
        );

        assertThat(response.getMessages()).containsExactly("duplicate: Duplicate appOrderId");
    }

    @Test
    void treatsDuplicateAppOrderIdResponseAsSuccessfulDuplicate() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());
        NhanhResponse<NhanhOrderAddResult> response = new NhanhResponse<>(0, null, null);
        response.setMessages(List.of("Duplicate appOrderId"));
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(response);

        NhanhOrderAddResult result = gateway.createOrder(gateway.buildAddRequest(normalOrder(), normalPayment()), "access-token");

        assertThat(result.isDuplicate()).isTrue();
    }

    @Test
    void treatsVietnameseExistingOrderResponseAsSuccessfulDuplicate() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());
        NhanhResponse<NhanhOrderAddResult> response = new NhanhResponse<>(0, null, null);
        response.setMessages(List.of("Đơn hàng: SOBU-ORD-20260531223411-3213 đã tồn tại"));
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(response);

        NhanhOrderAddResult result = gateway.createOrder(gateway.buildAddRequest(normalOrder(), normalPayment()), "access-token");

        assertThat(result.isDuplicate()).isTrue();
    }

    @Test
    void findsExistingOrderByAppOrderIdViaOrderList() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());
        Order order = normalOrder();
        order.setCreatedAt(LocalDateTime.now());

        String json = """
                {
                  "code": 1,
                  "data": [
                    {
                      "info": {
                        "id": 654321
                      },
                      "channel": {
                        "appOrderId": "SOBU-REQ-1"
                      }
                    }
                  ]
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhResponse<List<com.vn.sodu.nhanh.dto.NhanhOrderListItem>> response;
        try {
            response = mapper.readValue(
                    json,
                    new TypeReference<NhanhResponse<List<com.vn.sodu.nhanh.dto.NhanhOrderListItem>>>() {}
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        when(nhanhClient.fetchAllPages(
                eq("/v3.0/order/list"),
                eq("access-token"),
                any(Map.class),
                anyLookupResponseType()
        )).thenReturn(response.getData());

        Optional<com.vn.sodu.nhanh.dto.NhanhOrderListItem> result = gateway.findOrderByReference(order, "access-token");

        assertThat(result).isPresent();
        assertThat(result.get().resolveNhanhOrderId()).isEqualTo("654321");
    }

    private Order normalOrder() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .appOrderId("SOBU-REQ-1")
                .type(OrderType.NORMAL)
                .description("Handle with care")
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerEmail("a@example.com")
                .customerAddress("12 Le Loi")
                .customerCityId(79L)
                .customerDistrictId(760L)
                .customerWardId(26734L)
                .carrierId(8L)
                .carrierServiceId(1L)
                .shippingFee(new BigDecimal("35000"))
                .depositAmount(BigDecimal.ZERO)
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .nhanhProductId("12345")
                .name("Item")
                .price(new BigDecimal("150000"))
                .quantity(2)
                .build();
        order.setItems(List.of(item));
        return order;
    }

    private OrderPayment normalPayment() {
        return OrderPayment.builder()
                .id(55L)
                .paymentCode("SOBU-PAY-55")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("335000"))
                .build();
    }

    private NhanhProperties nhanhProperties() {
        NhanhProperties properties = new NhanhProperties();
        properties.setClientId("client");
        properties.setClientSecret("secret");
        properties.setRedirectUri("http://localhost/callback");
        properties.setBaseUrl("https://pos.open.nhanh.vn/api");
        properties.setBusinessId("biz");
        properties.setDepotId(12345L);
        NhanhProperties.Accounting accounting = new NhanhProperties.Accounting();
        accounting.setAccountId(266363L);
        properties.setAccounting(accounting);
        return properties;
    }

    @Test
    void throwsExceptionWhenNhanhReturnsSuccessButDataIsNull() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient, nhanhProperties());
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(new NhanhResponse<>(1, null, null));

        NhanhOrderAddRequest request = gateway.buildAddRequest(normalOrder(), normalPayment());

        try {
            gateway.createOrder(request, "access-token");
            assertThat(true).as("Should have thrown exception for null data").isFalse();
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(Exception.class);
            assertThat(ex.getMessage()).contains("null", "data");
        }
    }

    @SuppressWarnings("unchecked")
    private ParameterizedTypeReference<NhanhResponse<NhanhOrderAddResult>> anyResponseType() {
        return any(ParameterizedTypeReference.class);
    }

    @SuppressWarnings("unchecked")
    private ParameterizedTypeReference<NhanhResponse<List<com.vn.sodu.nhanh.dto.NhanhOrderListItem>>> anyLookupResponseType() {
        return any(ParameterizedTypeReference.class);
    }
}
