import { PageResponse } from './api-response';

export type ReviewStatus = 'PUBLISHED' | 'HIDDEN';
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
    orderId: number;
    rating: number;
    content: string;
    imageUrls: string[];
}

export interface ReviewEligibilityResponse {
    canReview: boolean;
    reason: string;
    orderId?: number;
    alreadyReviewed: boolean;
    deliveredOrderFound: boolean;
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
