package com.vn.sodu.request.mapper;

import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestAttachment;
import com.vn.sodu.request.RequestItem;
import com.vn.sodu.request.dto.RequestAttachmentDto;
import com.vn.sodu.request.dto.RequestItemResponseDto;
import com.vn.sodu.request.dto.RequestResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestResponseMapper {

    public RequestResponseDto toDto(Request request) {
        if (request == null) {
            return null;
        }

        return RequestResponseDto.builder()
                .id(request.getId())
                .requestCode(request.getRequestCode())
                .customerPhone(request.getCustomerPhone())
                .type(request.getType())
                .status(request.getStatus())
                .totalAmount(request.getTotalAmount())
                .depositAmount(request.getDepositAmount())
                .customRequirements(request.getCustomRequirements())
                .nhanhOrderId(request.getNhanhOrderId())
                .nhanhOrderCode(request.getNhanhOrderCode())
                .items(mapItems(request.getItems()))
                .attachments(mapAttachments(request.getAttachments()))
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private List<RequestItemResponseDto> mapItems(List<RequestItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> RequestItemResponseDto.builder()
                        .id(item.getId())
                        .nhanhProductId(item.getNhanhProductId())
                        .name(item.getName())
                        .note(item.getNote())
                        .metadataJson(item.getMetadataJson())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private List<RequestAttachmentDto> mapAttachments(List<RequestAttachment> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> RequestAttachmentDto.builder()
                        .id(attachment.getId())
                        .url(attachment.getUrl())
                        .type(attachment.getType())
                        .mimeType(attachment.getMimeType())
                        .size(attachment.getSize())
                        .sortOrder(attachment.getSortOrder())
                        .uploadedBy(attachment.getUploadedBy())
                        .createdAt(attachment.getCreatedAt())
                        .build())
                .toList();
    }
}
