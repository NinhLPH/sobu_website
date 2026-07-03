package com.vn.sodu.support.service;

import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.support.ConversationStatus;
import com.vn.sodu.support.SupportConversation;
import com.vn.sodu.support.SupportMessage;
import com.vn.sodu.support.dto.ConversationSummaryDTO;
import com.vn.sodu.support.dto.MessageResponseDTO;
import com.vn.sodu.support.repo.SupportConversationRepo;
import com.vn.sodu.support.repo.SupportMessageRepo;
import com.vn.sodu.user.Account;
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

    @Transactional
    public SupportConversation getOrCreateConversation(Account account) {
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
    public SupportConversation getConversationForAccount(Long accountId) {
        return conversationRepo.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Support conversation not found"));
    }

    @Transactional(readOnly = true)
    public ConversationSummaryDTO toSummary(SupportConversation conversation) {
        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .status(conversation.getStatus())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .customerEmail(conversation.getAccount().getEmail())
                .customerName(conversation.getAccount().getFullName())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MessageResponseDTO> getMessages(Account account, Long conversationId, Pageable pageable) {
        SupportConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (!isStaff(account) && !conversation.getAccount().getId().equals(account.getId())) {
            throw new AccessDeniedException("Access denied to this conversation");
        }

        return messageRepo.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponseDTO> getMyMessages(Account account, Pageable pageable) {
        SupportConversation conversation = conversationRepo.findByAccountId(account.getId())
                .orElseThrow(() -> new NotFoundException("Support conversation not found"));

        return messageRepo.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public SupportMessage sendMessage(Account sender, String role, Long conversationId, String content) {
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

        return message;
    }

    @Transactional(readOnly = true)
    public Page<ConversationSummaryDTO> getStaffConversations(Pageable pageable) {
        return conversationRepo.findAllByOrderByLastMessageAtDesc(pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public SupportConversation getConversationById(Long conversationId) {
        return conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    public MessageResponseDTO toMessageResponse(SupportMessage message) {
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

    public boolean isStaff(Account account) {
        if (account == null || account.getRole() == null || account.getRole().getName() == null) {
            return false;
        }
        String name = account.getRole().getName().toUpperCase();
        return "ADMIN".equals(name) || "STAFF".equals(name);
    }
}
