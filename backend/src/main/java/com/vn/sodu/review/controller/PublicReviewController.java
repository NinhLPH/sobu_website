package com.vn.sodu.review.controller;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.review.ReviewService;
import com.vn.sodu.review.dto.ReviewResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/public/products/{id}/reviews")
    public ResponseEntity<PageResponse<ReviewResponseDto>> getProductReviews(
            @PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<ReviewResponseDto> result = reviewService.getPublicReviews(id, pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/api/public/reviews")
    public ResponseEntity<PageResponse<ReviewResponseDto>> getLatestReviews(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 6), sort);
        Page<ReviewResponseDto> result = reviewService.getLatestPublicReviews(pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }
}
