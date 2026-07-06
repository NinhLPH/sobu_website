package com.vn.sodu.support.service;

import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.support.ConversationStatus;
import com.vn.sodu.support.SupportConversation;
import com.vn.sodu.support.SupportMessage;
import com.vn.sodu.support.dto.ConversationSummaryDTO;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.dto.SupportPrincipalDTO;
import com.vn.sodu.support.repo.SupportConversationRepo;
import com.vn.sodu.support.repo.SupportMessageRepo;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock
    private SupportConversationRepo conversationRepo;

    @Mock
    private SupportMessageRepo messageRepo;

    @Mock
    private AccountRepo accountRepo;

    private SupportService supportService;

    @BeforeEach
    void setUp() {
        supportService = new SupportService(conversationRepo, messageRepo, accountRepo);
    }

    @Test
    void getOrCreateConversationSummary_createsWhenNotFound() {
        Account customer = customerAccount(1L, "user@example.com");
        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(customer));
        when(conversationRepo.findByAccountId(1L)).thenReturn(Optional.empty());

        SupportConversation saved = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(LocalDateTime.now())
                .build();
        when(conversationRepo.save(any())).thenReturn(saved);

        ConversationSummaryDTO result = supportService.getOrCreateConversationSummary("user@example.com");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(ConversationStatus.OPEN);
        assertThat(result.getCustomerEmail()).isEqualTo("user@example.com");
        verify(conversationRepo).save(any());
    }

    @Test
    void getOrCreateConversationSummary_returnsExistingWithCustomerInfo() {
        Account customer = customerAccount(1L, "user@example.com");
        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(customer));
        SupportConversation existing = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .status(ConversationStatus.OPEN)
                .build();
        when(conversationRepo.findByAccountId(1L)).thenReturn(Optional.of(existing));

        ConversationSummaryDTO result = supportService.getOrCreateConversationSummary("user@example.com");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCustomerEmail()).isEqualTo("user@example.com");
        assertThat(result.getCustomerName()).isEqualTo("Customer");
    }

    @Test
    void customerCannotAccessAnotherCustomersConversation() {
        Account customer = customerAccount(1L, "user@example.com");
        Account otherAccount = customerAccount(2L, "other@example.com");
        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(customer));

        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(otherAccount)
                .status(ConversationStatus.OPEN)
                .build();

        when(conversationRepo.findById(10L)).thenReturn(Optional.of(conversation));

        assertThrows(AccessDeniedException.class, () ->
                supportService.getMessages("user@example.com", 10L, Pageable.unpaged()));
    }

    @Test
    void staffCanAccessAnyConversation() {
        Account staff = staffAccount(3L, "staff@example.com");
        Account otherCustomer = customerAccount(1L, "user@example.com");
        when(accountRepo.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));

        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(otherCustomer)
                .status(ConversationStatus.OPEN)
                .build();

        when(conversationRepo.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepo.findByConversationIdOrderByCreatedAtDesc(10L, Pageable.unpaged()))
                .thenReturn(Page.empty());

        Page<MessageResponseDTO> messages = supportService.getMessages("staff@example.com", 10L, Pageable.unpaged());

        assertThat(messages).isEmpty();
    }

    @Test
    void staffCanGetAllConversations() {
        when(conversationRepo.findAllByOrderByLastMessageAtDesc(Pageable.unpaged()))
                .thenReturn(Page.empty());

        assertThat(supportService.getStaffConversations(Pageable.unpaged())).isEmpty();
    }

    @Test
    void getMyMessages_createsConversationWhenMissing() {
        Account customer = customerAccount(1L, "user@example.com");
        when(accountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(customer));
        SupportConversation saved = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(LocalDateTime.now())
                .build();

        when(conversationRepo.findByAccountId(1L)).thenReturn(Optional.empty());
        when(conversationRepo.save(any())).thenReturn(saved);
        when(messageRepo.findByConversationIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 20)))
                .thenReturn(Page.empty());

        Page<MessageResponseDTO> messages = supportService.getMyMessages("user@example.com", PageRequest.of(0, 20));

        assertThat(messages).isEmpty();
        verify(conversationRepo).save(any());
    }

    @Test
    void sendMessage_updatesLastMessageAtAndReturnsDto() {
        Account customer = customerAccount(1L, "user@example.com");
        when(accountRepo.findById(1L)).thenReturn(Optional.of(customer));
        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(null)
                .build();

        SupportMessage savedMessage = SupportMessage.builder()
                .id(100L)
                .conversation(conversation)
                .sender(customer)
                .senderRole("USER")
                .content("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationRepo.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepo.save(any())).thenReturn(savedMessage);
        when(conversationRepo.save(any())).thenReturn(conversation);

        MessageResponseDTO result = supportService.sendMessage(1L, "USER", 10L, "Hello");

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Hello");
        assertThat(result.getSenderEmail()).isEqualTo("user@example.com");
        verify(conversationRepo).save(conversation);
        assertThat(conversation.getLastMessageAt()).isNotNull();
    }

    @Test
    void getConversationCustomerId_throwsWhenNotFound() {
        when(conversationRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> supportService.getConversationCustomerId(99L));
    }

    @Test
    void getSupportPrincipal_returnsLightweightPrincipal() {
        Account staff = staffAccount(3L, "staff@example.com");
        staff.setStatus(Account.AccountStatus.ACTIVE);
        when(accountRepo.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));

        SupportPrincipalDTO principal = supportService.getSupportPrincipal("staff@example.com");

        assertThat(principal.accountId()).isEqualTo(3L);
        assertThat(principal.email()).isEqualTo("staff@example.com");
        assertThat(principal.roleName()).isEqualTo("STAFF");
        assertThat(principal.staff()).isTrue();
        assertThat(principal.active()).isTrue();
    }

    private Account customerAccount(Long id, String email) {
        return Account.builder()
                .id(id)
                .email(email)
                .fullName("Customer")
                .role(Role.builder().name("USER").build())
                .build();
    }

    private Account staffAccount(Long id, String email) {
        return Account.builder()
                .id(id)
                .email(email)
                .fullName("Staff")
                .role(Role.builder().name("STAFF").build())
                .build();
    }
}
