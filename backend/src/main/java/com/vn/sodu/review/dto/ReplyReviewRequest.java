package com.vn.sodu.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyReviewRequest {

    @NotBlank
    private String adminReply;
}
