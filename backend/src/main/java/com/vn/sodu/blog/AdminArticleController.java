package com.vn.sodu.blog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/admin/articles", "/api/v1/admin/articles"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Articles", description = "Admin CRUD for blog & articles")
public class AdminArticleController {

    private final ArticleService articleService;

    @PostMapping
    @Operation(summary = "Create article")
    public ResponseEntity<ArticleDetailDTO> create(@RequestBody ArticleDetailDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.createArticle(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update article")
    public ResponseEntity<ArticleDetailDTO> update(@PathVariable Long id, @RequestBody ArticleDetailDTO dto) {
        return ResponseEntity.ok(articleService.updateArticle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete article")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
