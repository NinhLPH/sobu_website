import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import RequestDetail from './RequestDetail';
import { useRequestStore } from '../store/useRequestStore';
import { useProductStore } from '../store/useProductStore';
import {
    RequestResponseDto,
    UpdateRequestDto
} from '../interface/customer-request.model';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    ),
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: '123' })
}), { virtual: true });
jest.mock('../store/useRequestStore');
jest.mock('../store/useProductStore');
jest.mock('../service/toast.service');
jest.mock('../components/common/ImageUploader', () => ({
    __esModule: true,
    default: () => <div data-testid="image-uploader" />
}));

const mockedUseRequestStore = jest.mocked(useRequestStore);
const mockedUseProductStore = jest.mocked(useProductStore);
const getRequestDetail = jest.fn(async () => undefined);
const clearError = jest.fn();
const clearCurrentDetail = jest.fn();
const fetchAllProducts = jest.fn(async () => undefined);

const detail: RequestResponseDto = {
    id: 123,
    requestCode: 'REQ-123',
    customerPhone: '0901234567',
    type: 'CUSTOM',
    status: 'WAITING_CUSTOMER',
    totalAmount: 2000000,
    depositAmount: 300000,
    customRequirements: 'Original requirements',
    items: [
        {
            id: 1,
            name: 'Custom model',
            note: 'Original note',
            price: 2000000,
            quantity: 1
        }
    ],
    attachments: [
        {
            id: 1,
            url: '/requests/reference.jpg',
            type: 'IMAGE'
        }
    ],
    createdAt: '2026-06-15T00:00:00Z',
    updatedAt: '2026-06-15T00:00:00Z'
};
const updateRequestAction = jest.fn(async (
    _id: string | number,
    _data: UpdateRequestDto,
    _role?: 'user' | 'admin'
) => detail);

beforeEach(() => {
    jest.clearAllMocks();
    mockedUseRequestStore.mockReturnValue({
        currentRequestDetail: detail,
        getRequestDetail,
        updateRequestAction,
        isLoading: false,
        isSubmitting: false,
        error: null,
        clearError,
        clearCurrentDetail
    } as ReturnType<typeof useRequestStore>);
    mockedUseProductStore.mockReturnValue({
        allProducts: [],
        fetchAllProducts,
        isAllProductsLoading: false
    } as ReturnType<typeof useProductStore>);
});

describe('RequestDetail customer editing', () => {
    it('keeps quote fields read-only and omits all pricing from the update payload', async () => {
        render(<RequestDetail />);

        const phoneInput = screen.getByDisplayValue('0901234567') as HTMLInputElement;
        const typeInput = screen.getByDisplayValue('Độ chế / ráp custom') as HTMLInputElement;
        expect(phoneInput.disabled).toBe(true);
        expect(typeInput.disabled).toBe(true);
        expect(fetchAllProducts).toHaveBeenCalledTimes(1);

        fireEvent.change(screen.getByDisplayValue('Original requirements'), {
            target: { value: 'Updated requirements' }
        });
        fireEvent.click(screen.getByRole('button', { name: /Lưu thay đổi/i }));

        await waitFor(() => {
            expect(updateRequestAction).toHaveBeenCalledWith(
                '123',
                {
                    customRequirements: 'Updated requirements',
                    items: [
                        {
                            nhanhProductId: undefined,
                            name: 'Custom model',
                            quantity: 1,
                            note: 'Original note',
                            metadataJson: undefined
                        }
                    ],
                    uploadedImageUrls: ['/requests/reference.jpg']
                },
                'user'
            );
        });

        const payload = updateRequestAction.mock.calls[0][1];
        expect(payload).not.toHaveProperty('customerPhone');
        expect(payload).not.toHaveProperty('type');
        expect(payload).not.toHaveProperty('totalAmount');
        expect(payload).not.toHaveProperty('depositAmount');
        expect(payload.items?.[0]).not.toHaveProperty('price');
    });
});
