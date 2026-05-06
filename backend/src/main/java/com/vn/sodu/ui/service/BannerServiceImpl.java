package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.BannerRepo;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.CreateBannerRequest;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import com.vn.sodu.ui.mapper.BannerMapper;
import com.vn.sodu.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
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
public class BannerServiceImpl implements BannerService {

    private final BannerRepo bannerRepo;
    private final BannerMapper bannerMapper;
    private final StorageService storageService;

    @Override
    @Transactional
    public BannerDTO createBanner(CreateBannerRequest request, MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            String imageUrl = storageService.store(image, "banners");
            request.setImageUrl(imageUrl);
        }
        Banner banner = bannerMapper.toEntity(request);
        Banner savedBanner = bannerRepo.save(banner);
        return bannerMapper.toDTO(savedBanner);
    }

    @Override
    @Transactional
    public BannerDTO updateBanner(Long id, UpdateBannerRequest request, MultipartFile image) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
        if (image != null && !image.isEmpty()) {
            String imageUrl = storageService.store(image, "banners");
            request.setImageUrl(imageUrl);
        }
        bannerMapper.updateEntity(banner, request);
        Banner updatedBanner = bannerRepo.save(banner);
        return bannerMapper.toDTO(updatedBanner);
    }

    @Override
    public BannerDTO getBannerById(Long id) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
        return bannerMapper.toDTO(banner);
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
        if (banner.getIsActive() != null && banner.getIsActive()) {
            banner.setIsActive(false);
            bannerRepo.save(banner);
        } else {
            bannerRepo.delete(banner);
        }
    }

    @Override
    public PageResponse<BannerDTO> getAllBanners(SearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, request.getSortBy());
        }

        Pageable pageable = PageRequest.of(request.getPage() > 0 ? request.getPage() - 1 : 0, 
                                           request.getPageSize() > 0 ? request.getPageSize() : 10, 
                                           sort);

        Specification<Banner> spec = (root, query, cb) -> cb.conjunction();

        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + request.getSearchTerm().toLowerCase() + "%"));
        }

        Page<Banner> bannerPage = bannerRepo.findAll(spec, pageable);
        
        List<BannerDTO> items = bannerPage.getContent().stream()
                .map(bannerMapper::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<BannerDTO>builder()
                .content(items)
                .pageNumber(bannerPage.getNumber() + 1)
                .pageSize(bannerPage.getSize())
                .totalElements(bannerPage.getTotalElements())
                .totalPages(bannerPage.getTotalPages())
                .first(bannerPage.isFirst())
                .last(bannerPage.isLast())
                .hasNext(bannerPage.hasNext())
                .hasPrevious(bannerPage.hasPrevious())
                .build();
    }

    @Override
    public List<BannerDTO> getActiveBanners(Banner.DeviceType deviceType, Banner.BannerPosition position) {
        Specification<Banner> spec = (root, query, cb) -> {
            var predicate = cb.isTrue(root.get("isActive"));
            if (deviceType != null) {
                predicate = cb.and(predicate, cb.or(cb.equal(root.get("deviceType"), deviceType), cb.equal(root.get("deviceType"), Banner.DeviceType.ALL)));
            }
            if (position != null) {
                predicate = cb.and(predicate, cb.equal(root.get("position"), position));
            }
            return predicate;
        };

        return bannerRepo.findAll(spec, Sort.by(Sort.Direction.ASC, "displayOrder")).stream()
                .map(bannerMapper::toDTO)
                .collect(Collectors.toList());
    }
}
