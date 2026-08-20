package com.vn.sodu.blog;

import com.vn.sodu.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/public/articles", "/api/v1/public/articles"})
@RequiredArgsConstructor
@Tag(name = "Public Articles", description = "Public blog & article endpoints for SEO")
public class PublicArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "Get published articles", description = "Returns a paginated list of published articles")
    public ResponseEntity<PageResponse<ArticleDTO>> getArticles(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ArticleDTO> page = articleService.getPublishedArticles(category, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get article detail by slug", description = "Returns full article content and SEO metadata by slug")
    public ResponseEntity<ArticleDetailDTO> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.getPublishedArticleBySlug(slug));
    }
}
