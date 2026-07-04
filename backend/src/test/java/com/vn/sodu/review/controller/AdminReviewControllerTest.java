package com.vn.sodu.review.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.review.ReviewService;
import com.vn.sodu.review.ReviewStatus;
import com.vn.sodu.review.dto.ReplyReviewRequest;
import com.vn.sodu.review.dto.ReviewResponseDto;
import com.vn.sodu.review.dto.UpdateReviewStatusRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Test
    void listReviewsReturnsPageWithStaffRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "staff@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_STAFF")));

        ReviewResponseDto dto = ReviewResponseDto.builder().id(1L).rating(5).build();
        Page<ReviewResponseDto> page = new PageImpl<>(List.of(dto));

        when(reviewService.getAdminReviews(isNull(), any(Pageable.class))).thenReturn(page);

        AdminReviewController controller = new AdminReviewController(reviewService);
        ResponseEntity<ApiResponseDTO<PageResponse<ReviewResponseDto>>> response =
                controller.listReviews(auth, null, 0, 20, "createdAt", "DESC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void listReviewsRejectsUserRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        AdminReviewController controller = new AdminReviewController(reviewService);

        assertThatThrownBy(() -> controller.listReviews(auth, null, 0, 20, "createdAt", "DESC"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getReviewReturnsReviewForStaff() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "staff@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_STAFF")));
        ReviewResponseDto dto = ReviewResponseDto.builder().id(1L).rating(4).build();

        when(reviewService.getReviewById(1L)).thenReturn(dto);

        AdminReviewController controller = new AdminReviewController(reviewService);
        ResponseEntity<ApiResponseDTO<ReviewResponseDto>> response = controller.getReview(1L, auth);

        assertThat(response.getBody().getData().getRating()).isEqualTo(4);
    }

    @Test
    void updateReviewStatusWorksForAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UpdateReviewStatusRequest req = new UpdateReviewStatusRequest();
        req.setStatus(ReviewStatus.HIDDEN);

        ReviewResponseDto dto = ReviewResponseDto.builder().id(1L).status(ReviewStatus.HIDDEN).build();
        when(reviewService.updateReviewStatus(1L, req)).thenReturn(dto);

        AdminReviewController controller = new AdminReviewController(reviewService);
        ResponseEntity<ApiResponseDTO<ReviewResponseDto>> response = controller.updateReviewStatus(1L, req, auth);

        assertThat(response.getBody().getData().getStatus()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    void replyToReviewWorksForAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        ReplyReviewRequest req = new ReplyReviewRequest();
        req.setAdminReply("Thank you!");

        ReviewResponseDto dto = ReviewResponseDto.builder().id(1L).adminReply("Thank you!").build();
        when(reviewService.replyToReview(1L, req, "admin@example.com")).thenReturn(dto);

        AdminReviewController controller = new AdminReviewController(reviewService);
        ResponseEntity<ApiResponseDTO<ReviewResponseDto>> response = controller.replyToReview(1L, req, auth);

        assertThat(response.getBody().getData().getAdminReply()).isEqualTo("Thank you!");
    }

    @Test
    void deleteReviewWorksForAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        AdminReviewController controller = new AdminReviewController(reviewService);
        ResponseEntity<ApiResponseDTO<Void>> response = controller.deleteReview(1L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reviewService).deleteReview(1L);
    }
}
