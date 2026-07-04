import { PageResponse } from './api-response';

export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type ReviewSortDirection = 'ASC' | 'DESC';

export interface ReviewResponseDto {
    id: number;
    productId: number;
    orderId?: number;
    rating: number;
    content: string;
    imageUrls?: string[];
    status?: ReviewStatus;
    customerName?: string;
    adminReply?: string;
    repliedAt?: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface CreateReviewDto {
    productId: number;
    rating: number;
    content: string;
    imageUrls: string[];
}

export interface AdminReviewQueryParams {
    status?: ReviewStatus;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: ReviewSortDirection;
}

export interface ReviewUploadResponse {
    url: string;
}

export type ReviewPageResponse = PageResponse<ReviewResponseDto>;
