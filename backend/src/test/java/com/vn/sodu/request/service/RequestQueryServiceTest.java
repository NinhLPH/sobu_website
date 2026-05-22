package com.vn.sodu.request.service;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestItem;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.RequestResponseDto;
import com.vn.sodu.request.mapper.RequestResponseMapper;
import com.vn.sodu.request.repo.RequestRepo;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestQueryServiceTest {

    @Mock
    private RequestRepo requestRepo;

    @Mock
    private AccountRepo accountRepo;

    private RequestQueryService service;

    @BeforeEach
    void setUp() {
        service = new RequestQueryService(requestRepo, accountRepo, new RequestResponseMapper());
    }

    @Test
    void customerOnlySeesOwnRequests() {
        Authentication auth = authentication("customer@example.com", "ROLE_USER");
        Account account = account("customer@example.com", "0123456789");
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(requestRepo.findByCustomerPhone(
                "0123456789",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        )).thenReturn(samplePageEntity());

        Page<RequestResponseDto> page = service.listRequests(auth, 0, 20, null, null);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getRequestCode()).isEqualTo("SOBU-REQ-1");
    }

    @Test
    void adminCanSeeAllRequests() {
        Authentication auth = authentication("admin@example.com", "ROLE_ADMIN");
        when(requestRepo.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(samplePageEntity());

        Page<RequestResponseDto> page = service.listRequests(auth, 0, 20, null, null);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void customerCanSeeOnlyOwnDetail() {
        Authentication auth = authentication("customer@example.com", "ROLE_USER");
        Account account = account("customer@example.com", "0123456789");
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(requestRepo.findByIdAndCustomerPhone(1L, "0123456789")).thenReturn(Optional.of(sampleRequest()));

        RequestResponseDto dto = service.getRequestDetail(1L, auth);

        assertThat(dto.getRequestCode()).isEqualTo("SOBU-REQ-1");
    }

    @Test
    void customerCannotSeeOtherRequestDetail() {
        Authentication auth = authentication("customer@example.com", "ROLE_USER");
        Account account = account("customer@example.com", "0123456789");
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(requestRepo.findByIdAndCustomerPhone(2L, "0123456789")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getRequestDetail(2L, auth));
    }

    @Test
    void adminCanSeeAnyDetail() {
        Authentication auth = authentication("admin@example.com", "ROLE_ADMIN");
        when(requestRepo.findById(3L)).thenReturn(Optional.of(sampleRequest()));

        RequestResponseDto dto = service.getRequestDetail(3L, auth);

        assertThat(dto.getRequestCode()).isEqualTo("SOBU-REQ-1");
    }

    @Test
    void customerWithoutPhoneIsDenied() {
        Authentication auth = authentication("customer@example.com", "ROLE_USER");
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account("customer@example.com", null)));

        assertThrows(AccessDeniedException.class, () -> service.listRequests(auth, 0, 20, null, null));
    }

    private org.springframework.data.domain.Page<Request> samplePageEntity() {
        return new org.springframework.data.domain.PageImpl<>(
                List.of(sampleRequest()),
                PageRequest.of(0, 20),
                1
        );
    }

    private Request sampleRequest() {
        Request request = Request.builder()
                .id(1L)
                .requestCode("SOBU-REQ-1")
                .customerPhone("0123456789")
                .type(OrderType.NORMAL)
                .status(RequestStatus.REVIEWING)
                .totalAmount(new BigDecimal("100.00"))
                .depositAmount(BigDecimal.ZERO)
                .build();
        request.setItems(List.of(
                RequestItem.builder()
                        .id(10L)
                        .request(request)
                        .name("Item A")
                        .price(new BigDecimal("100"))
                        .quantity(1)
                        .build()
        ));
        return request;
    }

    private Authentication authentication(String email, String role) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() {
                return "";
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return email;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return email;
            }
        };
    }

    private Account account(String email, String phone) {
        return Account.builder()
                .email(email)
                .phone(phone)
                .role(Role.builder().name("USER").build())
                .build();
    }
}
