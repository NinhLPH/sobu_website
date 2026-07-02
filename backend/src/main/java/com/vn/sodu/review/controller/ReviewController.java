package com.vn.sodu.review.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.review.ReviewService;
import com.vn.sodu.review.dto.CreateReviewRequest;
import com.vn.sodu.review.dto.ReviewResponseDto;
import com.vn.sodu.storage.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<ReviewResponseDto>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        ReviewResponseDto data = reviewService.createReview(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(data, "Review created", HttpStatus.CREATED.value()));
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.store(file, "reviews");
        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "File uploaded successfully"));
    }
}
