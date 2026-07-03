package com.vn.sodu.support.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.support.ConversationStatus;
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
class AdminSupportControllerTest {

    @Mock
    private SupportService supportService;

    @Mock
    private AccountRepo accountRepo;

    private AdminSupportController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSupportController(supportService, accountRepo);
    }

    @Test
    void listConversations_acceptsStaff() {
        Authentication auth = authentication("staff@example.com", "ROLE_STAFF");
        Page<ConversationSummaryDTO> page = new PageImpl<>(List.of(
                ConversationSummaryDTO.builder()
                        .id(10L)
                        .status(ConversationStatus.OPEN)
                        .customerEmail("user@example.com")
                        .customerName("User")
                        .lastMessageAt(LocalDateTime.now())
                        .build()
        ));

        when(supportService.getStaffConversations(any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = controller.listConversations(auth, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponseDTO<?> body = (ApiResponseDTO<?>) response.getBody();
        assertThat(body.getData()).isInstanceOf(PageResponse.class);
    }

    @Test
    void listConversations_acceptsAdmin() {
        Authentication auth = authentication("admin@example.com", "ROLE_ADMIN");

        when(supportService.getStaffConversations(any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<?> response = controller.listConversations(auth, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listConversations_rejectsUser() {
        Authentication auth = authentication("user@example.com", "ROLE_USER");

        assertThrows(AccessDeniedException.class, () -> controller.listConversations(auth, 0, 20));
    }

    @Test
    void getConversationMessages_acceptsStaff() {
        Authentication auth = authentication("staff@example.com", "ROLE_STAFF");
        Account account = account(2L, "staff@example.com", "STAFF");
        Page<MessageResponseDTO> messagePage = new PageImpl<>(List.of(
                MessageResponseDTO.builder()
                        .id(100L)
                        .conversationId(10L)
                        .senderId(1L)
                        .senderRole("USER")
                        .content("Hello")
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        when(accountRepo.findByEmail("staff@example.com")).thenReturn(Optional.of(account));
        when(supportService.getMessages(any(), any(), any(Pageable.class))).thenReturn(messagePage);

        ResponseEntity<?> response = controller.getConversationMessages(auth, 10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getConversationMessages_rejectsUser() {
        Authentication auth = authentication("user@example.com", "ROLE_USER");

        assertThrows(AccessDeniedException.class, () -> controller.getConversationMessages(auth, 10L, 0, 20));
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
