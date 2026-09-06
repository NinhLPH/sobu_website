import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, jest} from '@jest/globals';
import VoucherPickerModal from './VoucherPickerModal';
import {ActiveVoucher} from '../../interface/voucher.model';

const vouchers: ActiveVoucher[] = [
    {
        id: 1,
        code: 'SAVE10',
        name: 'Giảm toàn đơn',
        type: 'DISCOUNT_PERCENT',
        slot: 'ORDER',
        scope: 'ALL',
        value: 10,
        minOrderValue: 50000,
        maxDiscountAmount: 20000,
        endDate: '2026-12-31T23:59:59'
    },
    {
        id: 2,
        code: 'ITEM20K',
        name: 'Giảm cho sản phẩm',
        type: 'DISCOUNT_AMOUNT',
        slot: 'ITEM',
        scope: 'PRODUCT',
        value: 20000
    },
    {
        id: 3,
        code: 'FREESHIP',
        name: 'Miễn phí giao hàng',
        type: 'FREE_SHIP',
        slot: 'SHIPPING',
        scope: 'ALL',
        value: 0
    }
];

const renderModal = (overrides: Partial<React.ComponentProps<typeof VoucherPickerModal>> = {}) => {
    const props: React.ComponentProps<typeof VoucherPickerModal> = {
        open: true,
        vouchers,
        subtotal: 100000,
        discountVoucherCode: '',
        shippingVoucherCode: '',
        autoAppliedVoucherCodes: [],
        isLoading: false,
        error: null,
        onSelectionChange: jest.fn(),
        onClose: jest.fn(),
        ...overrides
    };
    return {props, ...render(<VoucherPickerModal {...props}/>)};
};

describe('VoucherPickerModal', () => {
    it('groups discount and shipping vouchers and renders the card information', () => {
        renderModal();

        expect(screen.getByRole('heading', {name: 'Voucher giảm giá'})).toBeTruthy();
        expect(screen.getByRole('heading', {name: 'Voucher miễn phí vận chuyển'})).toBeTruthy();
        expect(screen.getByText('Giảm 10%')).toBeTruthy();
        expect(screen.getByText(/Đơn tối thiểu 50\.000/)).toBeTruthy();
        expect(screen.getByText(/Tối đa 20\.000/)).toBeTruthy();
        expect(screen.getByText(/Hạn sử dụng: 31\/12\/2026/)).toBeTruthy();
    });

    it('disables a voucher below the minimum and explains the missing amount', () => {
        renderModal({subtotal: 30000});

        const checkbox = screen.getByRole('checkbox', {name: 'Chọn voucher SAVE10'}) as HTMLInputElement;
        expect(checkbox.disabled).toBe(true);
        expect(screen.getByText(/Mua thêm 20\.000.*để sử dụng mã này/)).toBeTruthy();
    });

    it('reports immediate checkbox changes for both voucher groups', () => {
        const onSelectionChange = jest.fn();
        renderModal({onSelectionChange});

        fireEvent.click(screen.getByRole('checkbox', {name: 'Chọn voucher SAVE10'}));
        fireEvent.click(screen.getByRole('checkbox', {name: 'Chọn voucher FREESHIP'}));

        expect(onSelectionChange).toHaveBeenNthCalledWith(1, vouchers[0], true);
        expect(onSelectionChange).toHaveBeenNthCalledWith(2, vouchers[2], true);
    });

    it('shows an automatically applied voucher as checked and lets the customer deselect it', () => {
        const onSelectionChange = jest.fn();
        renderModal({autoAppliedVoucherCodes: ['save10'], onSelectionChange});

        const checkbox = screen.getByRole('checkbox', {
            name: 'Bỏ chọn voucher SAVE10'
        }) as HTMLInputElement;
        expect(checkbox.checked).toBe(true);
        expect(checkbox.disabled).toBe(false);
        fireEvent.click(checkbox);
        expect(onSelectionChange).toHaveBeenCalledWith(vouchers[0], false);
    });

    it('renders loading, empty, and error states without changing selection', () => {
        const {rerender, props} = renderModal({vouchers: [], isLoading: true});
        expect(screen.getByText('Đang tải voucher...')).toBeTruthy();

        rerender(<VoucherPickerModal {...props} isLoading={false}/>);
        expect(screen.getByText('Hiện chưa có voucher khả dụng.')).toBeTruthy();

        rerender(<VoucherPickerModal {...props} isLoading={false} error="Không thể tải danh sách voucher."/>);
        expect(screen.getByRole('alert').textContent).toContain('Không thể tải danh sách voucher.');
        expect(props.onSelectionChange).not.toHaveBeenCalled();
    });

    it('shows voucher conditions and returns to the voucher list', async () => {
        renderModal();

        fireEvent.click(screen.getAllByRole('button', {name: 'Điều kiện'})[0]);
        expect(screen.getByRole('heading', {name: 'Điều kiện voucher'})).toBeTruthy();
        expect(screen.getByText('Giảm toàn đơn')).toBeTruthy();
        expect(screen.getByText('Toàn bộ đơn hàng')).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: 'Quay lại danh sách'}));
        expect(screen.getByRole('heading', {name: 'Chọn voucher'})).toBeTruthy();
        await waitFor(() => expect(document.activeElement?.textContent).toContain('Điều kiện'));
    });

    it('closes with Escape and restores body scrolling', () => {
        const onClose = jest.fn();
        const {unmount} = renderModal({onClose});

        expect(document.body.style.overflow).toBe('hidden');
        fireEvent.keyDown(document, {key: 'Escape'});
        expect(onClose).toHaveBeenCalledTimes(1);

        unmount();
        expect(document.body.style.overflow).toBe('');
    });
});
