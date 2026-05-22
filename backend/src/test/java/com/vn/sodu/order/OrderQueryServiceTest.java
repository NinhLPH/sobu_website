package com.vn.sodu.order;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AccountRepo accountRepo;

    private OrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new OrderQueryService(orderRepository, new OrderResponseMapper(), accountRepo);
    }

    @Test
    void listOrdersReturnsPagedDtosWithSafeDefaults() {
        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(orderRepository.findAll(expectedPageable))
                .thenReturn(new PageImpl<>(List.of(sampleOrder()), expectedPageable, 1));

        Page<OrderResponseDto> page = service.listOrders(-1, 0, null, "invalid");

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getOrderCode()).isEqualTo("SOBU-REQ-1");
        assertThat(page.getContent().get(0).getRequestCode()).isEqualTo("REQ-1");
        assertThat(page.getContent().get(0).getItems()).hasSize(1);
    }

    @Test
    void getOrderDetailLoadsOrderWithItemsAndRequest() {
        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(sampleOrder()));

        OrderResponseDto dto = service.getOrderDetail(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getRequestId()).isEqualTo(10L);
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getNhanhProductId()).isEqualTo("123");
    }

    @Test
    void getOrderDetailRejectsMissingOrder() {
        when(orderRepository.findWithItemsAndRequestById(2L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getOrderDetail(2L));
    }

    @Test
    void getMyOrderDetailScopesByAuthenticatedCustomerPhone() {
        Authentication auth = customerAuth();
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account("0900000001")));
        when(orderRepository.findCustomerOrderById(1L, "0900000001")).thenReturn(Optional.of(sampleOrder()));

        OrderResponseDto dto = service.getMyOrderDetail(1L, auth);

        assertThat(dto.getOrderCode()).isEqualTo("SOBU-REQ-1");
        assertThat(dto.getCustomerMobile()).isEqualTo("0900000001");
    }

    @Test
    void getMyOrderDetailRejectsOtherCustomerOrder() {
        Authentication auth = customerAuth();
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account("0900000001")));
        when(orderRepository.findCustomerOrderById(2L, "0900000001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getMyOrderDetail(2L, auth));
    }

    @Test
    void getMyOrderDetailByNhanhOrderIdScopesByAuthenticatedCustomerPhone() {
        Authentication auth = customerAuth();
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account("0900000001")));
        when(orderRepository.findCustomerOrderByNhanhOrderIdOrCode("NH-123", "0900000001"))
                .thenReturn(Optional.of(sampleOrder()));

        OrderResponseDto dto = service.getMyOrderDetailByNhanhOrderId(" NH-123 ", auth);

        assertThat(dto.getOrderCode()).isEqualTo("SOBU-REQ-1");
    }

    @Test
    void getMyOrderDetailRequiresCustomerPhone() {
        Authentication auth = customerAuth();
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account(null)));

        assertThrows(AccessDeniedException.class, () -> service.getMyOrderDetail(1L, auth));
    }

    private Order sampleOrder() {
        Request request = Request.builder()
                .id(10L)
                .requestCode("REQ-1")
                .type(OrderType.NORMAL)
                .build();
        Order order = Order.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .request(request)
                .type(OrderType.NORMAL)
                .status(OrderStatus.NEW)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .depositAmount(BigDecimal.ZERO)
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .nhanhOrderId("NH-123")
                .nhanhOrderCode("SOBU-REQ-1")
                .build();
        OrderItem item = OrderItem.builder()
                .id(100L)
                .order(order)
                .nhanhProductId("123")
                .name("Item A")
                .price(new BigDecimal("100.00"))
                .quantity(1)
                .build();
        order.setItems(List.of(item));
        return order;
    }

    private Authentication customerAuth() {
        return new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
    }

    private Account account(String phone) {
        return Account.builder()
                .email("customer@example.com")
                .phone(phone)
                .role(Role.builder().name("USER").build())
                .build();
    }
}
