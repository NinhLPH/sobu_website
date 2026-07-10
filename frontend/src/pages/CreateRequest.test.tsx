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
const getRequirementsTextarea = () =>
    screen.getAllByRole('textbox').find(element => element.tagName.toLowerCase() === 'textarea') as HTMLElement;

beforeEach(() => {
    jest.clearAllMocks();
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

    it('lets CUSTOM requests create an item that is not in the catalog', async () => {
        render(<CreateRequest />);

        fireEvent.change(getRequestTypeSelect(), {
            target: { value: 'CUSTOM' }
        });
        fireEvent.change(screen.getByPlaceholderText('0912345678'), {
            target: { value: '0901234567' }
        });
        fireEvent.change(getProductInput(), {
            target: { value: 'Completely custom model' }
        });
        fireEvent.click(screen.getByRole('option', {
            name: /Tạo sản phẩm mới: “Completely custom model”/i
        }));
        fireEvent.change(getRequirementsTextarea(), {
            target: { value: 'Paint from attached reference' }
        });
        fireEvent.click(getSubmitButton());

        await waitFor(() => {
            expect(createRequestAction).toHaveBeenCalledWith(
                expect.objectContaining({
                    type: 'CUSTOM',
                    items: [
                        expect.objectContaining({
                            nhanhProductId: undefined,
                            name: 'Completely custom model'
                        })
                    ]
                })
            );
        });
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
