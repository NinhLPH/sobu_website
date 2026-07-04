package com.vn.sodu.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEligibilityResponse {
    private boolean canReview;
    private String reason;
    private Long orderId;
    private boolean alreadyReviewed;
    private boolean deliveredOrderFound;
}
