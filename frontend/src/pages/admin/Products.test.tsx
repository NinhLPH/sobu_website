import {describe, expect, it, jest, beforeEach} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminProducts from './Products';
import apiClient from '../../api/api-client';
import {AdminCatalogService} from '../../service/admin-catalog.service';
import {ConfirmDialogProvider} from '../../components/common/ConfirmDialog';
import {FileService} from '../../service/file.service';

jest.mock('../../service/admin-catalog.service');
jest.mock('../../api/api-client');
jest.mock('../../service/toast.service');
jest.mock('../../service/file.service');

const mockedFileService = jest.mocked(FileService);

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
        (AdminCatalogService.createProduct as any).mockResolvedValue({});
        (apiClient.get as any).mockResolvedValue({value: '5'});
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

    it('passes explicit false for out-of-stock and stock sort options without changing the default sort', async () => {
        renderPage();
        await screen.findAllByText('Serum phục hồi');
        expect(AdminCatalogService.getProducts).toHaveBeenLastCalledWith(expect.objectContaining({
            inStock: undefined, sortBy: undefined, sortDirection: undefined
        }));

        fireEvent.change(screen.getByLabelText('Lọc theo tồn kho'), {target: {value: 'OUT_OF_STOCK'}});
        fireEvent.change(screen.getByLabelText('Sắp xếp theo tồn kho'), {target: {value: 'DESC'}});
        await waitFor(() => expect(AdminCatalogService.getProducts).toHaveBeenLastCalledWith(expect.objectContaining({
            inStock: false, sortBy: 'stockAvailable', sortDirection: 'DESC'
        })));
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
        await waitFor(() => expect(screen.getAllByText('Hết hàng').filter(node => node.tagName !== 'OPTION')).toHaveLength(2));
        expect(screen.getAllByText('Sắp hết · 3')).toHaveLength(2);
        expect(screen.getAllByText('10').length).toBeGreaterThan(0);
        expect(screen.getAllByRole('article')).toHaveLength(3);
        expect(apiClient.get).toHaveBeenCalledWith(
            '/api/public/configs/key/business_low_stock_threshold',
            {signal: expect.any(AbortSignal)}
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

    it('uploads a single avatar to products and submits its URL in the existing payload', async () => {
        const file = new File(['avatar'], 'avatar.png', {type: 'image/png'});
        const uploadedUrl = '/api/public/files/products/avatar.png';
        mockedFileService.uploadFile.mockResolvedValue({url: uploadedUrl});

        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.click(screen.getByRole('button', {name: 'Thêm sản phẩm'}));
        fireEvent.change(screen.getByLabelText('Mã sản phẩm'), {target: {value: 'NEW-01'}});
        fireEvent.change(screen.getByLabelText('Tên sản phẩm'), {target: {value: 'Sản phẩm mới'}});
        fireEvent.change(screen.getByLabelText('Chọn ảnh đại diện tải lên'), {target: {files: [file]}});

        await waitFor(() => expect(mockedFileService.uploadFile).toHaveBeenCalledWith(file, 'products'));
        await waitFor(() => expect((screen.getByRole('button', {name: 'Lưu sản phẩm'}) as HTMLButtonElement).disabled).toBe(false));
        fireEvent.click(screen.getByRole('button', {name: 'Lưu sản phẩm'}));

        await waitFor(() => expect(AdminCatalogService.createProduct).toHaveBeenCalledWith(
            expect.objectContaining({avatarImage: uploadedUrl})
        ));
    });

    it('appends uploaded album images to manual URLs, removes duplicates and submits them in order', async () => {
        const first = new File(['first'], 'first.png', {type: 'image/png'});
        const duplicate = new File(['duplicate'], 'duplicate.png', {type: 'image/png'});
        const manualUrl = '/api/public/files/products/manual.png';
        const uploadedUrl = '/api/public/files/products/uploaded.png';
        mockedFileService.uploadFile
            .mockResolvedValueOnce({url: uploadedUrl})
            .mockResolvedValueOnce({url: manualUrl});

        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.click(screen.getByRole('button', {name: 'Thêm sản phẩm'}));
        fireEvent.change(screen.getByLabelText('Mã sản phẩm'), {target: {value: 'ALBUM-01'}});
        fireEvent.change(screen.getByLabelText('Tên sản phẩm'), {target: {value: 'Sản phẩm album'}});
        fireEvent.click(screen.getByText('Thông tin nâng cao'));
        const advancedSection = screen.getByText('Thông tin nâng cao').closest('details');
        const albumTextarea = advancedSection?.querySelector('textarea');
        expect(albumTextarea).not.toBeNull();
        fireEvent.change(albumTextarea as HTMLTextAreaElement, {target: {value: `${manualUrl}\n${manualUrl}`}});
        fireEvent.change(screen.getByLabelText('Chọn ảnh album tải lên'), {target: {files: [first, duplicate]}});

        await waitFor(() => expect(mockedFileService.uploadFile).toHaveBeenCalledTimes(2));
        await waitFor(() => expect((screen.getByRole('button', {name: 'Lưu sản phẩm'}) as HTMLButtonElement).disabled).toBe(false));
        fireEvent.click(screen.getByRole('button', {name: 'Lưu sản phẩm'}));

        await waitFor(() => expect(AdminCatalogService.createProduct).toHaveBeenCalledWith(
            expect.objectContaining({images: [manualUrl, uploadedUrl]})
        ));
    });

    it('blocks saving and closing the form until an image upload finishes', async () => {
        let resolveUpload: (value: {url: string}) => void = () => undefined;
        mockedFileService.uploadFile.mockImplementation(() => new Promise(resolve => {
            resolveUpload = resolve;
        }));
        const file = new File(['avatar'], 'pending.png', {type: 'image/png'});

        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.click(screen.getByRole('button', {name: 'Thêm sản phẩm'}));
        fireEvent.change(screen.getByLabelText('Chọn ảnh đại diện tải lên'), {target: {files: [file]}});

        const uploadButton = await screen.findByRole('button', {name: 'Đang tải ảnh...'});
        expect((uploadButton as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', {name: 'Hủy'}) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', {name: 'Đóng'}) as HTMLButtonElement).disabled).toBe(true);
        fireEvent.submit(screen.getByRole('dialog').querySelector('form') as HTMLFormElement);
        expect(AdminCatalogService.createProduct).not.toHaveBeenCalled();

        resolveUpload({url: '/api/public/files/products/pending.png'});
        await waitFor(() => expect((screen.getByRole('button', {name: 'Lưu sản phẩm'}) as HTMLButtonElement).disabled).toBe(false));
        expect((screen.getByRole('button', {name: 'Hủy'}) as HTMLButtonElement).disabled).toBe(false);
        expect((screen.getByRole('button', {name: 'Đóng'}) as HTMLButtonElement).disabled).toBe(false);
    });

    it('shows and preserves existing avatar and album paths when updating a product', async () => {
        const avatar = '/api/public/files/products/current-avatar.png';
        const album = [
            '/api/public/files/products/current-1.png',
            'https://cdn.example.com/current-2.png'
        ];
        (AdminCatalogService.getProduct as any).mockResolvedValue({
            ...products[0],
            retailPrice: 350000,
            avatarImage: avatar,
            images: album
        });
        (AdminCatalogService.updateProduct as any).mockResolvedValue({});

        renderPage();
        await screen.findAllByText('Serum phục hồi');
        fireEvent.click(screen.getAllByTitle('Chỉnh sửa')[0]);

        await screen.findByDisplayValue(avatar);
        expect(screen.getAllByAltText(/Ảnh đính kèm/)).toHaveLength(3);
        fireEvent.click(screen.getByRole('button', {name: 'Lưu sản phẩm'}));

        await waitFor(() => expect(AdminCatalogService.updateProduct).toHaveBeenCalledWith(
            10,
            expect.objectContaining({avatarImage: avatar, images: album})
        ));
    });
});
