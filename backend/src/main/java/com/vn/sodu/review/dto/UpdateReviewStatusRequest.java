package com.vn.sodu.review.dto;

import com.vn.sodu.review.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewStatusRequest {

    @NotNull
    private ReviewStatus status;
}
