import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProductReviewSection from './ProductReviewSection';
import { ProductModel } from '../../interface/product.model';
import { ReviewService } from '../../service/review.service';
import { useAuthStore } from '../../store/useAuthStore';
import { useReviewStore } from '../../store/useReviewStore';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => <a href={to}>{children}</a>,
    MemoryRouter: ({ children }: { children: React.ReactNode }) => <>{children}</>
}), { virtual: true });

jest.mock('../../service/review.service');

const mockedReviewService = jest.mocked(ReviewService);

const product: ProductModel = {
    id: '1001',
    externalId: '9001001',
    nhanhProductId: '9001001',
    name: 'SOBU model',
    price: 100000,
    brand: 'SOBU',
    imageUrl: '/model.jpg',
    description: 'Test product',
    stock: 5,
    rating: 4.5,
    reviewsCount: 2
};

const renderSection = () => render(
    <MemoryRouter>
        <ProductReviewSection product={product} />
    </MemoryRouter>
);

describe('ProductReviewSection', () => {
    beforeEach(() => {
        jest.clearAllMocks();
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
        useAuthStore.setState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
            error: null
        });
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

    it('asks guests to log in before checking review eligibility', async () => {
        renderSection();

        expect(screen.getByText('Đăng nhập')).toBeTruthy();
        await waitFor(() => expect(mockedReviewService.getPublicProductReviews).toHaveBeenCalledWith(
            '1001',
            expect.objectContaining({ page: 0 })
        ));
        expect(mockedReviewService.getReviewEligibility).not.toHaveBeenCalled();
    });

    it('keeps the form locked when no delivered purchase exists', async () => {
        useAuthStore.setState({
            user: { id: 1, email: 'customer@example.com', fullName: 'Customer' } as any,
            isAuthenticated: true
        });
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

        renderSection();

        await waitFor(() => expect(screen.getByText('Hãy mua hàng rồi mới đăng review')).toBeTruthy());
        expect(screen.queryByText('Gửi đánh giá')).toBeNull();
    });

    it('shows already reviewed state instead of the form', async () => {
        useAuthStore.setState({
            user: { id: 1, email: 'customer@example.com', fullName: 'Customer' } as any,
            isAuthenticated: true
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

        renderSection();

        await waitFor(() => expect(screen.getByText('Bạn đã đánh giá sản phẩm này')).toBeTruthy());
        expect(screen.queryByRole('button', { name: 'Gửi đánh giá' })).toBeNull();
    });

    it('opens the review form for an eligible delivered order', async () => {
        useAuthStore.setState({
            user: { id: 1, email: 'customer@example.com', fullName: 'Customer' } as any,
            isAuthenticated: true
        });
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

        renderSection();

        await waitFor(() => expect(screen.getByRole('button', { name: 'Gửi đánh giá' })).toBeTruthy());
        expect(screen.getByPlaceholderText('Chia sẻ cảm nhận của bạn về sản phẩm...')).toBeTruthy();
    });
});
