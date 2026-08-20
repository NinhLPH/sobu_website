package com.vn.sodu.blog;

import com.vn.sodu.seo.dto.SeoMetadataDTO;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {
    private Long id;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String thumbnailAlt;
    private String excerpt;
    private String authorName;
    private String category;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private SeoMetadataDTO seo;
}
