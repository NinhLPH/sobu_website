import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import MyOrders from './MyOrders';
import { CustomerService } from '../service/custom.service';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => <a href={to}>{children}</a>,
    useNavigate: () => mockNavigate
}), { virtual: true });
jest.mock('../service/custom.service');

const mockedCustomerService = jest.mocked(CustomerService);

describe('MyOrders', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedCustomerService.getMyOrders.mockResolvedValue({
            success: true,
            message: 'Orders retrieved',
            data: {
                content: [{
                    id: 12,
                    orderCode: 'SOBU-12',
                    status: 'PROCESSING',
                    totalAmount: 350000,
                    paymentStatus: 'PENDING',
                    createdAt: '2026-07-13T10:00:00'
                }],
                pageNumber: 0,
                pageSize: 10,
                totalElements: 1,
                totalPages: 1,
                first: true,
                last: true,
                hasNext: false,
                hasPrevious: false
            }
        });
    });

    it('loads customer-scoped orders without sync filters or sync status UI', async () => {
        render(<MyOrders />);

        await waitFor(() => expect(mockedCustomerService.getMyOrders).toHaveBeenCalledWith({
            page: 0,
            size: 10,
            query: undefined,
            status: undefined,
            createdFrom: undefined,
            createdTo: undefined,
            sortBy: 'createdAt',
            sortDirection: 'DESC'
        }));
        expect(await screen.findByText('#SOBU-12')).toBeTruthy();
        expect(screen.queryByText('Đồng bộ')).toBeNull();
        expect(document.querySelector('a[href="/orders/12"]')).not.toBeNull();
    });

    it('routes Nhanh lookup through the order detail experience', async () => {
        render(<MyOrders />);

        await screen.findByText('#SOBU-12');

        fireEvent.click(screen.getByRole('button', { name: 'Nhanh ID / code' }));
        fireEvent.change(screen.getByLabelText('Tra cứu theo mã đơn'), { target: { value: 'NH-123' } });
        fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }));

        expect(mockNavigate).toHaveBeenCalledWith('/orders/lookup?nhanhOrderId=NH-123');
    });
});
