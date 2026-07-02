package com.vn.sodu.global.exception;

import com.vn.sodu.global.dto.ApiResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void redisConnectionFailuresReturnServiceUnavailable() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponseDTO<Void>> response = handler.handleRedisConnectionFailureException(
                new RedisConnectionFailureException("Unable to connect to Redis")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isEqualTo("CART_STORAGE_UNAVAILABLE");
        assertThat(response.getBody().getMessage()).isEqualTo("Cart storage is unavailable");
    }
}
