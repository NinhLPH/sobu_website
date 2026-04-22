package com.vn.sodu.nhanh;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhanhTokenApiResponse {
    private int code;
    private Data data;

    @Getter @Setter
    public static class Data {
        private String accessToken;
        private Long businessId;
        private Long expiredAt;
    }
}
