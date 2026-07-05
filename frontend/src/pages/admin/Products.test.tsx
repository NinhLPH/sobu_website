import { describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react';
import { ProductListItemDTO } from '../../interface/product.model';
import AdminProducts from './Products';

const mockProducts: ProductListItemDTO[] = [
    { id: 10, nhanhProductId: 90010, code: 'SD-SERUM', name: 'Serum phục hồi', price: 350000, categoryName: 'Chăm sóc da', brandName: 'Sodu', stockAvailable: 10, status: 'ACTIVE' },
    { id: 11, nhanhProductId: 90011, code: 'ML-LIP', name: 'Son lì', price: 249000, categoryName: 'Trang điểm', brandName: 'Melia', stockAvailable: 0, status: 'INACTIVE' },
    { id: 12, nhanhProductId: 90012, code: 'SD-CREAM', name: 'Kem dưỡng', price: 450000, categoryName: 'Chăm sóc da', brandName: 'Sodu', stockAvailable: 4, status: 'ACTIVE' },
];
const mockFetchProducts = jest.fn();
const mockFetchCategories = jest.fn();
const mockCategories: [] = [];

jest.mock('../../store/useProductStore', () => ({
    useProductStore: () => ({
        products: mockProducts,
        categories: mockCategories,
        fetchProducts: mockFetchProducts,
        fetchCategories: mockFetchCategories,
    }),
}));

describe('AdminProducts search suggest', () => {
    it('filters products by the current keyword', () => {
        render(<AdminProducts/>);

        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm quản trị'), {
            target: { value: 'serum' },
        });

        expect(screen.getAllByText('Serum phục hồi').length).toBeGreaterThan(0);
        expect(screen.queryByText('Son lì')).toBeNull();
        expect(screen.queryByText('Kem dưỡng')).toBeNull();
    });

    it('selects a product suggestion and filters immediately', () => {
        render(<AdminProducts/>);

        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm quản trị'), {
            target: { value: 'sodu' },
        });
        fireEvent.mouseDown(screen.getByRole('option', { name: /Kem dưỡng/i }));

        expect((screen.getByLabelText('Tìm kiếm sản phẩm quản trị') as HTMLInputElement).value).toBe('Kem dưỡng');
        expect(screen.getByText('Kem dưỡng')).toBeTruthy();
        expect(screen.queryByText('Serum phục hồi')).toBeNull();
        expect(screen.queryByText('Son lì')).toBeNull();
    });
});
