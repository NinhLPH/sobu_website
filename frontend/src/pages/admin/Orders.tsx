import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    ArrowRight,
    ChevronLeft,
    ChevronRight,
    Eye,
    FileText,
    Loader2,
    RefreshCw
} from 'lucide-react';
import { AdminWorkflowService } from '../../service/admin.service';
import { useAdminStore } from '../../store/useAdminStore';
import { formatCurrency } from '../../utils/format';
import {SearchSuggestion} from '../../components/common/SearchSuggestInput';
import {useIntegrationStore} from '../../store/useIntegrationStore';
import {hasNhanhHistory} from '../../utils/order-sync';
import {AdminFilterGroup, AdminFilterReset, AdminFilterSelect, AdminSearch, AdminToolbar} from '../../components/admin/AdminUi';
import {useConfirmDialog} from '../../components/common/ConfirmDialog';
import {getNextAdminOrderStatus} from '../../utils/admin-order-status';

const getStatusColor = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
            return 'bg-amber-500/15 text-amber-700 dark:text-amber-300';
        case 'WAITING_DEPOSIT':
            return 'bg-yellow-500/15 text-yellow-700 dark:text-yellow-300';
        case 'DEPOSIT_PAID':
            return 'bg-cyan-500/15 text-cyan-700 dark:text-cyan-300';
        case 'READY_FOR_FINAL_PAYMENT':
            return 'bg-indigo-500/15 text-indigo-700 dark:text-indigo-300';
        case 'PROCESSING':
            return 'bg-blue-500/15 text-blue-700 dark:text-blue-300';
        case 'SHIPPED':
            return 'bg-purple-500/15 text-purple-700 dark:text-purple-300';
        case 'DELIVERED':
            return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300';
        case 'CANCELLED':
            return 'bg-error/10 text-error';
        default:
            return 'bg-surface-container text-on-surface-variant';
    }
};

const getStatusText = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
            return 'Mới';
        case 'WAITING_DEPOSIT':
            return 'Chờ đặt cọc';
        case 'DEPOSIT_PAID':
            return 'Đã đặt cọc';
        case 'READY_FOR_FINAL_PAYMENT':
            return 'Chờ thanh toán cuối';
        case 'PROCESSING':
            return 'Đang xử lý';
        case 'SHIPPED':
            return 'Đang giao';
        case 'DELIVERED':
            return 'Đã giao';
        case 'CANCELLED':
            return 'Đã hủy';
        default:
            return status || 'Chưa cập nhật';
    }
};

const getSyncStatusColor = (status?: string) => {
    switch (status) {
        case 'SYNCED':
            return 'border-emerald-500/25 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300';
        case 'FAILED':
            return 'border-error/25 bg-error/10 text-error';
        case 'NEED_RECONCILE':
            return 'border-orange-500/25 bg-orange-500/10 text-orange-700 dark:text-orange-300';
        case 'DEAD':
            return 'border-outline-variant/40 bg-surface-container text-on-surface-variant';
        case 'PENDING':
            return 'border-amber-500/25 bg-amber-500/10 text-amber-700 dark:text-amber-300';
        default:
            return 'border-outline-variant/40 bg-surface-container-low text-on-surface-variant';
    }
};

const getSyncStatusText = (status?: string) => {
    switch (status) {
        case 'SYNCED':
            return 'Đã đồng bộ';
        case 'FAILED':
            return 'Thất bại';
        case 'NEED_RECONCILE':
            return 'Cần đối soát';
        case 'DEAD':
            return 'Đã dừng retry';
        case 'PENDING':
            return 'Đang chờ';
        default:
            return status || 'Chưa cập nhật';
    }
};

const canRetrySync = (status?: string) =>
    status === 'FAILED' || status === 'NEED_RECONCILE' || status === 'DEAD';

export default function AdminOrders() {
    const confirm = useConfirmDialog();
    const {
        workflowOrders,
        fetchOrders,
        retryOrderSync,
        retryingOrderIds,
        updateAdminOrderStatus,
        updatingOrderStatusIds,
        isOrdersLoading,
        ordersError,
        ordersPage
    } = useAdminStore();
    const [page, setPage] = useState(0);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [syncFilter, setSyncFilter] = useState('ALL');
    const [sortBy, setSortBy] = useState('createdAt');
    const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('DESC');
    const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);
    const [isExporting, setIsExporting] = useState(false);
    const nhanhEnabled = useIntegrationStore(state => state.nhanhEnabled);
    const integrationLoaded = useIntegrationStore(state => state.loaded);
    const ensureIntegrationLoaded = useIntegrationStore(state => state.ensureLoaded);
    const showNhanhControls = integrationLoaded && nhanhEnabled;

    const handleExportSpx = async () => {
        try {
            setIsExporting(true);
            const payload = selectedOrderIds.length > 0
                ? { ids: selectedOrderIds }
                : { status: statusFilter !== 'ALL' ? statusFilter : undefined, query: searchTerm || undefined };
            const blob = await AdminWorkflowService.exportSpxOrders(payload);
            const url = window.URL.createObjectURL(new Blob([blob]));
            const link = document.createElement('a');
            link.href = url;
            const timestamp = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14);
            link.setAttribute('download', `Spx_Orders_${timestamp}.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error('Failed to export Spx Excel', err);
        } finally {
            setIsExporting(false);
        }
    };

    useEffect(() => {
        void ensureIntegrationLoaded();
    }, [ensureIntegrationLoaded]);

    useEffect(() => {
        if (!showNhanhControls) {
            setSyncFilter('ALL');
        }
    }, [showNhanhControls]);

    useEffect(() => {
        fetchOrders({ page, size: 10, sortBy, sortDirection });
    }, [fetchOrders, page, sortBy, sortDirection]);

    const filteredOrders = useMemo(() => {
        const search = searchTerm.trim().toLowerCase();
        return workflowOrders.filter((order) => {
            const matchesSearch = !search || [
                order.id,
                order.orderCode,
                order.customerName,
                order.customerMobile,
                order.nhanhOrderId,
                order.nhanhOrderCode
            ].some(value => String(value || '').toLowerCase().includes(search));
            const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter;
            const matchesSync = !showNhanhControls || syncFilter === 'ALL' || order.syncStatus === syncFilter;
            return matchesSearch && matchesStatus && matchesSync;
        });
    }, [workflowOrders, searchTerm, statusFilter, syncFilter, showNhanhControls]);

    const searchSuggestions = useMemo<SearchSuggestion[]>(() => workflowOrders.map((order) => {
        const primaryCode = order.orderCode || String(order.id);
        return {
            id: order.id,
            label: `#${primaryCode}`,
            description: [
                order.customerName,
                order.customerMobile,
                order.nhanhOrderCode || order.nhanhOrderId,
            ].filter(Boolean).join(' • '),
            searchValue: primaryCode,
        };
    }), [workflowOrders]);

    const refresh = () => {
        fetchOrders({ page, size: 10, sortBy, sortDirection });
    };

    const handleRetrySync = async (orderId: number) => {
        try {
            await retryOrderSync(orderId);
        } catch {
            // The store exposes the backend error through ordersError.
        }
    };

    const handleAdvanceOrderStatus = async (orderId: number, orderCode: string | number | undefined, nextStatusLabel: string, nextStatus: Parameters<typeof updateAdminOrderStatus>[1]) => {
        const confirmed = await confirm({
            title: 'Chuyển trạng thái đơn hàng?',
            message: `Đơn #${orderCode || orderId} sẽ chuyển sang trạng thái “${nextStatusLabel}”.`,
            confirmLabel: `Chuyển sang ${nextStatusLabel}`,
            tone: 'warning'
        });
        if (!confirmed) {
            return;
        }
        try {
            await updateAdminOrderStatus(orderId, nextStatus);
        } catch {
            // The admin store exposes the backend error through ordersError.
        }
    };

    return (
        <div className="space-y-6 pt-6">
            <div className="flex items-center justify-between gap-4">
                <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface">
                    {showNhanhControls ? 'Quản lý đơn hàng ERP' : 'Quản lý đơn hàng'}
                </h1>
                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={handleExportSpx}
                        disabled={isExporting || isOrdersLoading}
                        className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
                    >
                        {isExporting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <FileText className="h-3.5 w-3.5" />}
                        {selectedOrderIds.length > 0 ? `Xuất Excel SPX (${selectedOrderIds.length})` : 'Xuất Excel SPX'}
                    </button>
                    <button
                        type="button"
                        onClick={refresh}
                        disabled={isOrdersLoading}
                        className="flex items-center gap-1.5 rounded-xl bg-surface-container px-4 py-2 text-xs font-bold text-on-surface transition-colors hover:bg-surface-container-high disabled:opacity-50"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${isOrdersLoading ? 'animate-spin' : ''}`} />
                        Làm mới
                    </button>
                </div>
            </div>

            {ordersError && (
                <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">
                    {ordersError}
                </div>
            )}

            <div className="overflow-visible rounded-2xl border border-outline-variant/30 bg-surface shadow-sm">
                <AdminToolbar className="rounded-2xl border-b-0">
                    <AdminSearch value={searchTerm} onChange={value => { setSearchTerm(value); setPage(0); }}
                                 onSubmit={value => { setSearchTerm(value); setPage(0); }} suggestions={searchSuggestions}
                                 placeholder={showNhanhControls ? 'Tìm mã đơn, khách hàng, điện thoại hoặc mã Nhanh.vn...' : 'Tìm mã đơn, khách hàng hoặc điện thoại...'}
                                 ariaLabel="Tìm kiếm đơn hàng quản trị"/>
                    <AdminFilterGroup>
                    <AdminFilterSelect value={statusFilter} onChange={value => { setStatusFilter(value); setPage(0); }} label="Lọc theo trạng thái đơn hàng">
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="NEW">Mới</option>
                        <option value="WAITING_DEPOSIT">Chờ cọc</option>
                        <option value="DEPOSIT_PAID">Đã cọc</option>
                        <option value="READY_FOR_FINAL_PAYMENT">Chờ thanh toán cuối</option>
                        <option value="PROCESSING">Đang xử lý</option>
                        <option value="SHIPPED">Đang giao</option>
                        <option value="DELIVERED">Đã giao</option>
                        <option value="CANCELLED">Đã hủy</option>
                    </AdminFilterSelect>
                    {showNhanhControls && (
                        <AdminFilterSelect value={syncFilter} onChange={value => { setSyncFilter(value); setPage(0); }} label="Lọc theo trạng thái đồng bộ Nhanh.vn">
                            <option value="ALL">Tất cả đồng bộ</option>
                            <option value="SYNCED">Đã đồng bộ</option>
                            <option value="PENDING">Đang chờ</option>
                            <option value="FAILED">Thất bại</option>
                            <option value="NEED_RECONCILE">Cần đối soát</option>
                            <option value="DEAD">Đã dừng retry</option>
                        </AdminFilterSelect>
                    )}
                    <AdminFilterSelect value={sortBy} onChange={value => {
                            setSortBy(value);
                            setPage(0);
                        }} label="Sắp xếp danh sách đơn hàng">
                        <option value="createdAt">Sắp xếp: Ngày tạo</option>
                        <option value="totalAmount">Sắp xếp: Tổng tiền</option>
                        <option value="status">Sắp xếp: Trạng thái</option>
                    </AdminFilterSelect>
                    <AdminFilterSelect value={sortDirection} onChange={value => {
                            setSortDirection(value as 'ASC' | 'DESC');
                            setPage(0);
                        }} label="Chiều sắp xếp danh sách đơn hàng">
                        <option value="DESC">Giảm dần</option>
                        <option value="ASC">Tăng dần</option>
                    </AdminFilterSelect>
                    <AdminFilterReset disabled={!searchTerm && statusFilter === 'ALL' && syncFilter === 'ALL' && sortBy === 'createdAt' && sortDirection === 'DESC'} onClick={() => {
                        setSearchTerm(''); setStatusFilter('ALL'); setSyncFilter('ALL'); setSortBy('createdAt'); setSortDirection('DESC'); setPage(0);
                    }}/>
                    </AdminFilterGroup>
                </AdminToolbar>
            </div>

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                        <thead className="bg-surface-variant font-bold text-on-surface-variant">
                            <tr>
                                <th className="px-4 py-4 text-center">
                                    <input
                                        type="checkbox"
                                        checked={filteredOrders.length > 0 && selectedOrderIds.length === filteredOrders.length}
                                        onChange={(e) => {
                                            if (e.target.checked) {
                                                setSelectedOrderIds(filteredOrders.map(o => o.id));
                                            } else {
                                                setSelectedOrderIds([]);
                                            }
                                        }}
                                        className="h-4 w-4 rounded border-outline-variant/40 accent-primary"
                                        aria-label="Chọn tất cả đơn hàng"
                                    />
                                </th>
                                <th className="px-6 py-4">Mã đơn</th>
                                <th className="px-6 py-4">Khách hàng</th>
                                <th className="px-6 py-4">Ngày tạo</th>
                                <th className="px-6 py-4 text-right">Tổng tiền</th>
                                {showNhanhControls && <th className="px-6 py-4 text-center">Đồng bộ</th>}
                                <th className="px-6 py-4 text-center">Trạng thái</th>
                                <th className="px-6 py-4 text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {isOrdersLoading ? (
                                <tr>
                                    <td colSpan={showNhanhControls ? 8 : 7} className="px-6 py-16 text-center">
                                        <Loader2 className="mx-auto mb-2 h-8 w-8 animate-spin text-primary" />
                                        <p className="text-[10px] font-bold text-outline">
                                            Đang tải danh sách đơn hàng...
                                        </p>
                                    </td>
                                </tr>
                            ) : filteredOrders.map((order) => (
                                <tr
                                    key={order.id}
                                    className="border-b border-outline-variant/20 hover:bg-surface-container-lowest/50"
                                >
                                    <td className="px-4 py-4 text-center">
                                        <input
                                            type="checkbox"
                                            checked={selectedOrderIds.includes(order.id)}
                                            onChange={(e) => {
                                                if (e.target.checked) {
                                                    setSelectedOrderIds(prev => [...prev, order.id]);
                                                } else {
                                                    setSelectedOrderIds(prev => prev.filter(id => id !== order.id));
                                                }
                                            }}
                                            className="h-4 w-4 rounded border-outline-variant/40 accent-primary"
                                            aria-label={`Chọn đơn hàng ${order.orderCode || order.id}`}
                                        />
                                    </td>
                                    <td className="px-6 py-4 font-bold">
                                        <p className="font-black text-primary">
                                            #{order.orderCode || order.id}
                                        </p>
                                        {hasNhanhHistory(order) && (order.nhanhOrderCode || order.nhanhOrderId) && (
                                            <span className="mt-0.5 block text-[10px] font-semibold text-outline">
                                                Nhanh{showNhanhControls ? '' : ' (lịch sử)'}: {order.nhanhOrderCode || order.nhanhOrderId}
                                            </span>
                                        )}
                                    </td>
                                    <td className="px-6 py-4">
                                        <p className="font-bold text-on-surface">
                                            {order.customerName || 'Chưa cập nhật'}
                                        </p>
                                        <p className="text-[10px] text-outline">
                                            {order.customerMobile || 'Chưa cập nhật'}
                                        </p>
                                    </td>
                                    <td className="px-6 py-4 font-semibold text-on-surface-variant">
                                        {order.createdAt
                                            ? new Date(order.createdAt).toLocaleString('vi-VN')
                                            : 'N/A'}
                                    </td>
                                    <td className="px-6 py-4 text-right text-sm font-black text-on-surface">
                                        {formatCurrency(order.totalAmount ?? 0)}
                                        {(order.shippingFee ?? 0) > 0 && (
                                            <span className="block text-[10px] font-semibold text-outline">
                                                Ship: {formatCurrency(order.shippingFee ?? 0)}
                                            </span>
                                        )}
                                    </td>
                                    {showNhanhControls && (
                                        <td className="px-6 py-4 text-center">
                                            <span className={`inline-block rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-wider ${getSyncStatusColor(order.syncStatus)}`}>
                                                {getSyncStatusText(order.syncStatus)}
                                            </span>
                                            <span className="mt-1 block text-[10px] font-semibold text-outline">
                                                {order.nhanhSyncStage || 'NONE'}
                                            </span>
                                        </td>
                                    )}
                                    <td className="px-6 py-4 text-center">
                                        <span className={`inline-block rounded-full px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${getStatusColor(order.status)}`}>
                                            {getStatusText(order.status)}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                        <div className="flex items-center justify-center gap-2">
                                            {!showNhanhControls && getNextAdminOrderStatus(order.status) && (() => {
                                                const transition = getNextAdminOrderStatus(order.status)!;
                                                const isUpdatingStatus = updatingOrderStatusIds.includes(order.id);
                                                return (
                                                    <button
                                                        type="button"
                                                        onClick={() => void handleAdvanceOrderStatus(order.id, order.orderCode, transition.label, transition.status)}
                                                        disabled={isUpdatingStatus}
                                                        className="inline-flex items-center gap-1 rounded-lg border border-primary/25 px-2 py-1 text-[10px] font-black text-primary transition-colors hover:bg-primary/10 disabled:cursor-not-allowed disabled:opacity-50"
                                                        aria-label={`Chuyển đơn ${order.orderCode || order.id} sang ${transition.label}`}
                                                    >
                                                        <ArrowRight className={`h-3.5 w-3.5 ${isUpdatingStatus ? 'animate-pulse' : ''}`} />
                                                        {transition.label}
                                                    </button>
                                                );
                                            })()}
                                            {showNhanhControls && canRetrySync(order.syncStatus) && (
                                                <button
                                                    type="button"
                                                    onClick={() => void handleRetrySync(order.id)}
                                                    disabled={retryingOrderIds.includes(order.id)}
                                                    className="p-1 text-primary transition-colors hover:text-primary-container disabled:opacity-50"
                                                    aria-label={`Retry đồng bộ đơn ${order.orderCode || order.id}`}
                                                >
                                                    <RefreshCw className={`h-5 w-5 ${retryingOrderIds.includes(order.id) ? 'animate-spin' : ''}`} />
                                                </button>
                                            )}
                                            <Link
                                                to={`/admin/orders/${order.id}`}
                                                className="inline-block p-1 text-secondary transition-colors hover:text-primary"
                                                aria-label={`Xem đơn ${order.orderCode || order.id}`}
                                            >
                                                <Eye className="h-5 w-5" />
                                            </Link>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!isOrdersLoading && filteredOrders.length === 0 && (
                                <tr>
                                    <td colSpan={showNhanhControls ? 8 : 7} className="px-6 py-16 text-center font-bold text-outline">
                                        <FileText className="mx-auto mb-2 h-8 w-8 text-outline/30" />
                                        Không tìm thấy đơn hàng nào.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>

                {ordersPage.totalPages > 1 && (
                    <div className="flex items-center justify-between border-t border-outline-variant/20 px-6 py-4">
                        <span className="text-xs font-bold text-outline">
                            Trang {ordersPage.pageNumber + 1}/{ordersPage.totalPages}
                        </span>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={() => setPage((current) => Math.max(0, current - 1))}
                                disabled={!ordersPage.hasPrevious || isOrdersLoading}
                                className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                aria-label="Trang trước"
                            >
                                <ChevronLeft className="h-4 w-4" />
                            </button>
                            <button
                                type="button"
                                onClick={() => setPage((current) => current + 1)}
                                disabled={!ordersPage.hasNext || isOrdersLoading}
                                className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                aria-label="Trang sau"
                            >
                                <ChevronRight className="h-4 w-4" />
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
