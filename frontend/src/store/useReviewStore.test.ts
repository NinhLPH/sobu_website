import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { CustomerService } from '../service/custom.service';
import { ReviewService } from '../service/review.service';
import { useReviewStore } from './useReviewStore';

jest.mock('../service/custom.service');
jest.mock('../service/review.service');

const mockedCustomerService = jest.mocked(CustomerService);
const mockedReviewService = jest.mocked(ReviewService);

describe('useReviewStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useReviewStore.setState({
            reviews: [],
            isReviewsLoading: false,
            reviewsError: null,
            verifiedOrder: null,
            verificationResult: null,
            isVerifyingOrder: false,
            verificationError: null,
            isSubmittingReview: false,
            submitError: null,
            submitSuccessMessage: null
        });
    });

    it('verifies a single known order by internal order id', async () => {
        mockedCustomerService.getMyOrder.mockResolvedValue({
            success: true,
            message: 'Order retrieved',
            data: {
                id: 42,
                status: 'DELIVERED',
                items: [{ id: 1, productId: 1001, name: 'SOBU model', price: 100000, quantity: 1 }]
            }
        });

        const result = await useReviewStore.getState().verifyOrderForProduct(
            {
                id: '1001',
                name: 'SOBU model',
                price: 100000,
                brand: 'SOBU',
                imageUrl: '/model.jpg',
                description: 'Test product',
                stock: 5
            },
            'orderId',
            '42'
        );

        expect(mockedCustomerService.getMyOrder).toHaveBeenCalledWith('42');
        expect(mockedCustomerService.getOrderByNhanhId).not.toHaveBeenCalled();
        expect(result.canReview).toBe(true);
        expect(useReviewStore.getState().verifiedOrder?.id).toBe(42);
    });

    it('verifies a single known order by Nhanh id or code', async () => {
        mockedCustomerService.getOrderByNhanhId.mockResolvedValue({
            success: true,
            message: 'Order retrieved',
            data: {
                id: 43,
                status: 'DELIVERED',
                items: [{ id: 1, nhanhProductId: '9001001', name: 'SOBU model', price: 100000, quantity: 1 }]
            }
        });

        const result = await useReviewStore.getState().verifyOrderForProduct(
            {
                id: '1001',
                externalId: '9001001',
                nhanhProductId: '9001001',
                name: 'SOBU model',
                price: 100000,
                brand: 'SOBU',
                imageUrl: '/model.jpg',
                description: 'Test product',
                stock: 5
            },
            'nhanhOrderId',
            'NH-9001001'
        );

        expect(mockedCustomerService.getOrderByNhanhId).toHaveBeenCalledWith('NH-9001001');
        expect(mockedCustomerService.getMyOrder).not.toHaveBeenCalled();
        expect(result.canReview).toBe(true);
    });

    it('submits a product-only review payload after file upload', async () => {
        mockedReviewService.uploadReviewFile.mockResolvedValue({
            success: true,
            message: 'File uploaded',
            data: { url: '/api/public/files/reviews/a.jpg' }
        });
        mockedReviewService.createReview.mockResolvedValue({
            success: true,
            message: 'Review created',
            data: {
                id: 9,
                productId: 1001,
                rating: 5,
                content: 'Sản phẩm tốt',
                status: 'PENDING',
                imageUrls: ['/api/public/files/reviews/a.jpg']
            }
        });

        const file = new File(['image'], 'review.jpg', { type: 'image/jpeg' });
        await useReviewStore.getState().submitReview(1001, 5, ' Sản phẩm tốt ', [file]);

        expect(mockedReviewService.uploadReviewFile).toHaveBeenCalledWith(file);
        expect(mockedReviewService.createReview).toHaveBeenCalledWith({
            productId: 1001,
            rating: 5,
            content: 'Sản phẩm tốt',
            imageUrls: ['/api/public/files/reviews/a.jpg']
        });
        expect(mockedReviewService.createReview).not.toHaveBeenCalledWith(
            expect.objectContaining({ orderId: expect.anything() })
        );
        expect(useReviewStore.getState().submitSuccessMessage).toMatch(/chờ quản trị viên duyệt/i);
    });
});
