import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { AdminReviewService } from '../service/review.service';
import { useAdminReviewStore } from './useAdminReviewStore';

jest.mock('../service/review.service');

const mockedAdminReviewService = jest.mocked(AdminReviewService);

describe('useAdminReviewStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useAdminReviewStore.setState({
            reviews: [],
            activeStatus: 'ALL',
            isLoading: false,
            error: null,
            actionReviewIds: []
        });
    });

    it('loads admin reviews with the selected status filter', async () => {
        mockedAdminReviewService.getReviews.mockResolvedValue({
            success: true,
            message: 'Reviews retrieved',
            data: {
                content: [
                    {
                        id: 1,
                        productId: 1001,
                        rating: 5,
                        content: 'Good',
                        status: 'PENDING'
                    }
                ],
                pageNumber: 0,
                pageSize: 20,
                totalElements: 1,
                totalPages: 1
            }
        });

        await useAdminReviewStore.getState().fetchReviews({ status: 'PENDING', page: 0, size: 20 });

        expect(mockedAdminReviewService.getReviews).toHaveBeenCalledWith(
            expect.objectContaining({ status: 'PENDING', page: 0, size: 20 })
        );
        expect(useAdminReviewStore.getState().reviews).toHaveLength(1);
        expect(useAdminReviewStore.getState().isLoading).toBe(false);
    });

    it('updates review status and clears row action loading', async () => {
        useAdminReviewStore.setState({
            reviews: [
                {
                    id: 1,
                    productId: 1001,
                    rating: 5,
                    content: 'Good',
                    status: 'PENDING'
                }
            ]
        });
        mockedAdminReviewService.updateStatus.mockResolvedValue({
            success: true,
            message: 'Review status updated',
            data: {
                id: 1,
                productId: 1001,
                rating: 5,
                content: 'Good',
                status: 'APPROVED'
            }
        });

        await useAdminReviewStore.getState().updateReviewStatus(1, 'APPROVED');

        expect(mockedAdminReviewService.updateStatus).toHaveBeenCalledWith(1, 'APPROVED');
        expect(useAdminReviewStore.getState().reviews[0].status).toBe('APPROVED');
        expect(useAdminReviewStore.getState().actionReviewIds).toEqual([]);
    });

    it('saves admin reply into the local review row', async () => {
        useAdminReviewStore.setState({
            reviews: [
                {
                    id: 2,
                    productId: 1002,
                    rating: 4,
                    content: 'Nice',
                    status: 'APPROVED'
                }
            ]
        });
        mockedAdminReviewService.reply.mockResolvedValue({
            success: true,
            message: 'Reply submitted',
            data: {
                id: 2,
                productId: 1002,
                rating: 4,
                content: 'Nice',
                status: 'APPROVED',
                adminReply: 'Cảm ơn bạn'
            }
        });

        await useAdminReviewStore.getState().replyToReview(2, 'Cảm ơn bạn');

        expect(mockedAdminReviewService.reply).toHaveBeenCalledWith(2, 'Cảm ơn bạn');
        expect(useAdminReviewStore.getState().reviews[0].adminReply).toBe('Cảm ơn bạn');
    });

    it('removes deleted reviews from local state', async () => {
        useAdminReviewStore.setState({
            reviews: [
                {
                    id: 3,
                    productId: 1003,
                    rating: 2,
                    content: 'Bad',
                    status: 'REJECTED'
                }
            ]
        });
        mockedAdminReviewService.deleteReview.mockResolvedValue({
            success: true,
            message: 'Review deleted'
        });

        await useAdminReviewStore.getState().deleteReview(3);

        expect(mockedAdminReviewService.deleteReview).toHaveBeenCalledWith(3);
        expect(useAdminReviewStore.getState().reviews).toEqual([]);
    });
});
