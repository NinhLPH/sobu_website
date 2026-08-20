package com.vn.sodu.blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Long> {
    Optional<Article> findFirstBySlugAndStatus(String slug, String status);
    default Optional<Article> findBySlugAndStatus(String slug, String status) {
        return findFirstBySlugAndStatus(slug, status);
    }
    Optional<Article> findFirstBySlug(String slug);
    default Optional<Article> findBySlug(String slug) {
        return findFirstBySlug(slug);
    }
    boolean existsBySlug(String slug);
    Page<Article> findByStatusOrderByPublishedAtDesc(String status, Pageable pageable);
    Page<Article> findByStatusAndCategoryOrderByPublishedAtDesc(String status, String category, Pageable pageable);
}
