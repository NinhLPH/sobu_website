package com.vn.sodu.customer.royalty.mapper;

import com.vn.sodu.customer.royalty.LoyaltyTransaction;
import com.vn.sodu.customer.royalty.dto.CreateLoyaltyTransactionRequest;
import com.vn.sodu.customer.royalty.dto.LoyaltyTransactionDTO;
import com.vn.sodu.customer.royalty.dto.LoyaltyTransactionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class LoyaltyTransactionMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Convert LoyaltyTransaction entity to LoyaltyTransactionDTO
     */
    public LoyaltyTransactionDTO toDTO(LoyaltyTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return LoyaltyTransactionDTO.builder()
                .id(transaction.getId())
                .customerId(transaction.getCustomerId())
                .type(transaction.getType() != null ? transaction.getType().name() : null)
                .points(transaction.getPoints())
                .source(transaction.getSource() != null ? transaction.getSource().name() : null)
                .referenceId(transaction.getReferenceId())
                .note(transaction.getNote())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Convert LoyaltyTransactionDTO to LoyaltyTransaction entity
     */
    public LoyaltyTransaction toEntity(LoyaltyTransactionDTO dto) {
        if (dto == null) {
            return null;
        }
        return LoyaltyTransaction.builder()
                .id(dto.getId())
                .customerId(dto.getCustomerId())
                .type(dto.getType() != null ? LoyaltyTransaction.Type.valueOf(dto.getType()) : null)
                .points(dto.getPoints())
                .source(dto.getSource() != null ? LoyaltyTransaction.Source.valueOf(dto.getSource()) : null)
                .referenceId(dto.getReferenceId())
                .note(dto.getNote())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    /**
     * Convert CreateLoyaltyTransactionRequest to LoyaltyTransaction entity
     */
    public LoyaltyTransaction toEntity(CreateLoyaltyTransactionRequest request) {
        if (request == null) {
            return null;
        }
        return LoyaltyTransaction.builder()
                .customerId(request.getCustomerId())
                .type(request.getType() != null ? LoyaltyTransaction.Type.valueOf(request.getType()) : null)
                .points(request.getPoints())
                .source(request.getSource() != null ? LoyaltyTransaction.Source.valueOf(request.getSource()) : null)
                .referenceId(request.getReferenceId())
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Convert LoyaltyTransaction entity to LoyaltyTransactionResponse
     */
    public LoyaltyTransactionResponse toResponse(LoyaltyTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return LoyaltyTransactionResponse.builder()
                .id(transaction.getId())
                .customerId(transaction.getCustomerId())
                .type(transaction.getType() != null ? transaction.getType().name() : null)
                .points(transaction.getPoints())
                .source(transaction.getSource() != null ? transaction.getSource().name() : null)
                .referenceId(transaction.getReferenceId())
                .note(transaction.getNote())
                .createdAt(transaction.getCreatedAt() != null ? transaction.getCreatedAt().format(FORMATTER) : null)
                .build();
    }
}
