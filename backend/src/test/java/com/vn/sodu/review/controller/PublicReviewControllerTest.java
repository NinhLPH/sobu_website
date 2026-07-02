package com.vn.sodu.review.controller;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.review.ReviewService;
import com.vn.sodu.review.ReviewStatus;
import com.vn.sodu.review.dto.ReviewResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Test
    void getProductReviewsReturnsOnlyPublished() {
        ReviewResponseDto dto = ReviewResponseDto.builder()
                .id(1L)
                .productId(100L)
                .rating(5)
                .status(ReviewStatus.PUBLISHED)
                .build();
        Page<ReviewResponseDto> page = new PageImpl<>(List.of(dto));

        when(reviewService.getPublicReviews(eq(100L), any(Pageable.class))).thenReturn(page);

        PublicReviewController controller = new PublicReviewController(reviewService);
        ResponseEntity<PageResponse<ReviewResponseDto>> response = controller.getProductReviews(100L, 0, 20, "createdAt", "DESC");

        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(response.getBody().getContent().get(0).getProductId()).isEqualTo(100L);
    }

    @Test
    void getProductReviewsRespectsPagination() {
        when(reviewService.getPublicReviews(eq(100L), any(Pageable.class))).thenReturn(Page.empty());

        PublicReviewController controller = new PublicReviewController(reviewService);
        ResponseEntity<PageResponse<ReviewResponseDto>> response = controller.getProductReviews(100L, 0, 5, "createdAt", "DESC");

        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getPageSize()).isEqualTo(0);
    }
}
