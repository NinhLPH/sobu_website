package com.vn.sodu.support.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.support.ConversationStatus;
import com.vn.sodu.support.SupportConversation;
import com.vn.sodu.support.dto.ConversationSummaryDTO;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.service.SupportService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    @Mock
    private SupportService supportService;

    @Mock
    private AccountRepo accountRepo;

    private SupportController controller;

    @BeforeEach
    void setUp() {
        controller = new SupportController(supportService, accountRepo);
    }

    @Test
    void getOrCreateConversation_returnsConversationForCustomer() {
        Authentication auth = authentication("user@example.com", "ROLE_USER");
        Account account = account(1L, "user@example.com", "USER");
        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(account)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        ConversationSummaryDTO dto = ConversationSummaryDTO.builder()
                .id(10L)
                .status(ConversationStatus.OPEN)
                .customerEmail("user@example.com")
                .customerName("User")
                .build();

        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(supportService.isStaff(account)).thenReturn(false);
        when(supportService.getOrCreateConversation(account)).thenReturn(conversation);
        when(supportService.toSummary(conversation)).thenReturn(dto);

        ResponseEntity<?> response = controller.getOrCreateConversation(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ApiResponseDTO.class);
        ApiResponseDTO<?> body = (ApiResponseDTO<?>) response.getBody();
        assertThat(body.getData()).isEqualTo(dto);
    }

    @Test
    void getOrCreateConversation_deniesStaff() {
        Authentication auth = authentication("staff@example.com", "ROLE_STAFF");
        Account account = account(2L, "staff@example.com", "STAFF");

        when(accountRepo.findByEmail("staff@example.com")).thenReturn(Optional.of(account));
        when(supportService.isStaff(account)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> controller.getOrCreateConversation(auth));
    }

    @Test
    void getMyMessages_returnsOwnMessages() {
        Authentication auth = authentication("user@example.com", "ROLE_USER");
        Account account = account(1L, "user@example.com", "USER");
        Page<MessageResponseDTO> messagePage = new PageImpl<>(List.of(
                MessageResponseDTO.builder()
                        .id(100L)
                        .conversationId(10L)
                        .senderId(1L)
                        .senderRole("USER")
                        .content("Hello")
                        .build()
        ));

        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(supportService.getMyMessages(any(), any(Pageable.class))).thenReturn(messagePage);

        ResponseEntity<?> response = controller.getMyMessages(auth, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponseDTO<?> body = (ApiResponseDTO<?>) response.getBody();
        assertThat(body.getData()).isInstanceOf(PageResponse.class);
    }

    private Authentication authentication(String email, String role) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() { return ""; }

            @Override
            public Object getDetails() { return null; }

            @Override
            public Object getPrincipal() { return email; }

            @Override
            public boolean isAuthenticated() { return true; }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {}

            @Override
            public String getName() { return email; }
        };
    }

    private Account account(Long id, String email, String roleName) {
        return Account.builder()
                .id(id)
                .email(email)
                .fullName("User")
                .role(Role.builder().name(roleName).build())
                .build();
    }
}
