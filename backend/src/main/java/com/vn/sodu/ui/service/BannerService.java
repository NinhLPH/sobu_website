package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
    BannerDTO updateBanner(Long id, UpdateBannerRequest request, MultipartFile image);
    BannerDTO getBannerById(Long id);
    PageResponse<BannerDTO> getAllBanners(SearchRequest request);
    List<BannerDTO> getActiveBanners(Banner.DeviceType deviceType, String position);
}
