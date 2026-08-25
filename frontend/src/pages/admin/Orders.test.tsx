import {describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import AdminOrders from './Orders';
import {useAdminStore} from '../../store/useAdminStore';
import {useIntegrationStore} from '../../store/useIntegrationStore';

jest.mock('react-router-dom', () => ({
    Link: ({children, to, ...props}: {children: React.ReactNode; to: string}) => <a href={to} {...props}>{children}</a>,
}), {virtual: true});

jest.mock('../../store/useAdminStore');

const mockedUseAdminStore = jest.mocked(useAdminStore);

describe('AdminOrders search suggest', () => {
    it('selects an order suggestion and filters immediately', () => {
        useIntegrationStore.setState({nhanhEnabled: false, loaded: true, loading: false});
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

    it('hides sync controls in local mode but retains historical Nhanh code', () => {
        useIntegrationStore.setState({nhanhEnabled: false, loaded: true, loading: false});
        mockedUseAdminStore.mockReturnValue({
            workflowOrders: [{
                id: 1,
                orderCode: 'SO-001',
                status: 'NEW',
                syncStatus: 'FAILED',
                nhanhOrderCode: 'NH-001',
                syncError: 'Old error'
            }],
            fetchOrders: jest.fn(),
            retryOrderSync: jest.fn(),
            retryingOrderIds: [],
            isOrdersLoading: false,
            ordersError: null,
            ordersPage: {pageNumber: 0, pageSize: 10, totalElements: 1, totalPages: 1, first: true, last: true, hasNext: false, hasPrevious: false}
        } as any);

        render(<AdminOrders/>);

        expect(screen.getByText('Quản lý đơn hàng')).toBeTruthy();
        expect(screen.getByText(/Nhanh \(lịch sử\): NH-001/i)).toBeTruthy();
        expect(screen.queryByLabelText('Lọc theo trạng thái đồng bộ Nhanh.vn')).toBeNull();
        expect(screen.queryByLabelText(/Retry đồng bộ/i)).toBeNull();
    });
});
