import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import AdminVouchers from './Vouchers';
import {AdminCatalogService, AdminVoucherService} from '../../service/admin-catalog.service';

jest.mock('../../service/admin-catalog.service');
jest.mock('../../service/toast.service', () => ({
    ToastService: {
        success: require('@jest/globals').jest.fn(),
        error: require('@jest/globals').jest.fn()
    }
}));

const mockedCatalog = jest.mocked(AdminCatalogService);
const mockedVouchers = jest.mocked(AdminVoucherService);

const voucher = {
    id: 7,
    code: 'SAVE20',
    name: 'Giảm 20% sản phẩm',
    type: 'DISCOUNT_PERCENT' as const,
    slot: 'ITEM' as const,
    scope: 'PRODUCT' as const,
    geoScope: 'ALL' as const,
    value: 20,
    maxDiscountAmount: 50000,
    minOrderValue: 100000,
    usageLimit: 100,
    usedCount: 90,
    autoApply: false,
    applicableProductIds: [1001],
    applicableCategoryIds: [],
    active: true
};

describe('AdminVouchers', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedVouchers.getVouchers.mockResolvedValue({
            content: [voucher],
            number: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1
        });
        mockedVouchers.createVoucher.mockResolvedValue(voucher);
        mockedVouchers.updateVoucher.mockResolvedValue(voucher);
        mockedVouchers.toggleVoucher.mockResolvedValue({...voucher, active: false});
        mockedVouchers.deleteVoucher.mockResolvedValue(undefined);
        mockedCatalog.getProducts.mockResolvedValue({
            content: [{
                id: 1001,
                code: 'SD-CLEANSER-120',
                name: 'Sữa rửa mặt Sodu Gentle',
                price: 189000,
                stockAvailable: 10,
                active: true
            }],
            pageNumber: 0,
            pageSize: 100,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false
        });
        mockedCatalog.getCategories.mockResolvedValue([{
            id: 101,
            code: 'CLEANSER',
            name: 'Sữa rửa mặt',
            status: 1
        }]);
    });

    it('filters with the server contract and shows usage progress', async () => {
        render(<AdminVouchers/>);

        expect((await screen.findAllByText('SAVE20')).length).toBeGreaterThan(0);
        expect(screen.getAllByLabelText('Đã dùng 90 trên 100 lượt').length).toBeGreaterThan(0);

        fireEvent.change(screen.getByLabelText('Trạng thái voucher'), {target: {value: 'true'}});
        fireEvent.change(screen.getByLabelText('Phạm vi voucher'), {target: {value: 'PRODUCT'}});
        fireEvent.change(screen.getByLabelText('Slot voucher'), {target: {value: 'ITEM'}});
        fireEvent.change(screen.getByLabelText('Cách áp dụng voucher'), {target: {value: 'false'}});

        await waitFor(() => expect(mockedVouchers.getVouchers).toHaveBeenLastCalledWith(expect.objectContaining({
            active: true,
            scope: 'PRODUCT',
            slot: 'ITEM',
            autoApply: false,
            page: 0
        })));
    });

    it('creates a product-scoped voucher with keyboard-friendly product selection and restores focus', async () => {
        render(<AdminVouchers/>);
        await screen.findAllByText('SAVE20');

        const openButton = screen.getByRole('button', {name: /Tạo voucher/i});
        openButton.focus();
        fireEvent.click(openButton);
        const codeInput = await screen.findByLabelText('Mã voucher');
        await waitFor(() => expect(document.activeElement).toBe(codeInput));

        fireEvent.change(codeInput, {target: {value: 'new item'}});
        fireEvent.change(screen.getByLabelText('Tên chương trình'), {target: {value: 'Voucher sản phẩm mới'}});
        fireEvent.change(screen.getByLabelText('Giá trị'), {target: {value: '10'}});
        fireEvent.change(screen.getByLabelText('Phạm vi catalog'), {target: {value: 'PRODUCT'}});

        const productCheckbox = await screen.findByRole('checkbox', {name: /Sữa rửa mặt Sodu Gentle/i});
        fireEvent.click(productCheckbox);
        expect(screen.getByRole('button', {name: /Bỏ chọn Sữa rửa mặt Sodu Gentle/i})).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: 'Lưu voucher'}));
        await waitFor(() => expect(mockedVouchers.createVoucher).toHaveBeenCalledWith(expect.objectContaining({
            code: 'NEWITEM',
            name: 'Voucher sản phẩm mới',
            scope: 'PRODUCT',
            applicableProductIds: [1001],
            applicableCategoryIds: []
        })));
        await waitFor(() => expect(document.activeElement).toBe(openButton));
    });
});
