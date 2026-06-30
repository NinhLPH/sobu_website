package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.service.StaticPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/pages")
public class PublicStaticPageController {

    private final StaticPageService staticPageService;

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponseDTO<StaticPageDTO>> getPageBySlug(@PathVariable String slug) {
        StaticPageDTO page = staticPageService.getPublishedPageBySlug(slug);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Static page retrieved successfully"));
    }
}
