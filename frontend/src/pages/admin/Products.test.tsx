import {describe, expect, it, jest, beforeEach} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminProducts from './Products';
import {AdminCatalogService} from '../../service/admin-catalog.service';

jest.mock('../../service/admin-catalog.service');
jest.mock('../../service/toast.service');

const products = [
    {id: 10, code: 'SD-SERUM', name: 'Serum phục hồi', price: 350000, categoryName: 'Chăm sóc da', brandName: 'Sodu', stockAvailable: 10, active: true},
    {id: 11, code: 'ML-LIP', name: 'Son lì', price: 249000, categoryName: 'Trang điểm', brandName: 'Melia', stockAvailable: 0, active: false},
];

describe('AdminProducts API catalog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (AdminCatalogService.getProducts as any).mockResolvedValue({content: products, totalPages: 1});
        (AdminCatalogService.getCategories as any).mockResolvedValue([]);
        (AdminCatalogService.getBrands as any).mockResolvedValue([]);
        (AdminCatalogService.getBadges as any).mockResolvedValue([]);
    });

    it('renders products returned by admin API', async () => {
        render(<AdminProducts/>);
        expect(await screen.findByText('Serum phục hồi')).toBeTruthy();
        expect(screen.getByText('Son lì')).toBeTruthy();
        expect(AdminCatalogService.getProducts).toHaveBeenCalled();
    });

    it('sends the current keyword to the backend filter', async () => {
        render(<AdminProducts/>);
        await screen.findByText('Serum phục hồi');
        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm quản trị'), {target: {value: 'serum'}});
        await waitFor(() => expect(AdminCatalogService.getProducts).toHaveBeenLastCalledWith(expect.objectContaining({search: 'serum'})), {timeout: 1000});
    });
});
