import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import ProductDetail from './ProductDetails';
import {PublicCatalogService} from '../service/public-catalog.service';
import {VoucherService} from '../service/voucher.service';
import {useCartStore} from '../store/useCartStore';
import {useProductStore} from '../store/useProductStore';
import {usePublicUiStore} from '../store/usePublicUiStore';

jest.mock('react-router-dom', () => ({
    Link: ({children, to}: any) => <a href={to}>{children}</a>,
    useParams: () => ({id: '1001'})
}), {virtual: true});
jest.mock('../service/public-catalog.service');
jest.mock('../service/voucher.service');
jest.mock('../store/useCartStore');
jest.mock('../store/useProductStore');
jest.mock('../store/usePublicUiStore');
jest.mock('../components/common/ProductSlider', () => () => <div data-testid="product-slider"/>);
jest.mock('../components/reviews/ProductReviewSection', () => () => <div data-testid="reviews"/>);

const mockedCatalog = jest.mocked(PublicCatalogService);
const mockedVoucherService = jest.mocked(VoucherService);
const mockedUseCartStore = jest.mocked(useCartStore);
const mockedUseProductStore = jest.mocked(useProductStore);
const mockedUsePublicUiStore = jest.mocked(usePublicUiStore);
const addToCart = jest.fn();
const writeText = jest.fn<Promise<void>, [string]>();

const detail = {
    id: 1001,
    name: 'Sữa rửa mặt Sodu Gentle',
    code: 'SD-CLEANSER-120',
    description: 'Làm sạch dịu nhẹ',
    content: '',
    price: 189000,
    oldPrice: 229000,
    avatarImage: '/cleanser.jpg',
    brandName: 'Sodu Beauty',
    categoryId: 101,
    categoryName: 'Sữa rửa mặt',
    stockAvailable: 10,
    stockRemain: 10,
    units: [],
    attributes: [],
    images: ['/cleanser.jpg'],
    updatedAt: '2026-08-25T10:00:00'
};

describe('ProductDetail vouchers', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        Object.defineProperty(navigator, 'clipboard', {configurable: true, value: {writeText}});
        writeText.mockResolvedValue(undefined);
        mockedCatalog.getProductDetail.mockResolvedValue(detail);
        mockedVoucherService.getForProduct.mockResolvedValue({
            success: true,
            message: 'Product vouchers retrieved',
            data: [{
                id: 4,
                code: 'PROMOVIP15',
                name: 'Giảm 15% sản phẩm Sodu Gentle',
                type: 'DISCOUNT_PERCENT',
                slot: 'ITEM',
                scope: 'PRODUCT',
                value: 15,
                maxDiscountAmount: 60000,
                minOrderValue: 100000,
                estimatedDiscount: 28350,
                effectivePrice: 160650,
                endDate: '2026-12-31T23:59:59'
            }]
        });
        mockedUseCartStore.mockImplementation((selector: any) => selector({addToCart}));
        mockedUseProductStore.mockReturnValue({
            products: [],
            productsLoaded: true,
            fetchProducts: jest.fn()
        } as unknown as ReturnType<typeof useProductStore>);
        mockedUsePublicUiStore.mockImplementation((selector: any) => selector({configMap: {}}));
    });

    it('loads product vouchers with current pricing and copies a voucher code', async () => {
        render(<ProductDetail/>);

        expect(await screen.findByText('PROMOVIP15')).toBeTruthy();
        expect(screen.getByText(/160\.650/)).toBeTruthy();
        expect(mockedVoucherService.getForProduct).toHaveBeenCalledWith('1001', {
            categoryId: 101,
            oldPrice: 229000,
            price: 189000
        }, expect.any(AbortSignal));

        fireEvent.click(screen.getByRole('button', {name: 'Sao chép mã PROMOVIP15'}));
        await waitFor(() => expect(writeText).toHaveBeenCalledWith('PROMOVIP15'));
        expect(await screen.findByText('Đã sao chép mã PROMOVIP15.')).toBeTruthy();
    });

    it('keeps purchasing available when voucher loading fails and supports retry', async () => {
        mockedVoucherService.getForProduct
            .mockRejectedValueOnce(new Error('Voucher API unavailable'))
            .mockResolvedValueOnce({success: true, message: 'ok', data: []});

        render(<ProductDetail/>);

        expect(await screen.findByText('Voucher API unavailable')).toBeTruthy();
        expect(screen.getByRole('button', {name: /Thêm vào giỏ/i})).toBeTruthy();
        fireEvent.click(screen.getByRole('button', {name: 'Thử lại'}));
        await waitFor(() => expect(mockedVoucherService.getForProduct).toHaveBeenCalledTimes(2));
        expect(await screen.findByText('Hiện chưa có voucher riêng cho sản phẩm này.')).toBeTruthy();
    });
});
