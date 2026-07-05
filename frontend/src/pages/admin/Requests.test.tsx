import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import AdminRequests from './Requests';
import {useRequestStore} from '../../store/useRequestStore';
import {RequestResponseDto} from '../../interface/customer-request.model';

jest.mock('react-router-dom', () => ({
    Link: ({children, to, ...props}: {children: React.ReactNode; to: string}) => <a href={to} {...props}>{children}</a>,
}), {virtual: true});

jest.mock('../../store/useRequestStore');

const mockedUseRequestStore = jest.mocked(useRequestStore);
const fetchAdminRequests = jest.fn(async () => undefined);

const requests: RequestResponseDto[] = [
    {
        id: 1,
        requestCode: 'REQ-001',
        customerPhone: '0901111111',
        type: 'FINDING',
        status: 'REVIEWING',
        totalAmount: 0,
        depositAmount: 0,
        customRequirements: '',
        items: [{id: 1, name: 'Gundam RX-78', price: 0, quantity: 1}],
        attachments: [],
        createdAt: '2026-07-01T00:00:00Z',
        updatedAt: '2026-07-01T00:00:00Z',
    },
    {
        id: 2,
        requestCode: 'REQ-002',
        customerPhone: '0902222222',
        type: 'CUSTOM',
        status: 'WAITING_CUSTOMER',
        totalAmount: 1500000,
        depositAmount: 0,
        customRequirements: '',
        items: [{id: 2, name: 'Tomica Limited', price: 0, quantity: 1}],
        attachments: [],
        createdAt: '2026-07-02T00:00:00Z',
        updatedAt: '2026-07-02T00:00:00Z',
    },
];

describe('AdminRequests search suggest', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedUseRequestStore.mockReturnValue({
            adminRequests: requests,
            adminRequestsPage: {
                pageNumber: 0,
                pageSize: 20,
                totalElements: 2,
                totalPages: 1,
                first: true,
                last: true,
                hasNext: false,
                hasPrevious: false,
            },
            fetchAdminRequests,
            isLoading: false,
            error: null,
        } as any);
    });

    it('selects a request suggestion and filters immediately', () => {
        render(<AdminRequests/>);

        fireEvent.change(screen.getByLabelText('Tìm trong trang hiện tại'), {
            target: {value: 'tomica'},
        });
        fireEvent.mouseDown(screen.getByRole('option', {name: /REQ-002/i}));

        expect((screen.getByLabelText('Tìm trong trang hiện tại') as HTMLInputElement).value).toBe('REQ-002');
        expect(screen.getAllByText('#REQ-002').length).toBeGreaterThan(0);
        expect(screen.queryByText('#REQ-001')).toBeNull();
    });
});
