import { create } from 'zustand';
import { PageResponse } from '../interface/api-response';
import {
    AdminReviewQueryParams,
    ReviewResponseDto,
    ReviewStatus
} from '../interface/review.model';
import { AdminReviewService } from '../service/review.service';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

const defaultPage: Omit<PageResponse<ReviewResponseDto>, 'content'> = {
    pageNumber: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false
};

interface AdminReviewState {
    reviews: ReviewResponseDto[];
    reviewsPage: Omit<PageResponse<ReviewResponseDto>, 'content'>;
    activeStatus: ReviewStatus | 'ALL';
    isLoading: boolean;
    error: string | null;
    actionReviewIds: number[];

    setActiveStatus: (status: ReviewStatus | 'ALL') => void;
    fetchReviews: (params?: AdminReviewQueryParams) => Promise<void>;
    updateReviewStatus: (reviewId: number, status: ReviewStatus) => Promise<ReviewResponseDto>;
    replyToReview: (reviewId: number, reply: string) => Promise<ReviewResponseDto>;
    deleteReview: (reviewId: number) => Promise<void>;
    clearError: () => void;
}

export const useAdminReviewStore = create<AdminReviewState>((set) => ({
    reviews: [],
    reviewsPage: defaultPage,
    activeStatus: 'ALL',
    isLoading: false,
    error: null,
    actionReviewIds: [],

    setActiveStatus: (status) => set({ activeStatus: status }),

    fetchReviews: async (params) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AdminReviewService.getReviews({
                page: 0,
                size: 20,
                sortBy: 'createdAt',
                sortDirection: 'DESC',
                ...params
            });
            const data = response.data;
            set({
                reviews: data.content || [],
                reviewsPage: {
                    pageNumber: data.pageNumber ?? params?.page ?? 0,
                    pageSize: data.pageSize ?? params?.size ?? 20,
                    totalElements: data.totalElements ?? data.content?.length ?? 0,
                    totalPages: data.totalPages ?? 0,
                    first: data.first ?? true,
                    last: data.last ?? true,
                    hasNext: data.hasNext ?? false,
                    hasPrevious: data.hasPrevious ?? false
                },
                isLoading: false
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải danh sách đánh giá.'),
                isLoading: false
            });
        }
    },

    updateReviewStatus: async (reviewId, status) => {
        set((state) => ({
            actionReviewIds: [...state.actionReviewIds, reviewId],
            error: null
        }));
        try {
            const response = await AdminReviewService.updateStatus(reviewId, status);
            const updatedReview = response.data;
            set((state) => ({
                reviews: state.reviews.map(review =>
                    review.id === reviewId ? { ...review, ...updatedReview } : review
                ),
                actionReviewIds: state.actionReviewIds.filter(id => id !== reviewId)
            }));
            return updatedReview;
        } catch (error) {
            set((state) => ({
                error: getErrorMessage(error, 'Không thể cập nhật trạng thái đánh giá.'),
                actionReviewIds: state.actionReviewIds.filter(id => id !== reviewId)
            }));
            throw error;
        }
    },

    replyToReview: async (reviewId, reply) => {
        set((state) => ({
            actionReviewIds: [...state.actionReviewIds, reviewId],
            error: null
        }));
        try {
            const response = await AdminReviewService.reply(reviewId, reply);
            const updatedReview = response.data;
            set((state) => ({
                reviews: state.reviews.map(review =>
                    review.id === reviewId ? { ...review, ...updatedReview } : review
                ),
                actionReviewIds: state.actionReviewIds.filter(id => id !== reviewId)
            }));
            return updatedReview;
        } catch (error) {
            set((state) => ({
                error: getErrorMessage(error, 'Không thể lưu phản hồi đánh giá.'),
                actionReviewIds: state.actionReviewIds.filter(id => id !== reviewId)
            }));
            throw error;
        }
    },

    deleteReview: async (reviewId) => {
        set((state) => ({
            actionReviewIds: [...state.actionReviewIds, reviewId],
            error: null
        }));
        try {
            await AdminReviewService.deleteReview(reviewId);
            set((state) => ({
                reviews: state.reviews.filter(review => review.id !== reviewId),
                actionReviewIds: state.actionReviewIds.filter(id => id !== reviewId)
            }));
        } catch (error) {
            set((state) => ({
                error: getErrorMessage(error, 'Không thể xóa đánh giá.'),
                actionReviewIds: state.actionReviewIds.filter(id => id !== reviewId)
            }));
            throw error;
        }
    },

    clearError: () => set({ error: null })
}));
