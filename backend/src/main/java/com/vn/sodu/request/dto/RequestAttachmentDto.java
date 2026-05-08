package com.vn.sodu.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RequestAttachmentDto {

    @Schema(description = "Attachment identifier")
    private Long id;

    @Schema(description = "Attachment URL")
    private String url;

    @Schema(description = "Attachment type", example = "IMAGE")
    private String type;

    @Schema(description = "Attachment MIME type", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "Attachment size in bytes")
    private Long size;

    @Schema(description = "Attachment sort order")
    private Integer sortOrder;

    @Schema(description = "User who uploaded the attachment")
    private String uploadedBy;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
