import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import CreateRequest from './CreateRequest';
import { useRequestStore } from '../store/useRequestStore';
import { useProductStore } from '../store/useProductStore';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    )
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
const createRequestAction = jest.fn(async () => undefined);
const clearError = jest.fn();
const fetchAllProducts = jest.fn(async () => undefined);

beforeEach(() => {
    jest.clearAllMocks();
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

        const selects = screen.getAllByRole('combobox');
        expect(selects[0].querySelector('option[value="NORMAL"]')).toBeNull();
        expect(selects[0].querySelectorAll('option')).toHaveLength(3);
        expect(fetchAllProducts).toHaveBeenCalledTimes(1);

        fireEvent.change(screen.getByPlaceholderText('0912345678'), {
            target: { value: '0901234567' }
        });
        fireEvent.focus(screen.getByLabelText('Sản phẩm yêu cầu 1'));
        fireEvent.click(screen.getByRole('option', { name: /Gundam Catalog/i }));
        fireEvent.click(screen.getByRole('button', {
            name: /Gửi Yêu Cầu Cho SOBU Workshop/i
        }));

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

        fireEvent.change(screen.getAllByRole('combobox')[0], {
            target: { value: 'CUSTOM' }
        });
        fireEvent.change(screen.getByPlaceholderText('0912345678'), {
            target: { value: '0901234567' }
        });
        fireEvent.change(screen.getByLabelText('Sản phẩm yêu cầu 1'), {
            target: { value: 'Mô hình hoàn toàn mới' }
        });
        fireEvent.click(screen.getByRole('option', {
            name: /Tạo sản phẩm mới: “Mô hình hoàn toàn mới”/i
        }));
        fireEvent.change(screen.getByRole('textbox', {
            name: /Yêu cầu chi tiết/i
        }), {
            target: { value: 'Sơn theo ảnh đính kèm' }
        });
        fireEvent.click(screen.getByRole('button', {
            name: /Gửi Yêu Cầu Cho SOBU Workshop/i
        }));

        await waitFor(() => {
            expect(createRequestAction).toHaveBeenCalledWith(
                expect.objectContaining({
                    type: 'CUSTOM',
                    items: [
                        expect.objectContaining({
                            nhanhProductId: undefined,
                            name: 'Mô hình hoàn toàn mới'
                        })
                    ]
                })
            );
        });
    });
});
