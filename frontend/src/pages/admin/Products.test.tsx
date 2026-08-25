import {describe, expect, it, jest, beforeEach} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminProducts from './Products';
import {AdminCatalogService} from '../../service/admin-catalog.service';
import {PublicUiService} from '../../service/public-ui.service';
import {ConfirmDialogProvider} from '../../components/common/ConfirmDialog';

jest.mock('../../service/admin-catalog.service');
jest.mock('../../service/public-ui.service');
jest.mock('../../service/toast.service');

const products = [
    {id: 10, code: 'SD-SERUM', name: 'Serum phục hồi', price: 350000, categoryName: 'Chăm sóc da', brandName: 'Sodu', stockAvailable: 10, active: true},
    {id: 11, code: 'ML-LIP', name: 'Son lì', price: 249000, categoryName: 'Trang điểm', brandName: 'Melia', stockAvailable: 0, active: false},
    {id: 12, code: 'SD-MASK', name: 'Mặt nạ phục hồi', price: 99000, categoryName: 'Chăm sóc da', brandName: 'Sodu', stockAvailable: 3, active: true},
];

const renderPage = () => render(<ConfirmDialogProvider><AdminProducts/></ConfirmDialogProvider>);

describe('AdminProducts API catalog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (AdminCatalogService.getProducts as any).mockResolvedValue({content: products, totalPages: 1});
        (AdminCatalogService.getCategories as any).mockResolvedValue([]);
        (AdminCatalogService.getBrands as any).mockResolvedValue([]);
        (AdminCatalogService.getBadges as any).mockResolvedValue([]);
        (AdminCatalogService.setProductActive as any).mockResolvedValue({...products[0], active: false});
        (PublicUiService.getConfigByKey as any).mockResolvedValue({
            id: 1,
            key: 'business_low_stock_threshold',
            value: '5',
            type: 'number',
            isPublic: true
        });
    });

    it('renders products returned by admin API', async () => {
        renderPage();
        expect((await screen.findAllByText('Serum phục hồi')).length).toBeGreaterThan(0);
        expect(screen.getAllByText('Son lì').length).toBeGreaterThan(0);
        expect(AdminCatalogService.getProducts).toHaveBeenCalled();
    });

    it('sends the current keyword to the backend filter', async () => {
        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.change(screen.getByLabelText('Tìm kiếm sản phẩm quản trị'), {target: {value: 'serum'}});
        await waitFor(() => expect(AdminCatalogService.getProducts).toHaveBeenLastCalledWith(
            expect.objectContaining({search: 'serum'})
        ), {timeout: 1000});
    });

    it('sends the configured sale window and one manual tag when updating', async () => {
        (AdminCatalogService.getBadges as any).mockResolvedValue([
            {id: 3, name: 'HOT', color: '#dc2626', textColor: '#ffffff', status: 1}
        ]);
        (AdminCatalogService.getProduct as any).mockResolvedValue({
            ...products[0],
            retailPrice: 350000,
            oldPrice: 500000,
            saleValidFrom: '2026-08-25T08:00:00',
            saleValidThrough: '2026-08-31T23:59:00',
            badgeId: 3,
            images: []
        });
        (AdminCatalogService.updateProduct as any).mockResolvedValue({});

        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.click(screen.getAllByTitle('Chỉnh sửa')[0]);
        await screen.findByDisplayValue('2026-08-25T08:00');
        fireEvent.click(screen.getByRole('button', {name: 'Lưu sản phẩm'}));

        await waitFor(() => expect(AdminCatalogService.updateProduct).toHaveBeenCalledWith(
            10,
            expect.objectContaining({
                oldPrice: 500000,
                saleValidFrom: '2026-08-25T08:00',
                saleValidThrough: '2026-08-31T23:59',
                badgeId: 3
            })
        ));
    });

    it('uses the configured threshold for accessible desktop and mobile stock indicators', async () => {
        renderPage();
        expect((await screen.findAllByText('Hết hàng')).length).toBe(2);
        expect(screen.getAllByText('Sắp hết · 3')).toHaveLength(2);
        expect(screen.getAllByText('10').length).toBeGreaterThan(0);
        expect(screen.getAllByRole('article')).toHaveLength(3);
        expect(PublicUiService.getConfigByKey).toHaveBeenCalledWith(
            'business_low_stock_threshold',
            expect.any(AbortSignal)
        );
    });

    it('sends the active payload once, disables duplicate clicks and updates from the response', async () => {
        let resolveUpdate: (value: unknown) => void = () => undefined;
        (AdminCatalogService.setProductActive as any).mockImplementation(() => new Promise(resolve => {
            resolveUpdate = resolve;
        }));
        renderPage();
        await screen.findAllByText('Serum phục hồi');
        const toggleButton = screen.getAllByRole('button', {name: 'Tạm dừng Serum phục hồi'})[0];

        fireEvent.click(toggleButton);
        fireEvent.click(toggleButton);

        expect(AdminCatalogService.setProductActive).toHaveBeenCalledTimes(1);
        expect(AdminCatalogService.setProductActive).toHaveBeenCalledWith(10, false, 'Cập nhật từ trang quản trị');
        expect((toggleButton as HTMLButtonElement).disabled).toBe(true);
        resolveUpdate({...products[0], active: false});

        await waitFor(() => expect((screen.getAllByRole('button', {name: 'Kích hoạt Serum phục hồi'})[0] as HTMLButtonElement).disabled).toBe(false));
        expect(AdminCatalogService.getProducts).toHaveBeenCalledTimes(1);
    });

    it('keeps the previous active state when the endpoint fails', async () => {
        (AdminCatalogService.setProductActive as any).mockRejectedValue({response: {status: 500}});
        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.click(screen.getAllByRole('button', {name: 'Tạm dừng Serum phục hồi'})[0]);

        await waitFor(() => expect((screen.getAllByRole('button', {name: 'Tạm dừng Serum phục hồi'})[0] as HTMLButtonElement).disabled).toBe(false));
        expect(screen.queryByRole('button', {name: 'Kích hoạt Serum phục hồi'})).toBeNull();
    });
});
