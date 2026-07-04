import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { AdminReviewService, ReviewService } from './review.service';

const mockApiGet = jest.fn();
const mockApiPost = jest.fn();
const mockApiPut = jest.fn();
const mockApiDelete = jest.fn();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        get: (...args: any[]) => mockApiGet(...args),
        post: (...args: any[]) => mockApiPost(...args),
        put: (...args: any[]) => mockApiPut(...args),
        delete: (...args: any[]) => mockApiDelete(...args)
    }
}));

describe('ReviewService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('loads public product reviews from the raw page endpoint', () => {
        ReviewService.getPublicProductReviews(1001, { page: 0, size: 10 });

        expect(mockApiGet).toHaveBeenCalledWith(
            '/api/public/products/1001/reviews',
            { params: { page: 0, size: 10 } }
        );
    });

    it('uploads review files as multipart form data', () => {
        const file = new File(['image'], 'review.jpg', { type: 'image/jpeg' });

        ReviewService.uploadReviewFile(file);

        expect(mockApiPost).toHaveBeenCalledWith(
            '/api/reviews/files/upload',
            expect.any(FormData),
            { headers: { 'Content-Type': 'multipart/form-data' } }
        );
    });

    it('creates reviews with a product-only payload and no orderId', () => {
        ReviewService.createReview({
            productId: 1001,
            rating: 5,
            content: 'Sản phẩm tốt',
            imageUrls: ['/api/public/files/reviews/a.jpg']
        });

        expect(mockApiPost).toHaveBeenCalledWith('/api/reviews', {
            productId: 1001,
            rating: 5,
            content: 'Sản phẩm tốt',
            imageUrls: ['/api/public/files/reviews/a.jpg']
        });
        expect(mockApiPost).not.toHaveBeenCalledWith(
            '/api/reviews',
            expect.objectContaining({ orderId: expect.anything() })
        );
    });
});

describe('AdminReviewService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('filters admin reviews by status', () => {
        AdminReviewService.getReviews({ status: 'PENDING', page: 0, size: 20 });

        expect(mockApiGet).toHaveBeenCalledWith('/api/admin/reviews', {
            params: { status: 'PENDING', page: 0, size: 20 }
        });
    });

    it('approves or rejects reviews with the documented payload', () => {
        AdminReviewService.updateStatus(7, 'APPROVED');

        expect(mockApiPut).toHaveBeenCalledWith(
            '/api/admin/reviews/7/status',
            { status: 'APPROVED' }
        );
    });

    it('sends reply instead of the legacy adminReply field', () => {
        AdminReviewService.reply(7, 'Cảm ơn bạn');

        expect(mockApiPut).toHaveBeenCalledWith(
            '/api/admin/reviews/7/reply',
            { reply: 'Cảm ơn bạn' }
        );
    });

    it('deletes reviews permanently through the admin endpoint', () => {
        AdminReviewService.deleteReview(7);

        expect(mockApiDelete).toHaveBeenCalledWith('/api/admin/reviews/7');
    });
});
