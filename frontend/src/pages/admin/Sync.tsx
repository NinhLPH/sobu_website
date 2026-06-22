import {useState, useEffect} from 'react';
import {useSearchParams} from 'react-router-dom';
import {
    RefreshCw,
    CheckCircle2,
    AlertCircle,
    Loader2,
    Package,
    ListTree,
    Award,
    ShoppingCart,
    Clock3
} from 'lucide-react';
import {AdminSyncService} from '../../service/sync.service';
import {ToastService} from '../../service/toast.service';
import {useProductStore} from '../../store/useProductStore';
import {useAdminStore} from '../../store/useAdminStore';

const getOrderSyncStatusStyle = (status?: string) => {
    switch (status) {
        case 'FAILED':
            return 'border-red-200 bg-red-50 text-red-700';
        case 'NEED_RECONCILE':
            return 'border-orange-200 bg-orange-50 text-orange-700';
        case 'DEAD':
            return 'border-slate-300 bg-slate-100 text-slate-700';
        default:
            return 'border-gray-200 bg-gray-50 text-gray-700';
    }
};

const getOrderSyncStatusText = (status?: string) => {
    switch (status) {
        case 'FAILED':
            return 'Thất bại';
        case 'NEED_RECONCILE':
            return 'Cần đối soát';
        case 'DEAD':
            return 'Đã dừng retry';
        default:
            return status || 'Chưa cập nhật';
    }
};

export default function AdminSync() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [isConnecting, setIsConnecting] = useState(false);
    const [oauthStatus, setOauthStatus] = useState<'success' | 'error' | null>(null);
    const [oauthErrorMsg, setOauthErrorMsg] = useState<string | null>(null);
    const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);

    const {
        isSyncingProducts,
        isSyncingCategories,
        isSyncingBrands,
        triggerSyncProducts,
        triggerSyncCategories,
        triggerSyncBrands
    } = useProductStore();
    const isSyncing = {
        products: isSyncingProducts,
        categories: isSyncingCategories,
        brands: isSyncingBrands
    };
    const {
        orderSyncQueue,
        pendingOrderSyncCount,
        retryingOrderIds,
        isOrderSyncQueueLoading,
        orderSyncQueueError,
        orderSyncBatchProgress,
        orderSyncBatchResult,
        fetchOrderSyncQueue,
        retryOrderSync,
        retryOrderSyncBatch
    } = useAdminStore();

    const [syncMessages, setSyncMessages] = useState<{ [key: string]: string | null }>({
        products: null,
        categories: null,
        brands: null
    });

    const [syncErrors, setSyncErrors] = useState<{ [key: string]: string | null }>({
        products: null,
        categories: null,
        brands: null
    });

    useEffect(() => {
        const oauth = searchParams.get('oauth');
        const errMsg = searchParams.get('error');
        if (oauth === 'success') {
            setOauthStatus('success');
            setSearchParams({}, {replace: true});
        } else if (oauth === 'error') {
            setOauthStatus('error');
            setOauthErrorMsg(errMsg || 'Đã có lỗi xảy ra trong quá trình xác thực OAuth.');
            setSearchParams({}, {replace: true});
        }
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        const handleMessage = (event: MessageEvent) => {
            if (event.data === 'NHANH_AUTH_SUCCESS') {
                setOauthStatus('success');
                console.log('NHANH_AUTH_SUCCESS');
                ToastService.success('Kết nối tài khoản ERP Nhanh.vn thành công!');
            }
        };
        window.addEventListener('message', handleMessage);
        return () => window.removeEventListener('message', handleMessage);
    }, []);

    useEffect(() => {
        void fetchOrderSyncQueue();
    }, [fetchOrderSyncQueue]);

    useEffect(() => {
        const queueIds = new Set(orderSyncQueue.map(order => order.id));
        setSelectedOrderIds(current => current.filter(id => queueIds.has(id)));
    }, [orderSyncQueue]);

    const handleNhanhConnect = async () => {
        setIsConnecting(true);
        setOauthStatus(null);
        setOauthErrorMsg(null);
        try {
            const url = await AdminSyncService.getNhanhLoginUrl();
            if (url) {
                window.open(url, 'NhanhAuth', 'width=600,height=700,left=200,top=100');
            } else {
                throw new Error("Không lấy được URL đăng nhập từ hệ thống.");
            }
        } catch (err: any) {
            console.error("Lỗi lấy URL kết nối Nhanh.vn:", err);
            setOauthStatus('error');
            setOauthErrorMsg(err?.response?.data?.message || err?.message || 'Không thể lấy URL đăng nhập Nhanh.vn.');
        } finally {
            setIsConnecting(false);
        }
    };

    const handleSync = async (type: 'products' | 'categories' | 'brands') => {
        setSyncMessages(prev => ({...prev, [type]: null}));
        setSyncErrors(prev => ({...prev, [type]: null}));

        try {
            let res: { message: string };
            if (type === 'products') {
                res = await triggerSyncProducts();
            } else if (type === 'categories') {
                res = await triggerSyncCategories();
            } else {
                res = await triggerSyncBrands();
            }

            setSyncMessages(prev => ({...prev, [type]: res?.message || 'Đồng bộ dữ liệu thành công!'}));
        } catch (err: any) {
            const errorMsg = 'Có lỗi xảy ra trong quá trình đồng bộ ERP!';
            setSyncErrors(prev => ({...prev, [type]: errorMsg}));
        }
    };

    const isBatchRunning = Boolean(orderSyncBatchProgress?.running);
    const allOrdersSelected = orderSyncQueue.length > 0
        && orderSyncQueue.every(order => selectedOrderIds.includes(order.id));

    const toggleOrderSelection = (orderId: number) => {
        setSelectedOrderIds(current => current.includes(orderId)
            ? current.filter(id => id !== orderId)
            : [...current, orderId]);
    };

    const toggleAllOrders = () => {
        setSelectedOrderIds(allOrdersSelected ? [] : orderSyncQueue.map(order => order.id));
    };

    const handleRetryOrder = async (orderId: number) => {
        try {
            await retryOrderSync(orderId);
        } catch {
            // The store keeps the backend error on the matching queue row.
        }
    };

    const handleRetrySelectedOrders = async () => {
        if (selectedOrderIds.length === 0) {
            return;
        }
        try {
            await retryOrderSyncBatch(selectedOrderIds);
            setSelectedOrderIds([]);
        } catch {
            // The store exposes batch and request errors in the sync queue state.
        }
    };

    return (
        <div className="pt-6 space-y-6">
            <div>
                <h1 className="text-2xl font-black text-on-surface uppercase tracking-tight">Trung tâm đồng bộ ERP
                    Nhanh.vn</h1>
                <p className="text-xs text-outline font-bold mt-1">Đồng bộ dữ liệu thời gian thực từ phần mềm quản lý
                    bán hàng Nhanh.vn về cơ sở dữ liệu local.</p>
            </div>

            {/* OAuth Nhanh.vn Integration Section */}
            <div
                className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col md:flex-row justify-between items-center gap-6">
                <div className="space-y-2 max-w-2xl font-bold">
                    <h2 className="text-sm font-black text-on-surface uppercase tracking-wide">Tích hợp kết nối ERP
                        Nhanh.vn</h2>
                    <p className="text-xs text-outline leading-relaxed font-semibold">
                        Kết nối hệ thống bán hàng SOBU với tài khoản ERP Nhanh.vn qua giao thức OAuth2. Hệ thống sẽ tự
                        động ủy quyền để đồng bộ sản phẩm, danh mục, thương hiệu và đơn hàng.
                    </p>
                </div>
                <button
                    onClick={handleNhanhConnect}
                    disabled={isConnecting}
                    className="w-full md:w-auto px-6 py-3.5 bg-primary text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-2 cursor-pointer shrink-0"
                >
                    {isConnecting ? (
                        <>
                            <Loader2 className="w-4 h-4 animate-spin"/>
                            <span>Đang kết nối...</span>
                        </>
                    ) : (
                        <>
                            <RefreshCw className="w-4 h-4"/>
                            <span>Kết nối Nhanh.vn</span>
                        </>
                    )}
                </button>
            </div>

            {/* Connection Status Banner */}
            {oauthStatus === 'success' && (
                <div
                    className="p-4 bg-green-50 border border-green-200 rounded-2xl text-green-800 text-xs font-bold flex gap-3 items-center">
                    <CheckCircle2 className="w-5 h-5 text-green-600 shrink-0"/>
                    <span>Kết nối tài khoản ERP Nhanh.vn thành công! Hệ thống sẵn sàng đồng bộ.</span>
                </div>
            )}

            {oauthStatus === 'error' && (
                <div
                    className="p-4 bg-error/10 border border-error/20 rounded-2xl text-error text-xs font-bold flex gap-3 items-start">
                    <AlertCircle className="w-5 h-5 text-error shrink-0 mt-0.5"/>
                    <div className="space-y-1">
                        <span className="block uppercase tracking-wide">Lỗi kết nối Nhanh.vn OAuth:</span>
                        <p className="font-semibold text-outline-variant">{oauthErrorMsg}</p>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                {/* 1. PRODUCTS SYNC */}
                <div
                    className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col justify-between min-h-[300px]">
                    <div className="space-y-4">
                        <div
                            className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                            <Package className="w-6 h-6"/>
                        </div>
                        <div>
                            <h3 className="text-base font-black text-on-surface uppercase">Sản phẩm (Products)</h3>
                            <p className="text-xs text-outline leading-relaxed mt-1 font-semibold">
                                Đồng bộ tất cả sản phẩm mô hình, kiểm tra SKU sản phẩm, giá bán, giá cũ và số lượng tồn
                                kho khả dụng từ Nhanh.vn.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-4 pt-4 border-t border-surface-container">
                        {syncMessages.products && (
                            <div
                                className="p-3 rounded-xl bg-green-50 text-green-700 text-[10px] font-bold flex gap-2 items-center">
                                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0"/>
                                <span>{syncMessages.products}</span>
                            </div>
                        )}
                        {syncErrors.products && (
                            <div
                                className="p-3 rounded-xl bg-error/10 text-error text-[10px] font-bold flex gap-2 items-start">
                                <AlertCircle className="w-4 h-4 text-error shrink-0 mt-0.5"/>
                                <span className="text-red-600">{syncErrors.products}</span>
                            </div>
                        )}

                        <button
                            onClick={() => handleSync('products')}
                            disabled={isSyncing.products}
                            className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                            {isSyncing.products ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin"/>
                                    <span>Đang đồng bộ, vui lòng không tắt trang...</span>
                                </>
                            ) : (
                                <>
                                    <RefreshCw className="w-4 h-4"/>
                                    <span>Đồng bộ Sản phẩm</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>

                {/* 2. CATEGORIES SYNC */}
                <div
                    className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col justify-between min-h-[300px]">
                    <div className="space-y-4">
                        <div
                            className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                            <ListTree className="w-6 h-6"/>
                        </div>
                        <div>
                            <h3 className="text-base font-black text-on-surface uppercase">Danh mục (Categories)</h3>
                            <p className="text-xs text-outline leading-relaxed mt-1 font-semibold">
                                Đồng bộ toàn bộ cây danh mục sản phẩm từ ERP để cập nhật cấu trúc phân mục chính và danh
                                mục nhỏ trên trang cửa hàng SOBU.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-4 pt-4 border-t border-surface-container">
                        {syncMessages.categories && (
                            <div
                                className="p-3 rounded-xl bg-green-50 text-green-700 text-[10px] font-bold flex gap-2 items-center">
                                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0"/>
                                <span>{syncMessages.categories}</span>
                            </div>
                        )}
                        {syncErrors.categories && (
                            <div
                                className="p-3 rounded-xl bg-error/10 text-error text-[10px] font-bold flex gap-2 items-start">
                                <AlertCircle className="w-4 h-4 text-error shrink-0 mt-0.5"/>
                                <span className="text-red-600">{syncErrors.categories}</span>
                            </div>
                        )}

                        <button
                            onClick={() => handleSync('categories')}
                            disabled={isSyncing.categories}
                            className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                            {isSyncing.categories ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin"/>
                                    <span>Đang đồng bộ, vui lòng không tắt trang...</span>
                                </>
                            ) : (
                                <>
                                    <RefreshCw className="w-4 h-4"/>
                                    <span>Đồng bộ Danh mục</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>

                {/* 3. BRANDS SYNC */}
                <div
                    className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col justify-between min-h-[300px]">
                    <div className="space-y-4">
                        <div
                            className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                            <Award className="w-6 h-6"/>
                        </div>
                        <div>
                            <h3 className="text-base font-black text-on-surface uppercase">Thương hiệu (Brands)</h3>
                            <p className="text-xs text-outline leading-relaxed mt-1 font-semibold">
                                Đồng bộ danh sách thương hiệu đồ chơi mô hình chính hãng (Bandai, Mattel, Hot Toys,
                                LEGO, Tamiya) liên kết để hiển thị danh mục.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-4 pt-4 border-t border-surface-container">
                        {syncMessages.brands && (
                            <div
                                className="p-3 rounded-xl bg-green-50 text-green-700 text-[10px] font-bold flex gap-2 items-center">
                                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0"/>
                                <span>{syncMessages.brands}</span>
                            </div>
                        )}
                        {syncErrors.brands && (
                            <div
                                className="p-3 rounded-xl bg-error/10 text-error text-[10px] font-bold flex gap-2 items-start">
                                <AlertCircle className="w-4 h-4 text-error shrink-0 mt-0.5"/>
                                <span className="text-red-600">{syncErrors.brands}</span>
                            </div>
                        )}

                        <button
                            onClick={() => handleSync('brands')}
                            disabled={isSyncing.brands}
                            className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                            {isSyncing.brands ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin"/>
                                    <span>Đang đồng bộ, vui lòng không tắt trang...</span>
                                </>
                            ) : (
                                <>
                                    <RefreshCw className="w-4 h-4"/>
                                    <span>Đồng bộ Thương hiệu</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>

            </div>

            <section className="overflow-hidden rounded-3xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="flex flex-col gap-5 border-b border-surface-container p-6 lg:flex-row lg:items-center lg:justify-between">
                    <div className="flex items-start gap-4">
                        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                            <ShoppingCart className="h-6 w-6"/>
                        </div>
                        <div>
                            <h2 className="text-base font-black uppercase text-on-surface">Đồng bộ đơn hàng</h2>
                            <p className="mt-1 max-w-2xl text-xs font-semibold leading-relaxed text-outline">
                                Theo dõi các đơn cần can thiệp và gửi retry tuần tự sang Nhanh.vn. Đơn đang chờ sẽ tiếp
                                tục được backend tự động xử lý.
                            </p>
                            <div className="mt-3 flex flex-wrap gap-2 text-[10px] font-black uppercase tracking-wide">
                                <span className="rounded-full bg-red-50 px-3 py-1.5 text-red-700">
                                    Cần can thiệp: {orderSyncQueue.length}
                                </span>
                                <span className="flex items-center gap-1 rounded-full bg-amber-50 px-3 py-1.5 text-amber-700">
                                    <Clock3 className="h-3 w-3"/>
                                    Đang chờ tự động: {pendingOrderSyncCount}
                                </span>
                            </div>
                        </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={() => void fetchOrderSyncQueue()}
                            disabled={isOrderSyncQueueLoading || isBatchRunning}
                            className="flex items-center gap-2 rounded-xl bg-surface-container px-4 py-2.5 text-xs font-black uppercase tracking-wide text-on-surface disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            <RefreshCw className={`h-4 w-4 ${isOrderSyncQueueLoading ? 'animate-spin' : ''}`}/>
                            Làm mới
                        </button>
                        <button
                            type="button"
                            onClick={handleRetrySelectedOrders}
                            disabled={selectedOrderIds.length === 0 || isBatchRunning || isOrderSyncQueueLoading}
                            className="flex items-center gap-2 rounded-xl bg-gradient-to-br from-primary to-primary-container px-4 py-2.5 text-xs font-black uppercase tracking-wide text-white shadow-md disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {isBatchRunning ? <Loader2 className="h-4 w-4 animate-spin"/> : <RefreshCw className="h-4 w-4"/>}
                            Retry đã chọn ({selectedOrderIds.length})
                        </button>
                    </div>
                </div>

                {orderSyncBatchProgress && (
                    <div className="border-b border-surface-container bg-surface-container/30 px-6 py-4">
                        <div className="mb-2 flex justify-between gap-4 text-[11px] font-black uppercase tracking-wide text-on-surface">
                            <span>{orderSyncBatchProgress.running ? 'Đang xử lý hàng đợi' : 'Đã hoàn tất lượt retry'}</span>
                            <span>{orderSyncBatchProgress.current}/{orderSyncBatchProgress.total}</span>
                        </div>
                        <div className="h-2 overflow-hidden rounded-full bg-surface-container-high">
                            <div
                                className="h-full rounded-full bg-primary transition-all"
                                style={{
                                    width: `${orderSyncBatchProgress.total > 0
                                        ? (orderSyncBatchProgress.current / orderSyncBatchProgress.total) * 100
                                        : 0}%`
                                }}
                            />
                        </div>
                        <p className="mt-2 text-[10px] font-bold text-outline">
                            Đã sync: {orderSyncBatchProgress.synced} · Còn cần xử lý: {orderSyncBatchProgress.unresolved}
                            {' '}· Request lỗi: {orderSyncBatchProgress.failed}
                        </p>
                    </div>
                )}

                {orderSyncBatchResult && !isBatchRunning && (
                    <div className="mx-6 mt-5 rounded-2xl border border-green-200 bg-green-50 p-4 text-xs font-bold text-green-800">
                        Hoàn tất {orderSyncBatchResult.total} đơn: {orderSyncBatchResult.synced} đã đồng bộ,
                        {' '}{orderSyncBatchResult.unresolved} còn cần xử lý và {orderSyncBatchResult.failed} request lỗi.
                    </div>
                )}

                {orderSyncQueueError && (
                    <div className="mx-6 mt-5 flex items-start gap-2 rounded-2xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0"/>
                        {orderSyncQueueError}
                    </div>
                )}

                <div className="overflow-x-auto">
                    <table className="w-full min-w-[900px] text-left text-xs">
                        <thead className="bg-surface-container/60 text-[10px] font-black uppercase tracking-wider text-outline">
                        <tr>
                            <th className="w-12 px-6 py-4">
                                <input
                                    type="checkbox"
                                    checked={allOrdersSelected}
                                    onChange={toggleAllOrders}
                                    disabled={orderSyncQueue.length === 0 || isBatchRunning}
                                    aria-label="Chọn tất cả đơn cần retry"
                                    className="h-4 w-4 accent-primary"
                                />
                            </th>
                            <th className="px-4 py-4">Đơn hàng</th>
                            <th className="px-4 py-4">Trạng thái</th>
                            <th className="px-4 py-4">Milestone</th>
                            <th className="px-4 py-4">Nhanh ID</th>
                            <th className="px-4 py-4">Lỗi gần nhất</th>
                            <th className="px-4 py-4">Lần sync cuối</th>
                            <th className="px-6 py-4 text-right">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-surface-container">
                        {orderSyncQueue.map(order => {
                            const isRetrying = retryingOrderIds.includes(order.id);
                            return (
                                <tr key={order.id} className="hover:bg-surface-container/20">
                                    <td className="px-6 py-4">
                                        <input
                                            type="checkbox"
                                            checked={selectedOrderIds.includes(order.id)}
                                            onChange={() => toggleOrderSelection(order.id)}
                                            disabled={isBatchRunning || isRetrying}
                                            aria-label={`Chọn đơn ${order.orderCode || order.id}`}
                                            className="h-4 w-4 accent-primary"
                                        />
                                    </td>
                                    <td className="px-4 py-4 font-black text-primary">#{order.orderCode || order.id}</td>
                                    <td className="px-4 py-4">
                                        <span className={`inline-block rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-wide ${getOrderSyncStatusStyle(order.syncStatus)}`}>
                                            {getOrderSyncStatusText(order.syncStatus)}
                                        </span>
                                    </td>
                                    <td className="px-4 py-4 font-bold text-on-surface">{order.nhanhSyncStage || 'NONE'}</td>
                                    <td className="px-4 py-4 font-semibold text-on-surface">{order.nhanhOrderId || '—'}</td>
                                    <td className="max-w-xs px-4 py-4 font-semibold text-red-700">
                                        <span className="line-clamp-2" title={order.lastSyncMessage || order.syncError}>
                                            {order.lastSyncMessage || order.syncError || 'Không có chi tiết lỗi'}
                                        </span>
                                    </td>
                                    <td className="px-4 py-4 font-semibold text-outline">
                                        {order.lastSyncAt ? new Date(order.lastSyncAt).toLocaleString('vi-VN') : 'Chưa có'}
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <button
                                            type="button"
                                            onClick={() => void handleRetryOrder(order.id)}
                                            disabled={isRetrying || isBatchRunning}
                                            className="inline-flex items-center gap-1.5 rounded-xl bg-primary/10 px-3 py-2 text-[10px] font-black uppercase tracking-wide text-primary disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {isRetrying ? <Loader2 className="h-3.5 w-3.5 animate-spin"/> : <RefreshCw className="h-3.5 w-3.5"/>}
                                            {isRetrying ? 'Đang retry' : 'Retry'}
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}
                        {!isOrderSyncQueueLoading && orderSyncQueue.length === 0 && (
                            <tr>
                                <td colSpan={8} className="px-6 py-14 text-center font-bold text-outline">
                                    <CheckCircle2 className="mx-auto mb-2 h-8 w-8 text-green-500"/>
                                    Không có đơn hàng nào cần can thiệp đồng bộ.
                                </td>
                            </tr>
                        )}
                        {isOrderSyncQueueLoading && (
                            <tr>
                                <td colSpan={8} className="px-6 py-14 text-center font-bold text-outline">
                                    <Loader2 className="mx-auto mb-2 h-7 w-7 animate-spin text-primary"/>
                                    Đang tải toàn bộ hàng đợi đồng bộ...
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}
