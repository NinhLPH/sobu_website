import {describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import AdminOrders from './Orders';
import {useAdminStore} from '../../store/useAdminStore';

jest.mock('react-router-dom', () => ({
    Link: ({children, to, ...props}: {children: React.ReactNode; to: string}) => <a href={to} {...props}>{children}</a>,
}), {virtual: true});

jest.mock('../../store/useAdminStore');

const mockedUseAdminStore = jest.mocked(useAdminStore);

describe('AdminOrders search suggest', () => {
    it('selects an order suggestion and filters immediately', () => {
        mockedUseAdminStore.mockReturnValue({
            workflowOrders: [
                {
                    id: 1,
                    orderCode: 'SO-001',
                    customerName: 'Lan Nguyen',
                    customerMobile: '0901111111',
                    totalAmount: 100000,
                    status: 'NEW',
                    syncStatus: 'SYNCED',
                    createdAt: '2026-07-01T00:00:00Z',
                },
                {
                    id: 2,
                    orderCode: 'SO-002',
                    customerName: 'Minh Tran',
                    customerMobile: '0902222222',
                    totalAmount: 200000,
                    status: 'PROCESSING',
                    syncStatus: 'PENDING',
                    createdAt: '2026-07-02T00:00:00Z',
                },
            ],
            fetchOrders: jest.fn(),
            retryOrderSync: jest.fn(),
            retryingOrderIds: [],
            isOrdersLoading: false,
            ordersError: null,
            ordersPage: {
                pageNumber: 0,
                pageSize: 10,
                totalElements: 2,
                totalPages: 1,
                first: true,
                last: true,
                hasNext: false,
                hasPrevious: false,
            },
        } as any);

        render(<AdminOrders/>);

        fireEvent.change(screen.getByLabelText('Tìm kiếm đơn hàng quản trị'), {
            target: {value: 'minh'},
        });
        fireEvent.mouseDown(screen.getByRole('option', {name: /SO-002/i}));

        expect((screen.getByLabelText('Tìm kiếm đơn hàng quản trị') as HTMLInputElement).value).toBe('SO-002');
        expect(screen.getByText('#SO-002')).toBeTruthy();
        expect(screen.queryByText('#SO-001')).toBeNull();
    });
});
