import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AdminSync from './Sync';
import { AdminWorkflowService } from '../../service/admin.service';
import { useAdminStore } from '../../store/useAdminStore';

jest.mock('../../service/admin.service');
jest.mock('../../service/toast.service');
jest.mock('react-router-dom', () => ({
    useSearchParams: () => [new URLSearchParams(), () => undefined]
}), { virtual: true });

const mockedAdminWorkflowService = jest.mocked(AdminWorkflowService);

const orderPage = (content: any[]) => ({
    success: true,
    message: 'Orders retrieved',
    data: {
        content,
        pageNumber: 0,
        pageSize: 100,
        totalElements: content.length,
        totalPages: 1
    }
});

describe('AdminSync order queue', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useAdminStore.setState({
            orderSyncQueue: [],
            pendingOrderSyncCount: 0,
            retryingOrderIds: [],
            isRetryingOrderSync: false,
            isOrderSyncQueueLoading: false,
            orderSyncQueueError: null,
            orderSyncBatchProgress: null,
            orderSyncBatchResult: null,
            ordersError: null,
            orderActionMessage: null
        });
    });

    it('selects actionable orders and retries them sequentially', async () => {
        mockedAdminWorkflowService.getAdminOrders
            .mockResolvedValueOnce(orderPage([
                { id: 1, orderCode: 'SO-1', syncStatus: 'FAILED', syncError: 'Timeout' },
                { id: 2, orderCode: 'SO-2', syncStatus: 'PENDING' },
                { id: 3, orderCode: 'SO-3', syncStatus: 'DEAD', syncError: 'Retry exhausted' }
            ]))
            .mockResolvedValueOnce(orderPage([]));
        mockedAdminWorkflowService.retryOrderSync
            .mockResolvedValueOnce({
                success: true,
                message: 'Synced SO-1',
                data: { orderId: 1, orderCode: 'SO-1', syncStatus: 'SYNCED' }
            })
            .mockResolvedValueOnce({
                success: true,
                message: 'Synced SO-3',
                data: { orderId: 3, orderCode: 'SO-3', syncStatus: 'SYNCED' }
            });

        render(<AdminSync />);

        expect(await screen.findByText('#SO-1')).toBeTruthy();
        expect(screen.getByText('#SO-3')).toBeTruthy();
        expect(screen.queryByText('#SO-2')).toBeNull();
        expect(screen.getByText(/Đang chờ tự động: 1/i)).toBeTruthy();

        fireEvent.click(screen.getByLabelText('Chọn tất cả đơn cần retry'));
        fireEvent.click(screen.getByRole('button', { name: /Retry đã chọn \(2\)/i }));

        await waitFor(() => expect(mockedAdminWorkflowService.retryOrderSync).toHaveBeenCalledTimes(2));
        expect(mockedAdminWorkflowService.retryOrderSync.mock.calls.map(call => call[0])).toEqual([1, 3]);
        expect(await screen.findByText(/Hoàn tất 2 đơn: 2 đã đồng bộ/i)).toBeTruthy();
    });
});
