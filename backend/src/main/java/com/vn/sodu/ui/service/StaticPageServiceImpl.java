package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.ui.StaticPage;
import com.vn.sodu.ui.StaticPageRepo;
import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.dto.StaticPageRequest;
import com.vn.sodu.ui.mapper.StaticPageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StaticPageServiceImpl implements StaticPageService {

    private static final Pattern VIETNAMESE_D = Pattern.compile("[đĐ]");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+|-+$)");
    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final StaticPageRepo staticPageRepo;
    private final StaticPageMapper mapper;
    private final StaticPageHtmlSanitizer sanitizer;

    @Override
    @Transactional
    public StaticPageDTO createPage(StaticPageRequest request) {
        validateRequest(request);
        String slug = normalizeSlug(request.getSlug());
        if (staticPageRepo.existsBySlug(slug)) {
            throw new BadRequestException("Static page slug already exists: " + slug);
        }

        StaticPage page = StaticPage.builder()
                .slug(slug)
                .title(request.getTitle().trim())
                .htmlContent(sanitizer.sanitize(request.getHtmlContent()))
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();
        return mapper.toDTO(staticPageRepo.save(page));
    }

    @Override
    @Transactional
    public StaticPageDTO updatePage(Long id, StaticPageRequest request) {
        validateRequest(request);
        StaticPage page = staticPageRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Static page not found with id: " + id));
        String slug = normalizeSlug(request.getSlug());
        if (staticPageRepo.existsBySlugAndIdNot(slug, id)) {
            throw new BadRequestException("Static page slug already exists: " + slug);
        }

        page.setSlug(slug);
        page.setTitle(request.getTitle().trim());
        page.setHtmlContent(sanitizer.sanitize(request.getHtmlContent()));
        page.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : false);
        return mapper.toDTO(staticPageRepo.save(page));
    }

    @Override
    public StaticPageDTO getPageById(Long id) {
        return staticPageRepo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Static page not found with id: " + id));
    }

    @Override
    public StaticPageDTO getPublishedPageBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        return staticPageRepo.findBySlugAndIsPublishedTrue(normalizedSlug)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Static page not found with slug: " + normalizedSlug));
    }

    @Override
    public PageResponse<StaticPageDTO> searchPages(SearchRequest request) {
        int page = request != null && request.getPage() > 0 ? request.getPage() - 1 : 0;
        int pageSize = request != null && request.getPageSize() > 0 ? request.getPageSize() : 10;
        Sort sort = buildSort(request);
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Specification<StaticPage> spec = (root, query, cb) -> cb.conjunction();
        String searchTerm = request != null ? request.getSearchTerm() : null;
        if (searchTerm != null && !searchTerm.isBlank()) {
            String searchPattern = "%" + searchTerm.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("slug")), searchPattern),
                    cb.like(cb.lower(root.get("title")), searchPattern)
            ));
        }

        Page<StaticPageDTO> dtoPage = staticPageRepo.findAll(spec, pageable).map(mapper::toDTO);
        return PageResponse.<StaticPageDTO>builder()
                .content(dtoPage.getContent())
                .pageNumber(dtoPage.getNumber() + 1)
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .last(dtoPage.isLast())
                .hasNext(dtoPage.hasNext())
                .hasPrevious(dtoPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional
    public void deletePage(Long id) {
        StaticPage page = staticPageRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Static page not found with id: " + id));
        staticPageRepo.delete(page);
    }

    public String normalizeSlug(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Static page slug is required");
        }
        String withoutVietnameseD = VIETNAMESE_D.matcher(value.trim()).replaceAll("d");
        String normalized = Normalizer.normalize(withoutVietnameseD, Normalizer.Form.NFD);
        String withoutAccents = DIACRITICS.matcher(normalized).replaceAll("");
        String slug = NON_SLUG_CHARS.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = EDGE_DASHES.matcher(slug).replaceAll("");
        if (slug.isBlank() || slug.length() > 160 || !VALID_SLUG.matcher(slug).matches()) {
            throw new BadRequestException("Static page slug is invalid");
        }
        return slug;
    }

    private void validateRequest(StaticPageRequest request) {
        if (request == null) {
            throw new BadRequestException("Static page payload is required");
        }
        normalizeSlug(request.getSlug());
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Static page title is required");
        }
        if (request.getTitle().trim().length() > 255) {
            throw new BadRequestException("Static page title must not exceed 255 characters");
        }
    }

    private Sort buildSort(SearchRequest request) {
        String sortBy = request != null && request.getSortBy() != null && !request.getSortBy().isBlank()
                ? request.getSortBy()
                : "updatedAt";
        if (!sortBy.equals("id") && !sortBy.equals("slug") && !sortBy.equals("title")
                && !sortBy.equals("createdAt") && !sortBy.equals("updatedAt")) {
            sortBy = "updatedAt";
        }
        Sort.Direction direction = request != null && "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }
}
