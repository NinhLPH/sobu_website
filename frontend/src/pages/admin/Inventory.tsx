import {FormEvent, useEffect, useMemo, useRef, useState} from 'react';
import {Link} from 'react-router-dom';
import {AlertTriangle, ArrowDownToLine, PackageCheck, RefreshCw, ShoppingCart, Warehouse} from 'lucide-react';
import {
    InventoryAdjustment,
    InventoryAdjustmentType,
    InventoryBalance,
    InventoryProduct
} from '../../interface/admin-catalog.model';
import {AdminCatalogService, InventoryProductParams} from '../../service/admin-catalog.service';
import {
    DEFAULT_LOW_STOCK_THRESHOLD,
    InventoryDashboardService,
    inventoryQuantity
} from '../../service/inventory-dashboard.service';
import {ToastService} from '../../service/toast.service';
import {
    AdminButton,
    AdminCard,
    AdminEmpty,
    AdminError,
    AdminFilterGroup,
    AdminLoading,
    AdminModal,
    AdminPage,
    AdminPagination,
    AdminSearch,
    AdminToolbar,
    Field,
    getApiError,
    inputClass
} from '../../components/admin/AdminUi';
import {StockIndicator} from '../../components/admin/StockIndicator';

const LOW_STOCK_PAGE_SIZE = 10;
const INVENTORY_PAGE_SIZE = 20;
type InventorySelection = Pick<InventoryProduct, 'id' | 'name' | 'code'>;
type InventorySortBy = NonNullable<InventoryProductParams['sortBy']>;
type InventoryStockStatus = NonNullable<InventoryProductParams['stockStatus']> | 'ALL';
const typeNames: Record<InventoryAdjustmentType, string> = {
    OPENING_STOCK: 'Tồn đầu kỳ', STOCK_IN: 'Nhập kho', STOCK_OUT: 'Xuất kho',
    CORRECTION: 'Điều chỉnh kiểm kê', DAMAGED: 'Hàng hỏng', RETURNED: 'Hoàn kho',
    ORDER_RESERVATION: 'Giữ cho đơn hàng', ORDER_RELEASE: 'Hoàn giữ đơn hàng'
};
type ManualType = Exclude<InventoryAdjustmentType, 'OPENING_STOCK' | 'ORDER_RESERVATION' | 'ORDER_RELEASE'>;
const isAbortError = (error: unknown) => (error instanceof DOMException && error.name === 'AbortError')
    || (error as { code?: string })?.code === 'ERR_CANCELED';
const isOrderLedger = (entry: InventoryAdjustment) => entry.type === 'ORDER_RESERVATION' || entry.type === 'ORDER_RELEASE';
const balanceLabel = (entry: InventoryAdjustment) => isOrderLedger(entry) ? 'Khả dụng sau' : 'Thực tế sau';
const formatDate = (value?: string) => value ? new Date(value).toLocaleString('vi-VN') : '—';

export default function AdminInventory() {
    const [products, setProducts] = useState<InventoryProduct[]>([]);
    const [productQuery, setProductQuery] = useState('');
    const [stockStatus, setStockStatus] = useState<InventoryStockStatus>('ALL');
    const [sortBy, setSortBy] = useState<InventorySortBy>('name');
    const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('ASC');
    const [productPage, setProductPage] = useState(0);
    const [productTotalPages, setProductTotalPages] = useState(0);
    const [productListRetry, setProductListRetry] = useState(0);
    const [productId, setProductId] = useState<number | null>(null);
    const [selectedProduct, setSelectedProduct] = useState<InventorySelection | null>(null);
    const [productsLoading, setProductsLoading] = useState(true);
    const [productsError, setProductsError] = useState('');
    const [balance, setBalance] = useState<InventoryBalance | null>(null);
    const [ledger, setLedger] = useState<InventoryAdjustment[]>([]);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');
    const [detailRetry, setDetailRetry] = useState(0);
    const [threshold, setThreshold] = useState(DEFAULT_LOW_STOCK_THRESHOLD);
    const [lowStockProducts, setLowStockProducts] = useState<Array<InventorySelection & {
        stockAvailable?: number | null;
        stockRemain?: number | null;
    }>>([]);
    const [overviewLoading, setOverviewLoading] = useState(true);
    const [overviewError, setOverviewError] = useState('');
    const [overviewRetry, setOverviewRetry] = useState(0);
    const [lowStockPage, setLowStockPage] = useState(0);
    const [mode, setMode] = useState<'opening' | 'adjust' | null>(null);
    const [quantity, setQuantity] = useState('');
    const [type, setType] = useState<ManualType>('STOCK_IN');
    const [note, setNote] = useState('');
    const [formError, setFormError] = useState('');
    const [saving, setSaving] = useState(false);
    const [announcement, setAnnouncement] = useState('');
    const detailRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const controller = new AbortController();
        const timer = window.setTimeout(async () => {
            setProductsLoading(true);
            setProductsError('');
            try {
                const response = await AdminCatalogService.getInventoryProducts({
                    page: productPage,
                    pageSize: INVENTORY_PAGE_SIZE,
                    search: productQuery.trim() || undefined,
                    stockStatus: stockStatus === 'ALL' ? undefined : stockStatus,
                    sortBy,
                    sortDirection
                }, controller.signal);
                if (!controller.signal.aborted) {
                    setProducts(response.content ?? []);
                    setProductTotalPages(response.totalPages ?? 0);
                }
            } catch (error) {
                if (!controller.signal.aborted && !isAbortError(error)) setProductsError(getApiError(error, 'Không thể tải danh sách sản phẩm.'));
            } finally {
                if (!controller.signal.aborted) setProductsLoading(false);
            }
        }, 250);
        return () => {
            controller.abort();
            window.clearTimeout(timer);
        };
    }, [productPage, productQuery, productListRetry, sortBy, sortDirection, stockStatus]);

    useEffect(() => {
        const controller = new AbortController();
        setOverviewLoading(true);
        setOverviewError('');
        InventoryDashboardService.getOverview(controller.signal).then(response => {
            if (controller.signal.aborted) return;
            setThreshold(response.threshold);
            setLowStockProducts(response.products);
            setLowStockPage(0);
        }).catch(error => {
            if (!controller.signal.aborted && !isAbortError(error)) setOverviewError(getApiError(error, 'Không thể tải cảnh báo tồn thấp.'));
        }).finally(() => {
            if (!controller.signal.aborted) setOverviewLoading(false);
        });
        return () => controller.abort();
    }, [overviewRetry]);

    useEffect(() => {
        if (!productId) {
            setBalance(null);
            setLedger([]);
            setDetailError('');
            return;
        }
        const controller = new AbortController();
        setDetailLoading(true);
        setDetailError('');
        Promise.all([
            AdminCatalogService.getInventoryBalance(productId, controller.signal),
            AdminCatalogService.getInventoryLedger(productId, controller.signal)
        ]).then(([nextBalance, nextLedger]) => {
            if (controller.signal.aborted) return;
            setBalance(nextBalance);
            setLedger(nextLedger ?? []);
            setAnnouncement(`Đã tải tồn kho của ${selectedProduct?.name ?? `sản phẩm ${productId}`}.`);
        }).catch(error => {
            if (!controller.signal.aborted && !isAbortError(error)) setDetailError(getApiError(error, 'Không thể tải dữ liệu tồn kho.'));
        }).finally(() => {
            if (!controller.signal.aborted) setDetailLoading(false);
        });
        return () => controller.abort();
    }, [productId, detailRetry, selectedProduct?.name]);

    const outOfStockCount = useMemo(() => lowStockProducts.filter(product => inventoryQuantity(product) <= 0).length, [lowStockProducts]);
    const lowCount = Math.max(lowStockProducts.length - outOfStockCount, 0);
    const lowStockTotalPages = Math.ceil(lowStockProducts.length / LOW_STOCK_PAGE_SIZE);
    const visibleLowStockProducts = lowStockProducts.slice(lowStockPage * LOW_STOCK_PAGE_SIZE, (lowStockPage + 1) * LOW_STOCK_PAGE_SIZE);
    const canSetOpeningStock = Boolean(productId) && !detailLoading && !detailError && ledger.length === 0;

    const selectProduct = (product: InventorySelection, focusDetail = false) => {
        setSelectedProduct(product);
        setProductId(product.id);
        if (focusDetail) window.setTimeout(() => {
            detailRef.current?.scrollIntoView?.({behavior: 'smooth', block: 'start'});
            detailRef.current?.focus({preventScroll: true});
        }, 0);
    };
    const openModal = (nextMode: 'opening' | 'adjust') => {
        setMode(nextMode);
        setQuantity('');
        setType(nextMode === 'adjust' && ledger.length > 0 ? 'CORRECTION' : 'STOCK_IN');
        setNote('');
        setFormError('');
    };
    const validateAdjustment = (numericQuantity: number): string => {
        if (!Number.isFinite(numericQuantity)) return 'Vui lòng nhập số lượng hợp lệ.';
        if (mode === 'opening') {
            if (!canSetOpeningStock) return 'Tồn đầu kỳ chỉ được thiết lập khi sản phẩm chưa có lịch sử kho.';
            return numericQuantity < 0 ? 'Tồn đầu kỳ không được nhỏ hơn 0.' : '';
        }
        if (type === 'CORRECTION') return numericQuantity < (balance?.reserved ?? 0)
            ? `Tồn kiểm kê không được thấp hơn lượng đang giữ (${balance?.reserved ?? 0}).` : '';
        if (numericQuantity <= 0) return 'Số lượng điều chỉnh phải lớn hơn 0.';
        if ((type === 'STOCK_OUT' || type === 'DAMAGED') && numericQuantity > (balance?.stockAvailable ?? 0))
            return `Số lượng không được vượt tồn khả dụng (${balance?.stockAvailable ?? 0}).`;
        return '';
    };
    const submit = async (event: FormEvent) => {
        event.preventDefault();
        if (!productId || saving) return;
        const normalizedNote = note.trim();
        if (!normalizedNote) {
            setFormError('Vui lòng nhập ghi chú đối soát.');
            return;
        }
        const numericQuantity = Number(quantity);
        const validationMessage = validateAdjustment(numericQuantity);
        if (validationMessage) {
            setFormError(validationMessage);
            return;
        }
        setSaving(true);
        setFormError('');
        try {
            if (mode === 'opening') await AdminCatalogService.setOpeningStock(productId, numericQuantity, normalizedNote);
            else await AdminCatalogService.adjustInventory(productId, type, numericQuantity, normalizedNote);
            const message = mode === 'opening' ? 'Đã thiết lập tồn đầu kỳ.' : 'Đã ghi nhận điều chỉnh kho.';
            ToastService.success(message);
            setAnnouncement(message);
            setMode(null);
            setDetailRetry(value => value + 1);
            setOverviewRetry(value => value + 1);
            setProductListRetry(value => value + 1);
        } catch (error) {
            const message = getApiError(error, 'Không thể ghi nhận điều chỉnh kho.');
            setFormError(message);
            ToastService.error(message);
        } finally {
            setSaving(false);
        }
    };
    const renderStockState = (product: {stockAvailable?: number | null; stockRemain?: number | null}) =>
        <StockIndicator stock={inventoryQuantity(product)} threshold={threshold}/>;
    const changeInventoryFilter = (update: () => void) => {
        update();
        setProductPage(0);
    };
    const refreshInventory = () => {
        setProductListRetry(value => value + 1);
        setOverviewRetry(value => value + 1);
        if (productId) setDetailRetry(value => value + 1);
    };
    const quantityLabel = mode === 'opening'
        ? 'Số lượng tồn đầu kỳ'
        : type === 'CORRECTION' ? 'Tồn thực tế sau kiểm kê' : 'Số lượng';

    return <AdminPage title="Tồn kho"
                      description="Theo dõi tồn thực tế, tồn khả dụng, lượng đang giữ và lịch sử điều chỉnh.">
        <div className="sr-only" aria-live="polite">{announcement}</div>
        <section aria-labelledby="low-stock-title"><AdminCard>
            <header
                className="flex flex-col justify-between gap-3 border-b border-outline-variant/30 p-4 sm:flex-row sm:items-center">
                <div><h2 id="low-stock-title" className="flex items-center gap-2 font-black text-on-surface">
                    <AlertTriangle className="h-5 w-5 text-amber-600"/>Cảnh báo tồn thấp</h2>
                    <p className="mt-1 text-xs text-outline">Ngưỡng hiện hành: tồn khả dụng ≤ {threshold}</p></div>
                <AdminButton variant="secondary" disabled={overviewLoading || productsLoading || detailLoading}
                             onClick={refreshInventory}>
                    <RefreshCw
                        className={`h-4 w-4 ${overviewLoading ? 'animate-spin motion-reduce:animate-none' : ''}`}/>Làm
                    mới
                </AdminButton>
            </header>
            <div className="grid gap-3 p-4 sm:grid-cols-3">
                <div className="rounded-xl bg-error/10 p-4"><p className="text-xs font-bold uppercase text-error">Hết
                    hàng</p><p className="mt-1 text-2xl font-black text-error">{outOfStockCount}</p></div>
                <div className="rounded-xl bg-amber-500/15 p-4"><p
                    className="text-xs font-bold uppercase text-amber-700 dark:text-amber-300">Sắp hết</p><p
                    className="mt-1 text-2xl font-black text-amber-800 dark:text-amber-200">{lowCount}</p></div>
                <div className="rounded-xl bg-primary/10 p-4"><p
                    className="text-xs font-bold uppercase text-primary">Ngưỡng cảnh báo</p><p
                    className="mt-1 text-2xl font-black text-primary">{threshold}</p></div>
            </div>
            {overviewLoading ? <AdminLoading label="Đang tải cảnh báo tồn thấp..."/> : overviewError
                ? <AdminError message={overviewError} onRetry={() => setOverviewRetry(value => value + 1)}/>
                : !visibleLowStockProducts.length ? <AdminEmpty title="Không có sản phẩm tồn thấp"
                                                                description="Tất cả sản phẩm active đang cao hơn ngưỡng cảnh báo."/>
                    : <>
                        <div className="hidden overflow-x-auto md:block">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-surface-container text-xs uppercase text-outline">
                                <tr>
                                    <th className="px-4 py-3">Sản phẩm</th>
                                    <th className="px-4 py-3 text-right">Tồn khả dụng</th>
                                    <th className="px-4 py-3 text-right">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>{visibleLowStockProducts.map(product => <tr key={product.id}
                                                                                   className="border-t border-outline-variant/25">
                                    <td className="px-4 py-3"><p
                                        className="font-bold text-on-surface">{product.name}</p><p
                                        className="text-xs text-outline">{product.code || `#${product.id}`}</p></td>
                                    <td className="px-4 py-3 text-right">{renderStockState(product)}</td>
                                    <td className="px-4 py-3 text-right"><AdminButton variant="ghost"
                                                                                      className="!min-h-10 !px-3"
                                                                                      onClick={() => selectProduct(product, true)}>Xem
                                        sổ kho</AdminButton></td>
                                </tr>)}</tbody>
                            </table>
                        </div>
                        <div className="space-y-3 p-4 md:hidden">{visibleLowStockProducts.map(product => <button
                            key={product.id} type="button" onClick={() => selectProduct(product, true)}
                            className="flex min-h-16 w-full cursor-pointer items-center justify-between gap-3 rounded-xl border border-outline-variant/35 bg-surface p-4 text-left transition-colors hover:bg-surface-container focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30">
                            <span className="min-w-0"><span
                                className="block truncate font-bold text-on-surface">{product.name}</span><span
                                className="text-xs text-outline">{product.code || `#${product.id}`}</span></span>{renderStockState(product)}
                        </button>)}</div>
                        <AdminPagination page={lowStockPage} totalPages={lowStockTotalPages}
                                         onChange={setLowStockPage}/></>}
        </AdminCard></section>

        <section aria-labelledby="inventory-list-title"><AdminCard>
            <header className="border-b border-outline-variant/30 px-4 pt-4">
                <h2 id="inventory-list-title" className="font-black text-on-surface">Danh sách tồn kho</h2>
                <p className="mt-1 pb-4 text-xs text-outline">Hiển thị 20 sản phẩm mỗi trang. Chọn một sản phẩm để xem số dư và sổ kho.</p>
            </header>
            <AdminToolbar>
                <AdminSearch value={productQuery} onChange={value => changeInventoryFilter(() => setProductQuery(value))}
                             placeholder="Tìm sản phẩm theo tên hoặc mã" ariaLabel="Tìm kiếm tồn kho"/>
                <AdminFilterGroup label="Lọc và sắp xếp">
                    <label className="min-w-0 flex-1 sm:flex-none"><span className="sr-only">Trạng thái tồn kho</span><select
                        aria-label="Trạng thái tồn kho" value={stockStatus}
                        onChange={event => changeInventoryFilter(() => setStockStatus(event.target.value as InventoryStockStatus))}
                        className={`${inputClass} min-w-[10rem] cursor-pointer bg-surface-container-lowest sm:w-auto`}>
                        <option value="ALL">Tất cả tồn kho</option><option value="IN_STOCK">Còn hàng</option>
                        <option value="LOW_STOCK">Tồn thấp</option><option value="OUT_OF_STOCK">Hết hàng</option>
                    </select></label>
                    <label className="min-w-0 flex-1 sm:flex-none"><span className="sr-only">Sắp xếp tồn kho</span><select
                        aria-label="Sắp xếp tồn kho" value={`${sortBy}:${sortDirection}`}
                        onChange={event => changeInventoryFilter(() => {
                            const [nextSortBy, nextDirection] = event.target.value.split(':') as [InventorySortBy, 'ASC' | 'DESC'];
                            setSortBy(nextSortBy);
                            setSortDirection(nextDirection);
                        })} className={`${inputClass} min-w-[12rem] cursor-pointer bg-surface-container-lowest sm:w-auto`}>
                        <option value="name:ASC">Tên: A–Z</option><option value="name:DESC">Tên: Z–A</option>
                        <option value="stockAvailable:ASC">Tồn khả dụng: thấp đến cao</option>
                        <option value="stockAvailable:DESC">Tồn khả dụng: cao đến thấp</option>
                    </select></label>
                </AdminFilterGroup>
            </AdminToolbar>
            {productsLoading ? <AdminLoading label="Đang tải danh sách tồn kho..."/> : productsError
                ? <AdminError message={productsError} onRetry={() => setProductListRetry(value => value + 1)}/>
                : !products.length ? <AdminEmpty title="Không có sản phẩm phù hợp" description="Thử thay đổi từ khóa hoặc bộ lọc."/>
                    : <><div className="hidden overflow-x-auto md:block"><table className="w-full min-w-[760px] text-left text-sm">
                        <thead className="bg-surface-container text-xs uppercase text-outline"><tr><th className="px-4 py-3">Sản phẩm</th>
                            <th className="px-4 py-3 text-right">Tồn thực tế</th><th className="px-4 py-3 text-right">Tồn khả dụng</th>
                            <th className="px-4 py-3 text-right">Đang giữ</th><th className="px-4 py-3 text-right">Thao tác</th></tr></thead>
                        <tbody>{products.map(product => <tr key={product.id} className="border-t border-outline-variant/25 hover:bg-surface-container-low">
                            <td className="px-4 py-3"><p className="font-bold text-on-surface">{product.name}</p><p className="text-xs text-outline">{product.code || `#${product.id}`}</p></td>
                            <td className="px-4 py-3 text-right font-bold">{product.stockRemain ?? 0}</td>
                            <td className="px-4 py-3 text-right">{renderStockState(product)}</td><td className="px-4 py-3 text-right font-bold text-amber-700">{product.reserved ?? 0}</td>
                            <td className="px-4 py-3 text-right"><AdminButton variant="ghost" className="!min-h-10 !px-3" onClick={() => selectProduct(product, true)}>Xem sổ kho</AdminButton></td>
                        </tr>)}</tbody></table></div>
                        <div className="space-y-3 p-4 md:hidden">{products.map(product => <button key={product.id} type="button" onClick={() => selectProduct(product, true)}
                            className="w-full cursor-pointer rounded-xl border border-outline-variant/35 bg-surface p-4 text-left transition-colors hover:bg-surface-container focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30">
                            <span className="block font-bold text-on-surface">{product.name}</span><span className="text-xs text-outline">{product.code || `#${product.id}`}</span>
                            <span className="mt-3 grid grid-cols-3 gap-2 text-xs"><span><span className="block font-bold uppercase text-outline">Thực tế</span>{product.stockRemain ?? 0}</span><span><span className="block font-bold uppercase text-outline">Khả dụng</span>{product.stockAvailable ?? 0}</span><span><span className="block font-bold uppercase text-outline">Đang giữ</span>{product.reserved ?? 0}</span></span>
                        </button>)}</div><AdminPagination page={productPage} totalPages={productTotalPages} onChange={setProductPage}/></>}
        </AdminCard></section>

        <div ref={detailRef} tabIndex={-1} className="scroll-mt-32 outline-none">
            {!productId ? <AdminCard><AdminEmpty title="Chọn sản phẩm để xem tồn kho"/></AdminCard>
                : detailLoading ? <AdminCard><AdminLoading label="Đang tải số dư và sổ kho..."/></AdminCard>
                    : detailError ? <AdminCard><AdminError message={detailError}
                                                           onRetry={() => setDetailRetry(value => value + 1)}/></AdminCard>
                        : <>
                            <div className="mb-4 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                                <div><h2
                                    className="text-lg font-black text-on-surface">{selectedProduct?.name || `Sản phẩm #${productId}`}</h2>
                                    <p className="mt-1 text-xs text-outline">{selectedProduct?.code || `#${productId}`} ·
                                        Dữ liệu số dư do backend xác nhận</p>
                                    {!canSetOpeningStock && ledger.length > 0 &&
                                        <p className="mt-2 text-xs font-semibold text-amber-800">Sản phẩm đã có lịch sử
                                            kho. Dùng “Điều chỉnh kho” thay cho tồn đầu kỳ.</p>}</div>
                                <div className="flex flex-wrap gap-2"><AdminButton variant="secondary"
                                                                                   disabled={!canSetOpeningStock}
                                                                                   title={!canSetOpeningStock && ledger.length > 0 ? 'Chỉ dùng cho sản phẩm chưa có lịch sử kho' : undefined}
                                                                                   onClick={() => openModal('opening')}><PackageCheck
                                    className="h-4 w-4"/>Tồn đầu kỳ</AdminButton><AdminButton disabled={!balance}
                                                                                              onClick={() => openModal('adjust')}><ArrowDownToLine
                                    className="h-4 w-4"/>Điều chỉnh kho</AdminButton></div>
                            </div>
                            <div className="grid gap-4 sm:grid-cols-3"><AdminCard className="p-5"><p
                                className="text-xs font-bold uppercase text-outline">Tồn thực tế</p><p
                                className="mt-2 text-3xl font-black">{balance?.stockRemain ?? 0}</p>
                            </AdminCard><AdminCard className="p-5"><p
                                className="text-xs font-bold uppercase text-outline">Tồn khả dụng</p><p
                                className="mt-2 text-3xl font-black text-primary">{balance?.stockAvailable ?? 0}</p>
                            </AdminCard><AdminCard className="p-5"><p
                                className="text-xs font-bold uppercase text-outline">Đang giữ cho đơn</p><p
                                className="mt-2 text-3xl font-black text-amber-700">{balance?.reserved ?? 0}</p>
                            </AdminCard></div>
                            <AdminCard className="mt-4">
                                <div
                                    className="flex items-center justify-between border-b border-outline-variant/30 px-4 py-3">
                                    <div><h2 className="flex items-center gap-2 font-black"><Warehouse
                                        className="h-4 w-4 text-primary"/>Sổ kho</h2><p
                                        className="text-xs text-outline">{selectedProduct?.name}</p></div>
                                    <button type="button" onClick={() => setDetailRetry(value => value + 1)}
                                            className="flex min-h-10 min-w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-surface-container hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
                                            aria-label="Làm mới sổ kho"><RefreshCw className="h-4 w-4"/></button>
                                </div>
                                {!ledger.length ? <AdminEmpty title="Chưa có biến động kho"/> : <>
                                    <div className="hidden overflow-x-auto md:block">
                                        <table className="w-full min-w-[860px] text-sm">
                                            <thead
                                                className="bg-surface-container text-left text-xs uppercase text-outline">
                                            <tr>
                                                <th className="px-4 py-3">Thời gian</th>
                                                <th className="px-4 py-3">Loại</th>
                                                <th className="px-4 py-3 text-right">Thay đổi</th>
                                                <th className="px-4 py-3 text-right">Số dư sau</th>
                                                <th className="px-4 py-3">Đối soát</th>
                                                <th className="px-4 py-3">Người thực hiện</th>
                                            </tr>
                                            </thead>
                                            <tbody>{ledger.map(entry => <tr key={entry.id}
                                                                            className="border-t border-outline-variant/25">
                                                <td className="px-4 py-3 text-outline">{formatDate(entry.createdAt)}</td>
                                                <td className="px-4 py-3 font-bold">{typeNames[entry.type] || entry.type}</td>
                                                <td className={`px-4 py-3 text-right font-black ${entry.quantityDelta >= 0 ? 'text-emerald-700' : 'text-error'}`}>{entry.quantityDelta > 0 ? '+' : ''}{entry.quantityDelta}</td>
                                                <td className="px-4 py-3 text-right"><span
                                                    className="block font-bold">{entry.balanceAfter}</span><span
                                                    className="text-[10px] text-outline">{balanceLabel(entry)}</span>
                                                </td>
                                                <td className="px-4 py-3 text-outline">{entry.orderId ? <Link
                                                    className="inline-flex items-center gap-1 font-bold text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
                                                    to={`/admin/orders/${entry.orderId}`}><ShoppingCart
                                                    className="h-3.5 w-3.5"/>#{entry.orderCode || entry.orderId}
                                                </Link> : entry.note || '—'}</td>
                                                <td className="px-4 py-3 text-outline">{entry.actor || 'system'}</td>
                                            </tr>)}</tbody>
                                        </table>
                                    </div>
                                    <div className="space-y-3 p-4 md:hidden">{ledger.map(entry => <article
                                        key={entry.id} className="rounded-xl border border-outline-variant/35 p-4">
                                        <div className="flex items-start justify-between gap-3">
                                            <div><p
                                                className="font-black text-on-surface">{typeNames[entry.type] || entry.type}</p>
                                                <p className="mt-1 text-xs text-outline">{formatDate(entry.createdAt)}</p>
                                            </div>
                                            <span
                                                className={`text-lg font-black ${entry.quantityDelta >= 0 ? 'text-emerald-700' : 'text-error'}`}>{entry.quantityDelta > 0 ? '+' : ''}{entry.quantityDelta}</span>
                                        </div>
                                        <dl className="mt-4 grid grid-cols-2 gap-3 text-xs">
                                            <div>
                                                <dt className="font-bold uppercase text-outline">{balanceLabel(entry)}</dt>
                                                <dd className="mt-1 font-black text-on-surface">{entry.balanceAfter}</dd>
                                            </div>
                                            <div>
                                                <dt className="font-bold uppercase text-outline">Người thực hiện</dt>
                                                <dd className="mt-1 break-all font-bold text-on-surface">{entry.actor || 'system'}</dd>
                                            </div>
                                        </dl>
                                        <div
                                            className="mt-3 border-t border-outline-variant/25 pt-3 text-xs text-outline">{entry.orderId ?
                                            <Link
                                                className="inline-flex min-h-10 items-center gap-1 font-bold text-primary hover:underline"
                                                to={`/admin/orders/${entry.orderId}`}><ShoppingCart
                                                className="h-3.5 w-3.5"/>Đơn #{entry.orderCode || entry.orderId}
                                            </Link> : entry.note || 'Không có ghi chú'}</div>
                                    </article>)}</div>
                                </>}
                            </AdminCard></>}
        </div>

        <AdminModal open={Boolean(mode)} onClose={() => !saving && setMode(null)}
                    title={mode === 'opening' ? 'Thiết lập tồn đầu kỳ' : 'Điều chỉnh tồn kho'}
                    description={selectedProduct?.name} size="md">
            <form noValidate onSubmit={submit} className="space-y-4 p-5">{mode === 'adjust' &&
                <Field label="Loại điều chỉnh"><select aria-label="Loại điều chỉnh" data-autofocus
                                                       className={`${inputClass} admin-select`} value={type} onChange={event => {
                    setType(event.target.value as ManualType);
                    setQuantity('');
                    setFormError('');
                }}>
                    <option value="STOCK_IN">Nhập kho</option>
                    <option value="STOCK_OUT">Xuất kho</option>
                    <option value="CORRECTION">Điều chỉnh kiểm kê</option>
                    <option value="DAMAGED">Hàng hỏng</option>
                    <option value="RETURNED">Hoàn kho</option>
                </select></Field>}
                <Field label={quantityLabel}
                       hint={type === 'CORRECTION' && mode === 'adjust' ? `Nhập tồn thực tế mục tiêu; không thấp hơn ${balance?.reserved ?? 0} sản phẩm đang giữ.` : 'Hệ thống backend xác định chiều tăng/giảm theo loại điều chỉnh.'}><input
                    aria-label={quantityLabel} data-autofocus={mode === 'opening' ? true : undefined} required
                    type="number" step="any"
                    min={mode === 'opening' ? 0 : type === 'CORRECTION' ? balance?.reserved ?? 0 : 0.01}
                    max={mode === 'adjust' && (type === 'STOCK_OUT' || type === 'DAMAGED') ? balance?.stockAvailable : undefined}
                    className={inputClass} value={quantity} onChange={event => {
                    setQuantity(event.target.value);
                    setFormError('');
                }}/></Field>
                <Field label="Ghi chú đối soát" hint="Bắt buộc nhập lý do để truy vết trong sổ kho."><textarea
                    aria-label="Ghi chú đối soát" required className={`${inputClass} min-h-24 py-2`} value={note}
                    onChange={event => {
                        setNote(event.target.value);
                        setFormError('');
                    }} placeholder="Lý do và thông tin đối soát"/></Field>
                {formError && <div role="alert"
                                   className="rounded-lg border border-error/20 bg-error/10 px-3 py-2 text-sm font-bold text-error">{formError}</div>}
                <div
                    className="flex flex-col-reverse gap-2 border-t border-outline-variant/30 pt-4 sm:flex-row sm:justify-end">
                    <AdminButton type="button" variant="secondary" disabled={saving}
                                 onClick={() => setMode(null)}>Hủy</AdminButton><AdminButton type="submit"
                                                                                             disabled={saving}>{saving ? 'Đang ghi nhận...' : 'Xác nhận'}</AdminButton>
                </div>
            </form>
        </AdminModal>
    </AdminPage>;
}
