import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminInventory from './Inventory';
import {AdminCatalogService} from '../../service/admin-catalog.service';
const mockGetOverview = jest.fn<Promise<{threshold: number; products: any[]}>, [AbortSignal?]>();

jest.mock('react-router-dom', () => ({
    Link: ({children, to}: any) => <a href={to}>{children}</a>
}), {virtual: true});
jest.mock('../../service/admin-catalog.service');
jest.mock('../../service/toast.service');
jest.mock('../../service/inventory-dashboard.service', () => ({
    DEFAULT_LOW_STOCK_THRESHOLD: 5,
    inventoryQuantity: (product: {stockAvailable?: number | null; stockRemain?: number | null}) =>
        Number(product.stockAvailable ?? product.stockRemain ?? 0),
    InventoryDashboardService: {getOverview: (signal?: AbortSignal) => mockGetOverview(signal)}
}));

const mockedCatalog = jest.mocked(AdminCatalogService);
const product = {id: 10, code: 'SD-SERUM', name: 'Serum phục hồi', stockAvailable: 7, stockRemain: 10, active: true};
const ledger = [{
    id: 1,
    productId: 10,
    type: 'ORDER_RESERVATION' as const,
    quantityDelta: -3,
    balanceAfter: 7,
    orderId: 77,
    orderCode: 'SO-77',
    actor: 'system',
    createdAt: '2026-08-25T08:00:00'
}];

const renderInventory = () => render(<AdminInventory/>);

async function selectInventoryProduct() {
    renderInventory();
    const buttons = await screen.findAllByRole('button', {name: 'Xem sổ kho'});
    fireEvent.click(buttons[0]);
    await screen.findByText('Đang giữ cho đơn');
}

describe('AdminInventory', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetOverview.mockResolvedValue({threshold: 5, products: [
            {...product, stockAvailable: 0},
            {...product, id: 12, name: 'Sắp hết', stockAvailable: 3}
        ]});
        mockedCatalog.getProducts.mockResolvedValue({
            content: [product], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1,
            first: true, last: true, hasNext: false, hasPrevious: false
        });
        mockedCatalog.getInventoryBalance.mockResolvedValue({productId: 10, stockRemain: 10, stockAvailable: 7, reserved: 3});
        mockedCatalog.getInventoryLedger.mockResolvedValue(ledger);
        mockedCatalog.adjustInventory.mockResolvedValue(ledger[0]);
        mockedCatalog.setOpeningStock.mockResolvedValue({...ledger[0], type: 'OPENING_STOCK'});
    });

    it('renders the low-stock overview and ledger with authoritative balance labels', async () => {
        await selectInventoryProduct();
        expect(screen.getAllByText('Hết hàng').length).toBeGreaterThan(0);
        expect(screen.getAllByText('Sắp hết · 3').length).toBeGreaterThan(0);
        expect(screen.getAllByText('Khả dụng sau').length).toBeGreaterThan(0);
        expect(screen.getAllByText('system').length).toBeGreaterThan(0);
        expect(screen.getAllByRole('link', {name: /SO-77/})[0].getAttribute('href')).toBe('/admin/orders/77');
        expect((screen.getByRole('button', {name: /Tồn đầu kỳ/}) as HTMLButtonElement).disabled).toBe(true);
    });

    it('ignores a stale balance response when the operator selects another product quickly', async () => {
        let resolveFirst!: (value: {productId: number; stockRemain: number; stockAvailable: number; reserved: number}) => void;
        mockedCatalog.getInventoryBalance.mockImplementation(productId => productId === 10
            ? new Promise(resolve => { resolveFirst = resolve; })
            : Promise.resolve({productId: 12, stockRemain: 4, stockAvailable: 4, reserved: 0}));
        mockedCatalog.getInventoryLedger.mockResolvedValue([]);
        renderInventory();
        const buttons = await screen.findAllByRole('button', {name: 'Xem sổ kho'});
        fireEvent.click(buttons[0]);
        await waitFor(() => expect(mockedCatalog.getInventoryBalance).toHaveBeenCalledWith(10, expect.any(AbortSignal)));
        fireEvent.click(buttons[1]);
        await screen.findByText('Đã tải tồn kho của Sắp hết.');
        expect(screen.getAllByText('4').length).toBeGreaterThan(0);

        await act(async () => resolveFirst({productId: 10, stockRemain: 99, stockAvailable: 99, reserved: 0}));
        expect(screen.queryByText('99')).toBeNull();
        expect(screen.getByText('Đã tải tồn kho của Sắp hết.')).toBeTruthy();
    });

    it('keeps the correction modal open and explains a target below reserved stock', async () => {
        await selectInventoryProduct();
        fireEvent.click(screen.getByRole('button', {name: /Điều chỉnh kho/}));
        expect((await screen.findByLabelText('Loại điều chỉnh') as HTMLSelectElement).value).toBe('CORRECTION');
        fireEvent.change(await screen.findByRole('spinbutton'), {target: {value: '2'}});
        fireEvent.change(screen.getByPlaceholderText('Lý do và thông tin đối soát'), {target: {value: 'Kiểm kê'}});
        fireEvent.click(screen.getByRole('button', {name: 'Xác nhận'}));
        expect((await screen.findByRole('alert')).textContent).toContain('không được thấp hơn lượng đang giữ');
        expect(mockedCatalog.adjustInventory).not.toHaveBeenCalled();

        fireEvent.change(screen.getByRole('spinbutton'), {target: {value: '8'}});
        fireEvent.click(screen.getByRole('button', {name: 'Xác nhận'}));
        await waitFor(() => expect(mockedCatalog.adjustInventory).toHaveBeenCalledWith(10, 'CORRECTION', 8, 'Kiểm kê'));
    });

    it.each(['STOCK_IN', 'STOCK_OUT', 'DAMAGED', 'RETURNED'] as const)(
        'submits the %s manual adjustment through the existing backend contract',
        async adjustmentType => {
            await selectInventoryProduct();
            fireEvent.click(screen.getByRole('button', {name: /Điều chỉnh kho/}));
            fireEvent.change(await screen.findByLabelText('Loại điều chỉnh'), {target: {value: adjustmentType}});
            fireEvent.change(await screen.findByRole('spinbutton'), {target: {value: '2'}});
            fireEvent.change(screen.getByPlaceholderText('Lý do và thông tin đối soát'), {target: {value: 'Đối soát kho'}});
            fireEvent.click(screen.getByRole('button', {name: 'Xác nhận'}));
            await waitFor(() => expect(mockedCatalog.adjustInventory).toHaveBeenCalledWith(10, adjustmentType, 2, 'Đối soát kho'));
        }
    );

    it('allows opening stock only when the selected product has no ledger', async () => {
        mockedCatalog.getInventoryLedger.mockResolvedValue([]);
        mockedCatalog.getInventoryBalance.mockResolvedValue({productId: 10, stockRemain: 0, stockAvailable: 0, reserved: 0});
        await selectInventoryProduct();
        const openingButton = screen.getByRole('button', {name: /Tồn đầu kỳ/});
        expect((openingButton as HTMLButtonElement).disabled).toBe(false);
        fireEvent.click(openingButton);
        fireEvent.change(await screen.findByRole('spinbutton'), {target: {value: '12'}});
        fireEvent.change(screen.getByPlaceholderText('Lý do và thông tin đối soát'), {target: {value: 'Khởi tạo kho'}});
        fireEvent.click(screen.getByRole('button', {name: 'Xác nhận'}));
        await waitFor(() => expect(mockedCatalog.setOpeningStock).toHaveBeenCalledWith(10, 12, 'Khởi tạo kho'));
    });
});
