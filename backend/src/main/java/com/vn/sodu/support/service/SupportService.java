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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportConversationRepo conversationRepo;
    private final SupportMessageRepo messageRepo;
    private final AccountRepo accountRepo;

    @Transactional
    public ConversationSummaryDTO getOrCreateConversationSummary(String accountEmail) {
        Account account = resolveAccount(accountEmail);
        if (isStaff(account)) {
            throw new AccessDeniedException("Staff accounts do not have a personal support conversation");
        }
        return toSummary(getOrCreateConversation(account));
    }

    @Transactional(readOnly = true)
    public SupportPrincipalDTO getSupportPrincipal(String accountEmail) {
        Account account = resolveAccount(accountEmail);
        String roleName = getRoleName(account);
        return SupportPrincipalDTO.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .roleName(roleName)
                .staff(isStaffRole(roleName))
                .active(Account.AccountStatus.ACTIVE.equals(account.getStatus()))
                .build();
    }

    private SupportConversation getOrCreateConversation(Account account) {
        return conversationRepo.findByAccountId(account.getId())
                .orElseGet(() -> {
                    SupportConversation conversation = SupportConversation.builder()
                            .account(account)
                            .status(ConversationStatus.OPEN)
                            .lastMessageAt(LocalDateTime.now())
                            .build();
                    return conversationRepo.save(conversation);
                });
    }

    @Transactional(readOnly = true)
    public Long getConversationCustomerId(Long conversationId) {
        SupportConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        return conversation.getAccount().getId();
    }

    @Transactional(readOnly = true)
    public Page<MessageResponseDTO> getMessages(String accountEmail, Long conversationId, Pageable pageable) {
        Account account = resolveAccount(accountEmail);
        SupportConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (!isStaff(account) && !conversation.getAccount().getId().equals(account.getId())) {
            throw new AccessDeniedException("Access denied to this conversation");
        }

        return messageRepo.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public Page<MessageResponseDTO> getMyMessages(String accountEmail, Pageable pageable) {
        Account account = resolveAccount(accountEmail);
        SupportConversation conversation = getOrCreateConversation(account);

        return messageRepo.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public MessageResponseDTO sendMessage(Long senderId, String role, Long conversationId, String content) {
        Account sender = resolveAccount(senderId);
        SupportConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (!isStaff(sender) && !conversation.getAccount().getId().equals(sender.getId())) {
            throw new AccessDeniedException("Access denied to this conversation");
        }

        SupportMessage message = SupportMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .senderRole(role)
                .content(content)
                .build();

        message = messageRepo.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepo.save(conversation);

        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public Page<ConversationSummaryDTO> getStaffConversations(Pageable pageable) {
        return conversationRepo.findAllByOrderByLastMessageAtDesc(pageable)
                .map(this::toSummary);
    }

    private MessageResponseDTO toMessageResponse(SupportMessage message) {
        return MessageResponseDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderEmail(message.getSender().getEmail())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ConversationSummaryDTO toSummary(SupportConversation conversation) {
        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .status(conversation.getStatus())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .customerEmail(conversation.getAccount().getEmail())
                .customerName(conversation.getAccount().getFullName())
                .build();
    }

    private Account resolveAccount(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return accountRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated account not found"));
    }

    private Account resolveAccount(Long accountId) {
        if (accountId == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        return accountRepo.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Authenticated account not found"));
    }

    private boolean isStaff(Account account) {
        return isStaffRole(getRoleName(account));
    }

    private boolean isStaffRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        String name = roleName.toUpperCase();
        return "ADMIN".equals(name) || "STAFF".equals(name);
    }

    private String getRoleName(Account account) {
        if (account == null || account.getRole() == null || account.getRole().getName() == null) {
            return "USER";
        }
        return account.getRole().getName().toUpperCase();
    }
}
