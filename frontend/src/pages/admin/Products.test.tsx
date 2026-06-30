import { describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen, within } from '@testing-library/react';
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

describe('AdminProducts filters', () => {
    it('combines keyword, category, brand, stock and status filters', () => {
        render(<AdminProducts/>);

        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm quản trị'), { target: { value: 'sodu dưỡng' } });
        fireEvent.change(screen.getByLabelText('Lọc theo danh mục'), { target: { value: 'Chăm sóc da' } });
        fireEvent.change(screen.getByLabelText('Lọc theo thương hiệu'), { target: { value: 'Sodu' } });
        fireEvent.change(screen.getByLabelText('Lọc theo tồn kho'), { target: { value: 'IN_STOCK' } });
        fireEvent.change(screen.getByLabelText('Lọc theo trạng thái'), { target: { value: 'ACTIVE' } });

        expect(screen.getByText('Kem dưỡng')).toBeTruthy();
        expect(screen.queryByText('Serum phục hồi')).toBeNull();
        expect(screen.queryByText('Son lì')).toBeNull();
        expect(screen.getByText(/1\/3 sản phẩm trong trang hiện tại/i)).toBeTruthy();
    });

    it('sorts products and clears all active filters', () => {
        const { container } = render(<AdminProducts/>);

        fireEvent.change(screen.getByLabelText('Sắp xếp sản phẩm quản trị'), { target: { value: 'PRICE_DESC' } });
        const rows = within(container.querySelector('tbody') as HTMLElement).getAllByRole('row');
        expect(rows[0].textContent).toContain('Kem dưỡng');

        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm quản trị'), { target: { value: 'melia' } });
        fireEvent.click(screen.getByRole('button', { name: /xóa lọc/i }));

        expect((screen.getByLabelText('Tìm kiếm sản phẩm quản trị') as HTMLInputElement).value).toBe('');
        expect(screen.getByText(/3\/3 sản phẩm trong trang hiện tại/i)).toBeTruthy();
    });
});
