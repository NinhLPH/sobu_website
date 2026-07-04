import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ReviewService } from '../service/review.service';
import { useReviewStore } from './useReviewStore';

jest.mock('../service/review.service');

const mockedReviewService = jest.mocked(ReviewService);

describe('useReviewStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useReviewStore.setState({
            reviews: [],
            isReviewsLoading: false,
            reviewsError: null,
            verifiedOrder: null,
            reviewEligibility: null,
            isCheckingEligibility: false,
            eligibilityError: null,
            isSubmittingReview: false,
            submitError: null,
            submitSuccessMessage: null
        });
    });

    it('opens review submission when backend eligibility returns a delivered matching order', async () => {
        mockedReviewService.getReviewEligibility.mockResolvedValue({
            success: true,
            message: 'Review eligibility retrieved',
            data: {
                canReview: true,
                reason: 'Đơn hàng đã giao hợp lệ.',
                orderId: 42,
                alreadyReviewed: false,
                deliveredOrderFound: true
            }
        });

        const result = await useReviewStore.getState().checkReviewEligibility(1001);

        expect(mockedReviewService.getReviewEligibility).toHaveBeenCalledWith(1001);
        expect(result.canReview).toBe(true);
        expect(useReviewStore.getState().verifiedOrder?.id).toBe(42);
        expect(useReviewStore.getState().eligibilityError).toBeNull();
    });

    it('keeps the review form locked when the user has not bought the product', async () => {
        mockedReviewService.getReviewEligibility.mockResolvedValue({
            success: true,
            message: 'Review eligibility retrieved',
            data: {
                canReview: false,
                reason: 'Hãy mua hàng rồi mới đăng review.',
                alreadyReviewed: false,
                deliveredOrderFound: false
            }
        });

        const result = await useReviewStore.getState().checkReviewEligibility(1001);

        expect(result.canReview).toBe(false);
        expect(useReviewStore.getState().verifiedOrder).toBeNull();
        expect(useReviewStore.getState().eligibilityError).toMatch(/mua hàng/i);
    });

    it('submits a published review with the eligible order id after file upload', async () => {
        mockedReviewService.uploadReviewFile.mockResolvedValue({
            success: true,
            message: 'File uploaded',
            data: { url: '/api/public/files/reviews/a.jpg' }
        });
        mockedReviewService.getPublicProductReviews.mockResolvedValue({
            content: [],
            pageNumber: 0,
            pageSize: 10,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false
        });
        mockedReviewService.getReviewEligibility.mockResolvedValue({
            success: true,
            message: 'Review eligibility retrieved',
            data: {
                canReview: false,
                reason: 'Bạn đã đánh giá sản phẩm này.',
                alreadyReviewed: true,
                deliveredOrderFound: false
            }
        });
        mockedReviewService.createReview.mockResolvedValue({
            success: true,
            message: 'Review created',
            data: {
                id: 9,
                productId: 1001,
                orderId: 42,
                rating: 5,
                content: 'Sản phẩm tốt',
                status: 'PUBLISHED',
                imageUrls: ['/api/public/files/reviews/a.jpg']
            }
        });

        const file = new File(['image'], 'review.jpg', { type: 'image/jpeg' });
        await useReviewStore.getState().submitReview(1001, 42, 5, ' Sản phẩm tốt ', [file]);

        expect(mockedReviewService.uploadReviewFile).toHaveBeenCalledWith(file);
        expect(mockedReviewService.createReview).toHaveBeenCalledWith({
            productId: 1001,
            orderId: 42,
            rating: 5,
            content: 'Sản phẩm tốt',
            imageUrls: ['/api/public/files/reviews/a.jpg']
        });
        expect(mockedReviewService.getPublicProductReviews).toHaveBeenCalledWith(
            1001,
            expect.objectContaining({ page: 0, size: 10 })
        );
        expect(mockedReviewService.getReviewEligibility).toHaveBeenCalledWith(1001);
        expect(useReviewStore.getState().submitSuccessMessage).toMatch(/hiển thị công khai/i);
    });
});
