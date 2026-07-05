import apiClient from '../api/api-client';
import { ApiResponseDTO, ApiResponseWithoutDataDTO } from '../interface/api-response';
import {
    AdminReviewQueryParams,
    CreateReviewDto,
    ReviewEligibilityResponse,
    ReviewPageResponse,
    ReviewResponseDto,
    ReviewStatus,
    ReviewUploadResponse
} from '../interface/review.model';

export const ReviewService = {
    getPublicProductReviews: (
        productId: string | number,
        params?: Omit<AdminReviewQueryParams, 'status'>
    ): Promise<ReviewPageResponse> => {
        return apiClient.get(`/api/public/products/${productId}/reviews`, { params });
    },

    getLatestPublicReviews: (
        params?: Omit<AdminReviewQueryParams, 'status'>
    ): Promise<ReviewPageResponse> => {
        return apiClient.get('/api/public/reviews', { params });
    },

    uploadReviewFile: (
        file: File
    ): Promise<ApiResponseDTO<ReviewUploadResponse>> => {
        const formData = new FormData();
        formData.append('file', file);

        return apiClient.post('/api/reviews/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },

    createReview: (
        data: CreateReviewDto
    ): Promise<ApiResponseDTO<ReviewResponseDto>> => {
        return apiClient.post('/api/reviews', data);
    },

    getReviewEligibility: (
        productId: string | number
    ): Promise<ApiResponseDTO<ReviewEligibilityResponse>> => {
        return apiClient.get(`/api/reviews/products/${productId}/eligibility`);
    }
};

export const AdminReviewService = {
    getReviews: (
        params?: AdminReviewQueryParams
    ): Promise<ApiResponseDTO<ReviewPageResponse>> => {
        return apiClient.get('/api/admin/reviews', { params });
    },

    updateStatus: (
        reviewId: string | number,
        status: ReviewStatus
    ): Promise<ApiResponseDTO<ReviewResponseDto>> => {
        return apiClient.put(`/api/admin/reviews/${reviewId}/status`, { status });
    },

    reply: (
        reviewId: string | number,
        reply: string
    ): Promise<ApiResponseDTO<ReviewResponseDto>> => {
        return apiClient.put(`/api/admin/reviews/${reviewId}/reply`, { adminReply: reply });
    },

    deleteReview: (
        reviewId: string | number
    ): Promise<ApiResponseWithoutDataDTO> => {
        return apiClient.delete(`/api/admin/reviews/${reviewId}`);
    }
};
