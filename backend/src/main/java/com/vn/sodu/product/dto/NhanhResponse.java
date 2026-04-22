package com.vn.sodu.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhResponse<T> {
    private int code;
    private T data;

    private Paginator paginator;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paginator {
        private String next;
    }
}
