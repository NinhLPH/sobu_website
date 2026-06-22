import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import AdminRequests from './Requests';
import { useRequestStore } from '../../store/useRequestStore';
import { ToastService } from '../../service/toast.service';
import { RequestResponseDto } from '../../interface/customer-request.model';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    )
}), { virtual: true });

jest.mock('../../store/useRequestStore');
jest.mock('../../service/toast.service');
jest.mock('../../components/common/ImageUploader', () => ({
    __esModule: true,
    default: () => <div data-testid="image-uploader" />
}));

const mockedUseRequestStore = jest.mocked(useRequestStore);
const mockedToastService = jest.mocked(ToastService);

const requestDetail: RequestResponseDto = {
    id: 123,
    requestCode: 'REQ-001',
    customerPhone: '0901234567',
    type: 'FINDING',
    status: 'REVIEWING',
    totalAmount: 0,
    depositAmount: 0,
    customRequirements: 'Lap may theo ngan sach',
    items: [
        {
            id: 1,
            name: 'CPU',
            price: 0,
            quantity: 1
        },
        {
            id: 2,
            name: 'RAM',
            price: 0,
            quantity: 2
        }
    ],
    attachments: [],
    createdAt: '2026-06-15T00:00:00Z',
    updatedAt: '2026-06-15T00:00:00Z'
};

const fetchAdminRequests = jest.fn(async () => undefined);
const getRequestDetail = jest.fn(async () => undefined);
const updateRequestAction = jest.fn(async () => requestDetail);
const processRequestAction = jest.fn(async () => requestDetail);
const clearError = jest.fn();
const clearCurrentDetail = jest.fn();

const renderPage = (detail: RequestResponseDto = requestDetail) => {
    mockedUseRequestStore.mockReturnValue({
        adminRequests: [detail],
        adminRequestsPage: {
            pageNumber: 0,
            pageSize: 10,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false
        },
        currentRequestDetail: detail,
        fetchAdminRequests,
        getRequestDetail,
        updateRequestAction,
        processRequestAction,
        isLoading: false,
        isSubmitting: false,
        error: null,
        clearError,
        clearCurrentDetail
    } as ReturnType<typeof useRequestStore>);

    render(<AdminRequests />);
};

const fillQuotation = async (deposit = '0') => {
    const priceInputs = await screen.findAllByLabelText('Đơn giá (VND)');
    fireEvent.change(priceInputs[0], { target: { value: '1000000' } });
    fireEvent.change(priceInputs[1], { target: { value: '500000' } });
    fireEvent.change(screen.getByLabelText('Số tiền cọc (VND)'), {
        target: { value: deposit }
    });

    expect((screen.getByLabelText('Tổng tiền (VND)') as HTMLInputElement).value)
        .toBe('2000000');
};

beforeEach(() => {
    jest.clearAllMocks();
    fetchAdminRequests.mockResolvedValue(undefined);
    getRequestDetail.mockResolvedValue(undefined);
    updateRequestAction.mockResolvedValue(requestDetail);
    processRequestAction.mockResolvedValue({
        ...requestDetail,
        status: 'WAITING_CUSTOMER'
    } as RequestResponseDto);
});

describe('AdminRequests quotation form', () => {
    it('saves editable request data and accepts a zero deposit', async () => {
        renderPage();
        await fillQuotation();

        fireEvent.click(screen.getByRole('button', { name: /Lưu thay đổi/i }));

        await waitFor(() => {
            expect(updateRequestAction).toHaveBeenCalledWith(
                123,
                expect.objectContaining({
                    items: [
                        expect.objectContaining({
                            name: 'CPU',
                            price: 1000000,
                            quantity: 1
                        }),
                        expect.objectContaining({
                            name: 'RAM',
                            price: 500000,
                            quantity: 2
                        })
                    ],
                    totalAmount: 2000000,
                    depositAmount: 0,
                    customRequirements: 'Lap may theo ngan sach',
                    uploadedImageUrls: []
                }),
                'admin'
            );
        });
        expect(fetchAdminRequests).toHaveBeenCalledWith({ page: 0, size: 10 });
        expect(mockedToastService.success).toHaveBeenCalledWith('Đã lưu nháp báo giá.');
    });

    it('saves before waiting customer without sending a process deposit', async () => {
        renderPage();
        await fillQuotation('300000');

        fireEvent.click(screen.getByRole('button', { name: /Gửi báo giá cho khách/i }));

        await waitFor(() => {
            expect(processRequestAction).toHaveBeenCalledWith(123, {
                targetStatus: 'WAITING_CUSTOMER'
            });
        });
        expect(updateRequestAction).toHaveBeenCalledTimes(1);
        expect(updateRequestAction.mock.invocationCallOrder[0])
            .toBeLessThan(processRequestAction.mock.invocationCallOrder[0]);
    });

    it('sends the deposit directly only when approving', async () => {
        renderPage({
            ...requestDetail,
            totalAmount: 2000000
        });

        fireEvent.click(screen.getByRole('button', { name: /Cập nhật trạng thái/i }));
        const processForm = screen.getByText(/Duyệt Yêu Cầu/i).closest('form');
        expect(processForm).not.toBeNull();

        fireEvent.change(within(processForm!).getByRole('combobox'), {
            target: { value: 'APPROVED' }
        });
        fireEvent.change(within(processForm!).getByRole('spinbutton'), {
            target: { value: '0' }
        });
        fireEvent.click(within(processForm!).getByRole('button', { name: /Phê Duyệt/i }));

        await waitFor(() => {
            expect(processRequestAction).toHaveBeenCalledWith(123, {
                targetStatus: 'APPROVED',
                note: undefined,
                depositAmount: 0
            });
        });
        expect(updateRequestAction).not.toHaveBeenCalled();
        expect(fetchAdminRequests).toHaveBeenCalledWith({ page: 0, size: 10 });
    });
});
