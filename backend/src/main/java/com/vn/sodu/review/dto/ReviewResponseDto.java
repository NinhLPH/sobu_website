package com.vn.sodu.review.dto;

import com.vn.sodu.review.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponseDto {

    private Long id;
    private Long productId;
    private Long orderId;
    private Integer rating;
    private String content;
    private List<String> imageUrls;
    private ReviewStatus status;
    private String customerName;
    private String adminReply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
