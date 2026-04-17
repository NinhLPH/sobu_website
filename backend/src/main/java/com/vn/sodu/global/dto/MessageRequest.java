package com.vn.sodu.global.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MessageRequest {
    @NotBlank(message = "Message cannot be blank")
    private String message;
}
