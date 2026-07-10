import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import CreateRequest from './CreateRequest';
import { useRequestStore } from '../store/useRequestStore';
import { useProductStore } from '../store/useProductStore';
import { useAuthStore } from '../store/useAuthStore';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    )
}), { virtual: true });
jest.mock('../store/useRequestStore');
jest.mock('../store/useProductStore');
jest.mock('../store/useAuthStore');
let mockConfigMap: Record<string, string> = {};
jest.mock('../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({
        configMap: mockConfigMap
    })
}));
jest.mock('../service/toast.service');
jest.mock('../components/common/ImageUploader', () => ({
    __esModule: true,
    default: () => <div data-testid="image-uploader" />
}));

const mockedUseRequestStore = jest.mocked(useRequestStore);
const mockedUseProductStore = jest.mocked(useProductStore);
const mockedUseAuthStore = jest.mocked(useAuthStore);
const createRequestAction = jest.fn(async () => undefined);
const clearError = jest.fn();
const fetchAllProducts = jest.fn(async () => undefined);

const mockAuthPhone = (phone?: string) => {
    mockedUseAuthStore.mockImplementation(((selector: any) => {
        const state = {
            user: phone ? { phone } : null
        };
        return typeof selector === 'function' ? selector(state) : state;
    }) as typeof useAuthStore);
};

const getRequestTypeSelect = () => screen.getAllByRole('combobox')[0];
const getProductInput = () => screen.getAllByRole('combobox')[1];
const getSubmitButton = () => screen.getByRole('button', { name: /SOBU Workshop/i });

beforeEach(() => {
    jest.clearAllMocks();
    mockConfigMap = {
        social_links: JSON.stringify({
            facebook: 'https://facebook.com/sobu'
        })
    };
    mockAuthPhone();
    mockedUseRequestStore.mockReturnValue({
        createRequestAction,
        isSubmitting: false,
        error: null,
        clearError
    } as ReturnType<typeof useRequestStore>);
    mockedUseProductStore.mockReturnValue({
        allProducts: [
            {
                id: 7,
                nhanhProductId: 9001,
                name: 'Gundam Catalog',
                code: 'GD-01',
                price: 1500000
            }
        ],
        fetchAllProducts,
        isAllProductsLoading: false
    } as ReturnType<typeof useProductStore>);
});

describe('CreateRequest', () => {
    it('offers only workflow request types and sends the product code for catalog items', async () => {
        render(<CreateRequest />);

        const requestTypeSelect = getRequestTypeSelect();
        expect(requestTypeSelect.querySelector('option[value="NORMAL"]')).toBeNull();
        expect(requestTypeSelect.querySelectorAll('option')).toHaveLength(3);
        expect(fetchAllProducts).toHaveBeenCalledTimes(1);

        fireEvent.change(screen.getByPlaceholderText('0912345678'), {
            target: { value: '0901234567' }
        });
        fireEvent.focus(getProductInput());
        fireEvent.click(screen.getByRole('option', { name: /Gundam Catalog/i }));
        fireEvent.click(getSubmitButton());

        await waitFor(() => {
            expect(createRequestAction).toHaveBeenCalledWith({
                customerPhone: '0901234567',
                type: 'PREORDER',
                customRequirements: undefined,
                items: [
                    {
                        nhanhProductId: 'GD-01',
                        name: 'Gundam Catalog',
                        price: 1500000,
                        quantity: 1,
                        note: undefined
                    }
                ],
                uploadedImageUrls: []
            });
        });
    });

    it('turns CUSTOM requests into a Facebook-only contact panel', () => {
        render(<CreateRequest />);

        fireEvent.change(getRequestTypeSelect(), {
            target: { value: 'CUSTOM' }
        });

        expect(screen.queryByPlaceholderText('0912345678')).toBeNull();
        expect(screen.getAllByRole('combobox')).toHaveLength(1);
        expect(screen.queryByLabelText('Yêu cầu chi tiết')).toBeNull();
        expect(screen.queryByTestId('image-uploader')).toBeNull();
        expect(screen.queryByRole('button', { name: /SOBU Workshop/i })).toBeNull();
        expect(screen.getByText(/liên hệ Shop qua Facebook/i)).toBeTruthy();

        const facebookLink = screen.getByRole('link', { name: /Nhắn tin qua Facebook/i }) as HTMLAnchorElement;
        expect(facebookLink.getAttribute('href')).toBe('https://facebook.com/sobu');
        expect(facebookLink.getAttribute('target')).toBe('_blank');
        expect(facebookLink.getAttribute('rel')).toBe('noreferrer');

        fireEvent.submit(facebookLink.closest('form') as HTMLFormElement);

        expect(createRequestAction).not.toHaveBeenCalled();
    });

    it('shows a disabled fallback when CUSTOM has no configured Facebook link', () => {
        mockConfigMap = {
            social_links: JSON.stringify({
                facebook: '   '
            })
        };

        render(<CreateRequest />);

        fireEvent.change(getRequestTypeSelect(), {
            target: { value: 'CUSTOM' }
        });

        expect(screen.queryByRole('link', { name: /Nhắn tin qua Facebook/i })).toBeNull();
        expect(screen.getByText('Facebook chưa được cấu hình').getAttribute('aria-disabled')).toBe('true');
        fireEvent.submit(screen.getByText('Facebook chưa được cấu hình').closest('form') as HTMLFormElement);
        expect(createRequestAction).not.toHaveBeenCalled();
    });

    it('prefills the request phone when the authenticated user has one', () => {
        mockAuthPhone(' 0987654321 ');

        render(<CreateRequest />);

        expect((screen.getByPlaceholderText('0912345678') as HTMLInputElement).value).toBe('0987654321');
    });

    it('keeps the phone field empty and blocks submit when the user has no phone', async () => {
        render(<CreateRequest />);

        const phoneInput = screen.getByPlaceholderText('0912345678');
        expect((phoneInput as HTMLInputElement).value).toBe('');

        fireEvent.change(getProductInput(), {
            target: { value: 'Manual product' }
        });
        fireEvent.click(screen.getByRole('option', {
            name: /Tạo sản phẩm mới: “Manual product”/i
        }));

        fireEvent.submit(getSubmitButton().closest('form') as HTMLFormElement);

        await waitFor(() => {
            expect(createRequestAction).not.toHaveBeenCalled();
        });
    });
});
