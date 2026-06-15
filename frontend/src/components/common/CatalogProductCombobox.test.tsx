import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, jest } from '@jest/globals';
import CatalogProductCombobox from './CatalogProductCombobox';
import { ProductListItemDTO } from '../../interface/product.model';

const products: ProductListItemDTO[] = Array.from({ length: 7 }, (_, index) => ({
    id: index + 1,
    name: `Product ${index + 1}`,
    code: `CODE-${index + 1}`,
    price: (index + 1) * 100000
}));

describe('CatalogProductCombobox', () => {
    it('shows at most five products and filters by product code', () => {
        const onChange = jest.fn();
        render(
            <CatalogProductCombobox
                products={products}
                value={{ name: '' }}
                onChange={onChange}
                ariaLabel="Catalog product"
            />
        );

        const input = screen.getByRole('combobox', { name: 'Catalog product' });
        fireEvent.focus(input);
        expect(screen.getAllByRole('option')).toHaveLength(5);

        fireEvent.change(input, { target: { value: 'CODE-7' } });
        expect(screen.getAllByRole('option')).toHaveLength(1);
        expect(screen.getByRole('option', { name: /Product 7/i })).toBeTruthy();
    });

    it('creates a free-form product when no catalog item matches', () => {
        const onChange = jest.fn();
        render(
            <CatalogProductCombobox
                products={products}
                value={{ name: '' }}
                onChange={onChange}
                ariaLabel="Catalog product"
            />
        );

        const input = screen.getByRole('combobox', { name: 'Catalog product' });
        fireEvent.change(input, { target: { value: 'New custom product' } });
        fireEvent.click(screen.getByRole('option', {
            name: /Tạo sản phẩm mới: “New custom product”/i
        }));

        expect(onChange).toHaveBeenLastCalledWith({
            nhanhProductId: undefined,
            name: 'New custom product',
            price: undefined
        });
    });
});
