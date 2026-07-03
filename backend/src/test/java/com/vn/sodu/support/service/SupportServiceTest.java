package com.vn.sodu.support.service;

import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.support.ConversationStatus;
import com.vn.sodu.support.SupportConversation;
import com.vn.sodu.support.SupportMessage;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.repo.SupportConversationRepo;
import com.vn.sodu.support.repo.SupportMessageRepo;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
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

    private SupportService supportService;

    @BeforeEach
    void setUp() {
        supportService = new SupportService(conversationRepo, messageRepo);
    }

    @Test
    void getOrCreateConversation_createsWhenNotFound() {
        Account customer = customerAccount(1L, "user@example.com");
        when(conversationRepo.findByAccountId(1L)).thenReturn(Optional.empty());

        SupportConversation saved = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(LocalDateTime.now())
                .build();
        when(conversationRepo.save(any())).thenReturn(saved);

        SupportConversation result = supportService.getOrCreateConversation(customer);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(ConversationStatus.OPEN);
        verify(conversationRepo).save(any());
    }

    @Test
    void getOrCreateConversation_returnsExisting() {
        Account customer = customerAccount(1L, "user@example.com");
        SupportConversation existing = SupportConversation.builder()
                .id(10L)
                .account(customer)
                .status(ConversationStatus.OPEN)
                .build();
        when(conversationRepo.findByAccountId(1L)).thenReturn(Optional.of(existing));

        SupportConversation result = supportService.getOrCreateConversation(customer);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void customerCannotAccessAnotherCustomersConversation() {
        Account customer = customerAccount(1L, "user@example.com");
        Account otherAccount = customerAccount(2L, "other@example.com");

        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(otherAccount)
                .status(ConversationStatus.OPEN)
                .build();

        when(conversationRepo.findById(10L)).thenReturn(Optional.of(conversation));

        assertThrows(AccessDeniedException.class, () ->
                supportService.getMessages(customer, 10L, Pageable.unpaged()));
    }

    @Test
    void staffCanAccessAnyConversation() {
        Account staff = staffAccount(3L, "staff@example.com");
        Account otherCustomer = customerAccount(1L, "user@example.com");

        SupportConversation conversation = SupportConversation.builder()
                .id(10L)
                .account(otherCustomer)
                .status(ConversationStatus.OPEN)
                .build();

        when(conversationRepo.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepo.findByConversationIdOrderByCreatedAtDesc(10L, Pageable.unpaged()))
                .thenReturn(Page.empty());

        Page<MessageResponseDTO> messages = supportService.getMessages(staff, 10L, Pageable.unpaged());

        assertThat(messages).isEmpty();
    }

    @Test
    void staffCanGetAllConversations() {
        when(conversationRepo.findAllByOrderByLastMessageAtDesc(Pageable.unpaged()))
                .thenReturn(Page.empty());

        assertThat(supportService.getStaffConversations(Pageable.unpaged())).isEmpty();
    }

    @Test
    void sendMessage_updatesLastMessageAt() {
        Account customer = customerAccount(1L, "user@example.com");
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

        SupportMessage result = supportService.sendMessage(customer, "USER", 10L, "Hello");

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Hello");
        verify(conversationRepo).save(conversation);
        assertThat(conversation.getLastMessageAt()).isNotNull();
    }

    @Test
    void getConversationForAccount_throwsWhenNotFound() {
        when(conversationRepo.findByAccountId(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> supportService.getConversationForAccount(99L));
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
