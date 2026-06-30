package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.dto.StaticPageRequest;

public interface StaticPageService {
    StaticPageDTO createPage(StaticPageRequest request);
    StaticPageDTO updatePage(Long id, StaticPageRequest request);
    StaticPageDTO getPageById(Long id);
    StaticPageDTO getPublishedPageBySlug(String slug);
    PageResponse<StaticPageDTO> searchPages(SearchRequest request);
    void deletePage(Long id);
}
