import {describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import {BrandListItemDTO} from '../../interface/brand.model';
import AdminBrands from './Brands';

const mockBrands: BrandListItemDTO[] = [
    {id: 1, name: 'Sodu', code: 'SODU', status: 1},
    {id: 2, name: 'Melia', code: 'MELIA', status: 1},
    {id: 3, name: 'Bandai', code: 'BANDAI', status: 0},
];
const mockFetchBrands = jest.fn();

jest.mock('../../store/useProductStore', () => ({
    useProductStore: () => ({
        brands: mockBrands,
        fetchBrands: mockFetchBrands,
    }),
}));

describe('AdminBrands search suggest', () => {
    it('selects a brand suggestion and filters immediately', () => {
        render(<AdminBrands/>);

        fireEvent.change(screen.getByLabelText('Tìm kiếm thương hiệu quản trị'), {
            target: {value: 'ban'},
        });
        fireEvent.mouseDown(screen.getByRole('option', {name: /Bandai/i}));

        expect((screen.getByLabelText('Tìm kiếm thương hiệu quản trị') as HTMLInputElement).value).toBe('Bandai');
        expect(screen.getByText('Bandai')).toBeTruthy();
        expect(screen.queryByText('Sodu')).toBeNull();
        expect(screen.queryByText('Melia')).toBeNull();
    });
});
