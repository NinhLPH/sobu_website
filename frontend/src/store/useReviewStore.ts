import { create } from 'zustand';
import { PageResponse } from '../interface/api-response';
import { OrderResponseDto } from '../interface/order.model';
import { ProductModel } from '../interface/product.model';
import { ReviewResponseDto } from '../interface/review.model';
import { CustomerService } from '../service/custom.service';
import { ReviewService } from '../service/review.service';
import {
    canReviewProductFromOrder,
    ReviewEligibilityResult
} from '../utils/review-eligibility';

export type ReviewOrderLookupType = 'orderId' | 'nhanhOrderId';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

const defaultPage: Omit<PageResponse<ReviewResponseDto>, 'content'> = {
    pageNumber: 0,
    pageSize: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false
};

interface ReviewState {
    reviews: ReviewResponseDto[];
    reviewsPage: Omit<PageResponse<ReviewResponseDto>, 'content'>;
    isReviewsLoading: boolean;
    reviewsError: string | null;
    verifiedOrder: OrderResponseDto | null;
    verificationResult: ReviewEligibilityResult | null;
    isVerifyingOrder: boolean;
    verificationError: string | null;
    isSubmittingReview: boolean;
    submitError: string | null;
    submitSuccessMessage: string | null;

    fetchPublicReviews: (productId: string | number, page?: number) => Promise<void>;
    verifyOrderForProduct: (
        product: ProductModel,
        lookupType: ReviewOrderLookupType,
        reference: string
    ) => Promise<ReviewEligibilityResult>;
    submitReview: (
        productId: string | number,
        orderId: string | number,
        rating: number,
        content: string,
        files: File[]
    ) => Promise<ReviewResponseDto>;
    resetVerification: () => void;
    clearSubmitState: () => void;
}

export const useReviewStore = create<ReviewState>((set, get) => ({
    reviews: [],
    reviewsPage: defaultPage,
    isReviewsLoading: false,
    reviewsError: null,
    verifiedOrder: null,
    verificationResult: null,
    isVerifyingOrder: false,
    verificationError: null,
    isSubmittingReview: false,
    submitError: null,
    submitSuccessMessage: null,

    fetchPublicReviews: async (productId, page = 0) => {
        set({ isReviewsLoading: true, reviewsError: null });
        try {
            const response = await ReviewService.getPublicProductReviews(productId, {
                page,
                size: 10,
                sortBy: 'createdAt',
                sortDirection: 'DESC'
            });
            set({
                reviews: response.content || [],
                reviewsPage: {
                    pageNumber: response.pageNumber ?? page,
                    pageSize: response.pageSize ?? 10,
                    totalElements: response.totalElements ?? response.content?.length ?? 0,
                    totalPages: response.totalPages ?? 0,
                    first: response.first ?? page === 0,
                    last: response.last ?? true,
                    hasNext: response.hasNext ?? false,
                    hasPrevious: response.hasPrevious ?? page > 0
                },
                isReviewsLoading: false
            });
        } catch (error) {
            set({
                reviewsError: getErrorMessage(error, 'Không thể tải danh sách đánh giá.'),
                isReviewsLoading: false
            });
        }
    },

    verifyOrderForProduct: async (product, lookupType, reference) => {
        const normalizedReference = reference.trim();
        if (!normalizedReference) {
            const result = {
                canReview: false,
                reason: 'Vui lòng nhập mã đơn hàng để xác minh.'
            };
            set({
                verifiedOrder: null,
                verificationResult: result,
                verificationError: result.reason
            });
            return result;
        }

        set({
            isVerifyingOrder: true,
            verificationError: null,
            verificationResult: null,
            verifiedOrder: null,
            submitError: null,
            submitSuccessMessage: null
        });

        try {
            const response = lookupType === 'orderId'
                ? await CustomerService.getMyOrder(normalizedReference)
                : await CustomerService.getOrderByNhanhId(normalizedReference);
            const order = response.data;
            const result = canReviewProductFromOrder(order, product);
            set({
                verifiedOrder: result.canReview ? order : null,
                verificationResult: result,
                verificationError: result.canReview ? null : result.reason,
                isVerifyingOrder: false
            });
            return result;
        } catch (error) {
            const message = getErrorMessage(error, 'Không thể xác minh đơn hàng này.');
            const result = { canReview: false, reason: message };
            set({
                verifiedOrder: null,
                verificationResult: result,
                verificationError: message,
                isVerifyingOrder: false
            });
            return result;
        }
    },

    submitReview: async (productId, orderId, rating, content, files) => {
        const numericProductId = Number(productId);
        if (!Number.isFinite(numericProductId)) {
            throw new Error('Mã sản phẩm không hợp lệ.');
        }
        const numericOrderId = Number(orderId);
        if (!Number.isFinite(numericOrderId)) {
            throw new Error('Mã đơn hàng không hợp lệ.');
        }

        set({
            isSubmittingReview: true,
            submitError: null,
            submitSuccessMessage: null
        });

        try {
            const imageUrls: string[] = [];
            for (const file of files) {
                const response = await ReviewService.uploadReviewFile(file);
                if (response.data?.url) {
                    imageUrls.push(response.data.url);
                }
            }

            const response = await ReviewService.createReview({
                productId: numericProductId,
                orderId: numericOrderId,
                rating,
                content: content.trim(),
                imageUrls
            });
            await get().fetchPublicReviews(numericProductId, 0);

            set({
                isSubmittingReview: false,
                submitSuccessMessage: 'Đánh giá của bạn đã được đăng và hiển thị công khai.',
                submitError: null
            });
            return response.data;
        } catch (error) {
            const message = getErrorMessage(error, 'Không thể gửi đánh giá lúc này.');
            set({
                isSubmittingReview: false,
                submitError: message
            });
            throw error;
        }
    },

    resetVerification: () => set({
        verifiedOrder: null,
        verificationResult: null,
        verificationError: null,
        submitError: null,
        submitSuccessMessage: null
    }),

    clearSubmitState: () => set({
        submitError: null,
        submitSuccessMessage: null
    })
}));
