import apiClient from '../api/api-client';
import {PageResponse} from '../interface/api-response';
import {ArticleDTO, ArticleDetailDTO, ArticleMutationPayload} from '../interface/article.model';

export type ArticleListParams = {
    page?: number;
    size?: number;
    sort?: string;
    category?: string;
};

export const ArticleService = {
    getPublishedArticles: (params?: ArticleListParams): Promise<PageResponse<ArticleDTO>> =>
        apiClient.get('/api/public/articles', {params}),

    getPublishedArticle: (slug: string): Promise<ArticleDetailDTO> =>
        apiClient.get(`/api/public/articles/${encodeURIComponent(slug)}`),

    createArticle: (payload: ArticleMutationPayload): Promise<ArticleDetailDTO> =>
        apiClient.post('/api/admin/articles', payload),

    updateArticle: (id: number, payload: ArticleMutationPayload): Promise<ArticleDetailDTO> =>
        apiClient.put(`/api/admin/articles/${id}`, payload),

    deleteArticle: (id: number): Promise<void> =>
        apiClient.delete(`/api/admin/articles/${id}`),
};

