package com.vn.sodu.review.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.review.ReviewService;
import com.vn.sodu.review.ReviewStatus;
import com.vn.sodu.review.dto.CreateReviewRequest;
import com.vn.sodu.review.dto.ReviewResponseDto;
import com.vn.sodu.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private StorageService storageService;

    @Test
    void createReviewReturns201() {
        Authentication auth = new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
        CreateReviewRequest request = new CreateReviewRequest();
        request.setProductId(100L);
        request.setOrderId(200L);
        request.setRating(5);
        request.setContent("Great!");

        ReviewResponseDto dto = ReviewResponseDto.builder()
                .id(1L)
                .productId(100L)
                .orderId(200L)
                .rating(5)
                .content("Great!")
                .status(ReviewStatus.PUBLISHED)
                .build();

        when(reviewService.createReview(any(CreateReviewRequest.class), eq("customer@example.com"))).thenReturn(dto);

        ReviewController controller = new ReviewController(reviewService, storageService);
        ResponseEntity<ApiResponseDTO<ReviewResponseDto>> response = controller.createReview(request, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        assertThat(response.getBody().getMessage()).isEqualTo("Review created");
        verify(reviewService).createReview(request, "customer@example.com");
    }

    @Test
    void uploadFileReturnsUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        when(storageService.store(file, "reviews")).thenReturn("/api/public/files/reviews/uuid-test.jpg");

        ReviewController controller = new ReviewController(reviewService, storageService);
        ResponseEntity<ApiResponseDTO<Map<String, String>>> response = controller.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().get("url")).isEqualTo("/api/public/files/reviews/uuid-test.jpg");
        verify(storageService).store(file, "reviews");
    }

}
