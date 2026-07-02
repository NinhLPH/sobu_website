package com.vn.sodu.review.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.review.ReviewService;
import com.vn.sodu.review.ReviewStatus;
import com.vn.sodu.review.dto.ReplyReviewRequest;
import com.vn.sodu.review.dto.ReviewResponseDto;
import com.vn.sodu.review.dto.UpdateReviewStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponse<ReviewResponseDto>>> listReviews(
            Authentication authentication,
            @RequestParam(name = "status", required = false) ReviewStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection) {
        requireStaff(authentication);
        Sort sort = sortDirection.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<ReviewResponseDto> result = reviewService.getAdminReviews(status, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(PageResponse.from(result), "Reviews retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ReviewResponseDto>> getReview(
            @PathVariable Long id,
            Authentication authentication) {
        requireStaff(authentication);
        ReviewResponseDto data = reviewService.getReviewById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Review retrieved"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponseDTO<ReviewResponseDto>> updateReviewStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewStatusRequest request,
            Authentication authentication) {
        requireStaff(authentication);
        ReviewResponseDto data = reviewService.updateReviewStatus(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Review status updated"));
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ApiResponseDTO<ReviewResponseDto>> replyToReview(
            @PathVariable Long id,
            @Valid @RequestBody ReplyReviewRequest request,
            Authentication authentication) {
        requireStaff(authentication);
        String adminEmail = authentication.getName();
        ReviewResponseDto data = reviewService.replyToReview(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Review reply saved"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        requireStaff(authentication);
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Review deleted"));
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String name = authority.getAuthority().toUpperCase(Locale.ROOT);
            if (name.equals("ROLE_ADMIN") || name.equals("ROLE_STAFF")) {
                return true;
            }
        }
        return false;
    }

    private void requireStaff(Authentication authentication) {
        if (!isStaff(authentication)) {
            throw new AccessDeniedException("Staff access is required");
        }
    }
}
