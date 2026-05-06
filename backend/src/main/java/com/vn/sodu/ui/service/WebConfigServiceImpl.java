package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.WebConfigRepo;
import com.vn.sodu.ui.WebsiteConfiguration;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.dto.WebsiteConfigurationRequest;
import com.vn.sodu.ui.mapper.WebsiteConfigurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebConfigServiceImpl implements WebConfigService {

    private final WebConfigRepo webConfigRepo;
    private final WebsiteConfigurationMapper mapper;

    @Override
    @Transactional
    public WebsiteConfigurationDTO createConfig(WebsiteConfigurationRequest request) {
        WebsiteConfiguration entity = mapper.toEntity(request);
        WebsiteConfiguration saved = webConfigRepo.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public WebsiteConfigurationDTO updateConfig(Long id, WebsiteConfigurationRequest request) {
        WebsiteConfiguration entity = webConfigRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Web configuration not found with id: " + id));
        mapper.updateEntity(entity, request);
        WebsiteConfiguration updated = webConfigRepo.save(entity);
        return mapper.toDTO(updated);
    }

    @Override
    public WebsiteConfigurationDTO getConfigById(Long id) {
        WebsiteConfiguration entity = webConfigRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Web configuration not found with id: " + id));
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        WebsiteConfiguration entity = webConfigRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Web configuration not found with id: " + id));
        if (entity.getIsActive() != null && entity.getIsActive()) {
            entity.setIsActive(false);
            webConfigRepo.save(entity);
        } else {
            webConfigRepo.delete(entity);
        }
    }

    @Override
    public PageResponse<WebsiteConfigurationDTO> getAllConfigs(SearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, request.getSortBy());
        }

        Pageable pageable = PageRequest.of(request.getPage() > 0 ? request.getPage() - 1 : 0, 
                                           request.getPageSize() > 0 ? request.getPageSize() : 10, 
                                           sort);

        Specification<WebsiteConfiguration> spec = (root, query, cb) -> cb.conjunction();

        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            String searchPattern = "%" + request.getSearchTerm().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("key")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern)
            ));
        }

        Page<WebsiteConfiguration> page = webConfigRepo.findAll(spec, pageable);
        
        List<WebsiteConfigurationDTO> items = page.getContent().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<WebsiteConfigurationDTO>builder()
                .content(items)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    public List<WebsiteConfigurationDTO> getPublicConfigs() {
        Specification<WebsiteConfiguration> spec = (root, query, cb) -> cb.and(
            cb.isTrue(root.get("isPublic")),
            cb.isTrue(root.get("isActive"))
        );
        return webConfigRepo.findAll(spec).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WebsiteConfigurationDTO getConfigByKey(String key) {
        Specification<WebsiteConfiguration> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get("key"), key),
            cb.isTrue(root.get("isActive"))
        );
        return webConfigRepo.findOne(spec)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Web configuration not found with key: " + key));
    }
}
