package com.vn.sodu.order.nhanh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.product.dto.NhanhResponse;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.List;

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
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient);

        NhanhOrderAddRequest request = gateway.toNhanhOrderAddRequest(normalOrder());

        assertThat(request.getInfo().getType()).isEqualTo(2);
        assertThat(request.getInfo().getDescription()).isEqualTo("Handle with care");
        assertThat(request.getChannel().getAppOrderId()).isEqualTo("SOBU-REQ-1");
        assertThat(request.getChannel().getSourceName()).isEqualTo("Sodu Website");
        assertThat(request.getShippingAddress().getName()).isEqualTo("Nguyen Van A");
        assertThat(request.getShippingAddress().getMobile()).isEqualTo("0900000001");
        assertThat(request.getPayment().getDepositAmount()).isEqualByComparingTo("0");
        assertThat(request.getProducts()).hasSize(1);
        assertThat(request.getProducts().get(0).getId()).isEqualTo(12345L);
        assertThat(request.getProducts().get(0).getPrice()).isEqualByComparingTo("150000");
        assertThat(request.getProducts().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void invokesNhanhClientWithOrderAddEndpoint() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient);
        NhanhOrderAddResult data = new NhanhOrderAddResult();
        data.setId(987L);
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(new NhanhResponse<>(1, List.of(data), null));

        NhanhOrderAddResult result = gateway.createOrder(normalOrder(), "access-token");

        assertThat(result.getId()).isEqualTo(987L);
        ArgumentCaptor<NhanhOrderAddRequest> requestCaptor = ArgumentCaptor.forClass(NhanhOrderAddRequest.class);
        verify(nhanhClient).post(eq("/v3.0/order/add"), eq("access-token"), requestCaptor.capture(), anyResponseType());
        assertThat(requestCaptor.getValue().getChannel().getAppOrderId()).isEqualTo("SOBU-REQ-1");
    }

    @Test
    void parsesNhanhOrderAddDataArrayShape() throws Exception {
        String json = """
                {
                  "code": 1,
                  "data": [
                    {
                      "id": 987,
                      "trackingUrl": "https://track.example/order/987"
                    }
                  ]
                }
                """;

        NhanhResponse<List<NhanhOrderAddResult>> response = new ObjectMapper().readValue(
                json,
                new TypeReference<NhanhResponse<List<NhanhOrderAddResult>>>() {}
        );

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getId()).isEqualTo(987L);
        assertThat(response.getData().get(0).getTrackingUrl()).isEqualTo("https://track.example/order/987");
    }

    @Test
    void treatsDuplicateAppOrderIdResponseAsSuccessfulDuplicate() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient);
        NhanhResponse<List<NhanhOrderAddResult>> response = new NhanhResponse<>(0, null, null);
        response.setMessages(List.of("Duplicate appOrderId"));
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(response);

        NhanhOrderAddResult result = gateway.createOrder(normalOrder(), "access-token");

        assertThat(result.isDuplicate()).isTrue();
    }

    @Test
    void treatsVietnameseExistingOrderResponseAsSuccessfulDuplicate() {
        NhanhOrderGateway gateway = new NhanhOrderGateway(nhanhClient);
        NhanhResponse<List<NhanhOrderAddResult>> response = new NhanhResponse<>(0, null, null);
        response.setMessages(List.of("Đơn hàng: SOBU-ORD-20260531223411-3213 đã tồn tại"));
        when(nhanhClient.post(
                eq("/v3.0/order/add"),
                eq("access-token"),
                any(NhanhOrderAddRequest.class),
                anyResponseType()
        )).thenReturn(response);

        NhanhOrderAddResult result = gateway.createOrder(normalOrder(), "access-token");

        assertThat(result.isDuplicate()).isTrue();
    }

    private Order normalOrder() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .type(OrderType.NORMAL)
                .description("Handle with care")
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerEmail("a@example.com")
                .customerAddress("12 Le Loi")
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

    @SuppressWarnings("unchecked")
    private ParameterizedTypeReference<NhanhResponse<List<NhanhOrderAddResult>>> anyResponseType() {
        return any(ParameterizedTypeReference.class);
    }
}
