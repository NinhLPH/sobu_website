import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import AdminRequestDetail from './RequestDetails';
import { useRequestStore } from '../../store/useRequestStore';
import { ToastService } from '../../service/toast.service';
import { ProcessRequestDto, RequestResponseDto, UpdateRequestDto } from '../../interface/customer-request.model';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: any) => <a href={to}>{children}</a>,
    useParams: () => ({ id: '123' })
}), { virtual: true });
jest.mock('../../store/useRequestStore');
jest.mock('../../service/toast.service');
jest.mock('../../components/common/ImageUploader', () => ({ __esModule: true, default: () => <div data-testid="image-uploader" /> }));

const mockedUseRequestStore = jest.mocked(useRequestStore);
const mockedToast = jest.mocked(ToastService);
const getRequestDetail = jest.fn(async () => undefined);
const updateRequestAction = jest.fn(async (
    _id: string | number,
    _data: UpdateRequestDto,
    _role?: 'user' | 'admin'
): Promise<RequestResponseDto> => base);
const processRequestAction = jest.fn(async (
    _id: string | number,
    _data: ProcessRequestDto
): Promise<RequestResponseDto> => base);
const clearError = jest.fn();
const clearCurrentDetail = jest.fn();

const base: RequestResponseDto = {
    id: 123,
    requestCode: 'REQ-123',
    customerPhone: '0901234567',
    type: 'FINDING',
    status: 'SOURCING',
    totalAmount: 0,
    depositAmount: 0,
    customRequirements: 'Tìm mẫu hiếm',
    items: [{ id: 1, name: 'Rare figure', note: 'Bản gốc', price: 0, quantity: 1 }],
    attachments: [{ id: 1, url: '/requests/customer.jpg', type: 'IMAGE' }],
    createdAt: '2026-06-15T00:00:00Z',
    updatedAt: '2026-06-16T00:00:00Z'
};

const renderDetail = (detail: RequestResponseDto) => {
    updateRequestAction.mockResolvedValue(detail);
    processRequestAction.mockResolvedValue(detail);
    mockedUseRequestStore.mockReturnValue({
        currentRequestDetail: detail,
        getRequestDetail,
        updateRequestAction,
        processRequestAction,
        isLoading: false,
        isSubmitting: false,
        error: null,
        clearError,
        clearCurrentDetail
    } as ReturnType<typeof useRequestStore>);
    render(<AdminRequestDetail />);
};

beforeEach(() => {
    jest.clearAllMocks();
});

describe('AdminRequestDetail workflow', () => {
    it('saves quotation before moving FINDING to waiting customer and preserves customer images', async () => {
        renderDetail(base);
        fireEvent.change(screen.getByLabelText('Đơn giá (VND)'), { target: { value: '2000000' } });
        fireEvent.click(screen.getByRole('button', { name: /Gửi thông tin & báo giá/i }));

        await waitFor(() => expect(processRequestAction).toHaveBeenCalledWith(123, { targetStatus: 'WAITING_CUSTOMER' }));
        expect(updateRequestAction).toHaveBeenCalledWith(123, expect.objectContaining({
            totalAmount: 2000000,
            uploadedImageUrls: ['/requests/customer.jpg']
        }), 'admin');
        expect(updateRequestAction.mock.invocationCallOrder[0]).toBeLessThan(processRequestAction.mock.invocationCallOrder[0]);
        expect(mockedToast.success).toHaveBeenCalledWith('Đã lưu và gửi báo giá cho khách hàng.');
    });

    it('requires a valid deposit when approving after customer confirmation', async () => {
        renderDetail({ ...base, status: 'WAITING_CUSTOMER', totalAmount: 2000000 });
        fireEvent.click(screen.getByRole('button', { name: /Ghi nhận khách xác nhận & duyệt/i }));
        expect(screen.getByText(/Vui lòng nhập số tiền cọc/i)).toBeTruthy();
        expect(processRequestAction).not.toHaveBeenCalled();

        fireEvent.change(screen.getByLabelText('Số tiền cọc (VND)'), { target: { value: '300000' } });
        fireEvent.click(screen.getByRole('button', { name: /Ghi nhận khách xác nhận & duyệt/i }));
        await waitFor(() => expect(processRequestAction).toHaveBeenCalledWith(123, {
            targetStatus: 'APPROVED', note: undefined, depositAmount: 300000
        }));
    });

    it('approves the backend PREORDER SOURCING compatibility state', async () => {
        renderDetail({ ...base, type: 'PREORDER', status: 'SOURCING', totalAmount: 1000000 });
        fireEvent.change(screen.getByLabelText('Số tiền cọc (VND)'), { target: { value: '300000' } });
        fireEvent.click(screen.getByRole('button', { name: /Xác nhận còn suất & duyệt/i }));
        await waitFor(() => expect(processRequestAction).toHaveBeenCalledWith(123, {
            targetStatus: 'APPROVED', note: undefined, depositAmount: 300000
        }));
    });
});
