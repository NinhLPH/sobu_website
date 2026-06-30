import apiClient from '../api/api-client';
import {PageResponse} from '../interface/api-response';
import {StaticPageDTO, StaticPageMutationPayload} from '../interface/static-page.model';
import {UiSearchParams} from '../interface/public-ui-config.model';

export const StaticPageService = {
    searchPages: (searchTerm: string, params?: UiSearchParams): Promise<PageResponse<StaticPageDTO>> => {
        return apiClient.post('/api/admin/static-pages/search', {searchTerm, ...params});
    },

    getPageDetail: (id: string | number): Promise<StaticPageDTO> => {
        return apiClient.get(`/api/admin/static-pages/${id}`);
    },

    createPage: (data: StaticPageMutationPayload): Promise<StaticPageDTO> => {
        return apiClient.post('/api/admin/static-pages', data);
    },

    updatePage: (id: string | number, data: StaticPageMutationPayload): Promise<StaticPageDTO> => {
        return apiClient.put(`/api/admin/static-pages/${id}`, data);
    },

    deletePage: (id: string | number): Promise<void> => {
        return apiClient.delete(`/api/admin/static-pages/${id}`);
    },

    getPublishedPageBySlug: (slug: string): Promise<StaticPageDTO> => {
        return apiClient.get(`/api/public/pages/${slug}`);
    },
};
