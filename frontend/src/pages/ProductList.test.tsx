import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react';
import { ProductListItemDTO } from '../interface/product.model';
import ProductList from './ProductList';

var mockSearchString = '';
const mockSetSearchParams = jest.fn();
const mockNavigate = jest.fn();

const mockProducts: ProductListItemDTO[] = [
    { id: 10, nhanhProductId: 90010, code: 'SD-SERUM', name: 'Serum phục hồi', price: 350000, categoryName: 'Serum', brandName: 'Sodu', stockAvailable: 10, status: 'ACTIVE' },
    { id: 11, nhanhProductId: 90011, code: 'ML-LIP', name: 'Son lì', price: 249000, categoryName: 'Son môi', brandName: 'Melia', stockAvailable: 0, status: 'ACTIVE' },
    { id: 12, nhanhProductId: 90012, code: 'SD-CREAM', name: 'Kem dưỡng', price: 450000, categoryName: 'Kem dưỡng', brandName: 'Sodu', stockAvailable: 4, status: 'ACTIVE' },
];
const mockFetchProducts = jest.fn();
const mockFetchCategories = jest.fn();
const mockFetchBrands = jest.fn();
const mockCategories = [
    { id: 1, name: 'Chăm sóc da', parentId: null, children: [{ id: 2, name: 'Serum', parentId: 1 }, { id: 3, name: 'Kem dưỡng', parentId: 1 }] },
    { id: 2, name: 'Serum', parentId: 1 },
    { id: 3, name: 'Kem dưỡng', parentId: 1 },
    { id: 4, name: 'Trang điểm', parentId: null, children: [{ id: 5, name: 'Son môi', parentId: 4 }] },
    { id: 5, name: 'Son môi', parentId: 4 },
];
const mockBrands = [{ id: 1, name: 'Sodu' }, { id: 2, name: 'Melia' }];

jest.mock('react-router-dom', () => ({
    Link: ({ children }: any) => <span>{children}</span>,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [new URLSearchParams(mockSearchString), mockSetSearchParams],
}), { virtual: true });

jest.mock('../store/useProductStore', () => ({
    useProductStore: () => ({
        products: mockProducts,
        categories: mockCategories,
        brands: mockBrands,
        fetchProducts: mockFetchProducts,
        fetchCategories: mockFetchCategories,
        fetchBrands: mockFetchBrands,
    }),
}));

jest.mock('../components/common/ProductCard', () => ({
    __esModule: true,
    default: ({ product }: any) => <div data-testid="product-card">{product.name}</div>,
}));

describe('ProductList storefront filters', () => {
    beforeEach(() => {
        mockSearchString = '';
        mockSetSearchParams.mockClear();
        mockNavigate.mockClear();
    });

    it('applies the URL keyword without Vietnamese accents and submits a new keyword', () => {
        mockSearchString = 'search=Sodu%20serum';
        render(<ProductList/>);

        expect(screen.getByText('Serum phục hồi')).toBeTruthy();
        expect(screen.queryByText('Son lì')).toBeNull();
        expect((screen.getByLabelText('Tìm kiếm sản phẩm cửa hàng') as HTMLInputElement).value).toBe('Sodu serum');

        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm cửa hàng'), { target: { value: '  kem dưỡng  ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Tìm kiếm' }));

        const submittedParams = mockSetSearchParams.mock.calls[0][0] as URLSearchParams;
        expect(submittedParams.get('search')).toBe('kem dưỡng');
    });

    it('includes child products when selecting a parent category', () => {
        render(<ProductList/>);

        fireEvent.click(screen.getByText('Chăm sóc da'));

        expect(screen.getByText('Serum phục hồi')).toBeTruthy();
        expect(screen.getByText('Kem dưỡng')).toBeTruthy();
        expect(screen.queryByText('Son lì')).toBeNull();
    });

    it('filters stock and sorts the current page by price', () => {
        render(<ProductList/>);

        fireEvent.click(screen.getByRole('button', { name: /chỉ hiện hàng có sẵn/i }));
        fireEvent.change(screen.getByLabelText('Sắp xếp sản phẩm cửa hàng'), { target: { value: 'PRICE_DESC' } });

        expect(screen.queryByText('Son lì')).toBeNull();
        expect(screen.getAllByTestId('product-card').map((card) => card.textContent)).toEqual([
            'Kem dưỡng',
            'Serum phục hồi',
        ]);
    });
});
