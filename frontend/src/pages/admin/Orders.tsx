import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    ChevronLeft,
    ChevronRight,
    Eye,
    FileText,
    Loader2,
    RefreshCw,
    Search,
    SlidersHorizontal
} from 'lucide-react';
import { useAdminStore } from '../../store/useAdminStore';
import { formatCurrency } from '../../utils/format';
import SearchSuggestInput, {SearchSuggestion} from '../../components/common/SearchSuggestInput';

const getStatusColor = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
            return 'bg-amber-100 text-amber-800';
        case 'WAITING_DEPOSIT':
            return 'bg-yellow-100 text-yellow-800';
        case 'DEPOSIT_PAID':
            return 'bg-cyan-100 text-cyan-800';
        case 'READY_FOR_FINAL_PAYMENT':
            return 'bg-indigo-100 text-indigo-800';
        case 'PROCESSING':
            return 'bg-blue-100 text-blue-800';
        case 'SHIPPED':
            return 'bg-purple-100 text-purple-800';
        case 'DELIVERED':
            return 'bg-green-100 text-green-800';
        case 'CANCELLED':
            return 'bg-red-100 text-red-800';
        default:
            return 'bg-gray-100 text-gray-800';
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
            return 'border-green-200 bg-green-50 text-green-700';
        case 'FAILED':
            return 'border-red-200 bg-red-50 text-red-700';
        case 'NEED_RECONCILE':
            return 'border-orange-200 bg-orange-50 text-orange-700';
        case 'DEAD':
            return 'border-slate-300 bg-slate-100 text-slate-700';
        case 'PENDING':
            return 'border-amber-200 bg-amber-50 text-amber-700';
        default:
            return 'border-gray-200 bg-gray-50 text-gray-700';
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
    const {
        workflowOrders,
        fetchOrders,
        retryOrderSync,
        retryingOrderIds,
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
            const matchesSync = syncFilter === 'ALL' || order.syncStatus === syncFilter;
            return matchesSearch && matchesStatus && matchesSync;
        });
    }, [workflowOrders, searchTerm, statusFilter, syncFilter]);

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

    return (
        <div className="space-y-6 pt-6">
            <div className="flex items-center justify-between gap-4">
                <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface">
                    Quản lý đơn hàng ERP
                </h1>
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

            {ordersError && (
                <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">
                    {ordersError}
                </div>
            )}

            <div className="space-y-3 rounded-2xl border border-outline-variant/30 bg-white p-4 shadow-sm">
                <div className="relative flex items-center gap-3 rounded-xl bg-surface-container px-4 py-2.5">
                    <Search className="h-4 w-4 text-outline" />
                    <SearchSuggestInput
                        value={searchTerm}
                        onChange={setSearchTerm}
                        onSubmit={setSearchTerm}
                        suggestions={searchSuggestions}
                        placeholder="Tìm mã đơn, khách hàng, điện thoại hoặc mã Nhanh.vn..."
                        ariaLabel="Tìm kiếm đơn hàng quản trị"
                        className="w-full border-none bg-transparent text-xs font-semibold outline-none"
                    />
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-outline">
                        <SlidersHorizontal className="h-3.5 w-3.5" />
                        Bộ lọc:
                    </div>
                    <select
                        value={statusFilter}
                        onChange={(event) => setStatusFilter(event.target.value)}
                        className="rounded-lg border-none bg-surface-container px-3 py-1.5 text-xs font-bold text-on-surface outline-none"
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="NEW">Mới</option>
                        <option value="WAITING_DEPOSIT">Chờ cọc</option>
                        <option value="DEPOSIT_PAID">Đã cọc</option>
                        <option value="READY_FOR_FINAL_PAYMENT">Chờ thanh toán cuối</option>
                        <option value="PROCESSING">Đang xử lý</option>
                        <option value="SHIPPED">Đang giao</option>
                        <option value="DELIVERED">Đã giao</option>
                        <option value="CANCELLED">Đã hủy</option>
                    </select>
                    <select
                        value={syncFilter}
                        onChange={(event) => setSyncFilter(event.target.value)}
                        className="rounded-lg border-none bg-surface-container px-3 py-1.5 text-xs font-bold text-on-surface outline-none"
                    >
                        <option value="ALL">Tất cả đồng bộ</option>
                        <option value="SYNCED">Đã đồng bộ</option>
                        <option value="PENDING">Đang chờ</option>
                        <option value="FAILED">Thất bại</option>
                        <option value="NEED_RECONCILE">Cần đối soát</option>
                        <option value="DEAD">Đã dừng retry</option>
                    </select>
                    <select
                        value={sortBy}
                        onChange={(event) => {
                            setSortBy(event.target.value);
                            setPage(0);
                        }}
                        className="rounded-lg border-none bg-surface-container px-3 py-1.5 text-xs font-bold text-on-surface outline-none"
                    >
                        <option value="createdAt">Sắp xếp: Ngày tạo</option>
                        <option value="totalAmount">Sắp xếp: Tổng tiền</option>
                        <option value="status">Sắp xếp: Trạng thái</option>
                    </select>
                    <select
                        value={sortDirection}
                        onChange={(event) => {
                            setSortDirection(event.target.value as 'ASC' | 'DESC');
                            setPage(0);
                        }}
                        className="rounded-lg border-none bg-surface-container px-3 py-1.5 text-xs font-bold text-on-surface outline-none"
                    >
                        <option value="DESC">Giảm dần</option>
                        <option value="ASC">Tăng dần</option>
                    </select>
                </div>
            </div>

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                        <thead className="bg-surface-variant font-bold text-on-surface-variant">
                            <tr>
                                <th className="px-6 py-4">Mã đơn</th>
                                <th className="px-6 py-4">Khách hàng</th>
                                <th className="px-6 py-4">Ngày tạo</th>
                                <th className="px-6 py-4 text-right">Tổng tiền</th>
                                <th className="px-6 py-4 text-center">Đồng bộ</th>
                                <th className="px-6 py-4 text-center">Trạng thái</th>
                                <th className="px-6 py-4 text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {isOrdersLoading ? (
                                <tr>
                                    <td colSpan={7} className="px-6 py-16 text-center">
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
                                    <td className="px-6 py-4 font-bold">
                                        <p className="font-black text-primary">
                                            #{order.orderCode || order.id}
                                        </p>
                                        {(order.nhanhOrderCode || order.nhanhOrderId) && (
                                            <span className="mt-0.5 block text-[10px] font-semibold text-outline">
                                                Nhanh: {order.nhanhOrderCode || order.nhanhOrderId}
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
                                    <td className="px-6 py-4 text-center">
                                        <span className={`inline-block rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-wider ${getSyncStatusColor(order.syncStatus)}`}>
                                            {getSyncStatusText(order.syncStatus)}
                                        </span>
                                        <span className="mt-1 block text-[10px] font-semibold text-outline">
                                            {order.nhanhSyncStage || 'NONE'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                        <span className={`inline-block rounded-full px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${getStatusColor(order.status)}`}>
                                            {getStatusText(order.status)}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                        <div className="flex items-center justify-center gap-2">
                                            {canRetrySync(order.syncStatus) && (
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
                                    <td colSpan={7} className="px-6 py-16 text-center font-bold text-outline">
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
