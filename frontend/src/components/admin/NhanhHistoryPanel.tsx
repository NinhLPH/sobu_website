import {useEffect, useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import {
    AlertCircle,
    CheckCircle2,
    ChevronLeft,
    ChevronRight,
    Clock3,
    Eye,
    Loader2,
    RefreshCw
} from 'lucide-react';
import {OrderResponseDto} from '../../interface/order.model';
import {getNhanhSyncStatusStyle, getNhanhSyncStatusText} from '../../utils/order-sync';

const PAGE_SIZE = 10;

interface NhanhHistoryPanelProps {
    orders: OrderResponseDto[];
    loading: boolean;
    error: string | null;
    onRetry: () => void;
}

const formatDate = (value?: string) => value
    ? new Date(value).toLocaleString('vi-VN')
    : 'Chưa có';

export default function NhanhHistoryPanel({
    orders,
    loading,
    error,
    onRetry
}: NhanhHistoryPanelProps) {
    const [page, setPage] = useState(0);
    const totalPages = Math.ceil(orders.length / PAGE_SIZE);

    useEffect(() => {
        setPage(current => Math.min(current, Math.max(totalPages - 1, 0)));
    }, [totalPages]);

    const visibleOrders = useMemo(
        () => orders.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE),
        [orders, page]
    );

    return (
        <section className="overflow-hidden rounded-3xl border border-outline-variant/30 bg-surface shadow-sm">
            <div className="flex flex-col gap-4 border-b border-surface-container p-5 sm:p-6 md:flex-row md:items-center md:justify-between">
                <div className="flex items-start gap-3 sm:gap-4">
                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                        <Clock3 className="h-5 w-5" aria-hidden="true"/>
                    </div>
                    <div>
                        <div className="flex flex-wrap items-center gap-2">
                            <h2 className="text-sm font-black uppercase tracking-wide text-on-surface">
                                Lịch sử Nhanh.vn
                            </h2>
                            <span className="rounded-full border border-outline-variant/40 bg-surface-container px-2.5 py-1 text-[9px] font-black uppercase tracking-wide text-on-surface-variant">
                                Chỉ đọc
                            </span>
                        </div>
                        <p className="mt-1 max-w-2xl text-xs font-semibold leading-relaxed text-outline">
                            Dữ liệu của các đơn từng có hoạt động đồng bộ được giữ lại để tra cứu.
                        </p>
                    </div>
                </div>
                <button
                    type="button"
                    onClick={onRetry}
                    disabled={loading}
                    className="inline-flex min-h-10 cursor-pointer items-center justify-center gap-2 rounded-xl bg-surface-container px-4 py-2.5 text-xs font-black uppercase tracking-wide text-on-surface transition-colors hover:bg-surface-container-high focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} aria-hidden="true"/>
                    Làm mới lịch sử
                </button>
            </div>

            <div aria-live="polite" aria-atomic="true">
                {error && !loading && (
                    <div className="m-5 flex flex-col gap-3 rounded-2xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error sm:m-6 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex items-start gap-2">
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true"/>
                            <span>{error}</span>
                        </div>
                        <button
                            type="button"
                            onClick={onRetry}
                            className="min-h-9 cursor-pointer rounded-xl border border-error/30 px-3 py-2 text-[10px] font-black uppercase tracking-wide transition-colors hover:bg-error/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-error"
                        >
                            Thử lại
                        </button>
                    </div>
                )}

                {loading && (
                    <div className="flex min-h-48 flex-col items-center justify-center gap-3 px-5 py-12 text-center text-xs font-bold text-outline">
                        <Loader2 className="h-7 w-7 animate-spin text-primary" aria-hidden="true"/>
                        Đang tải lịch sử đồng bộ...
                    </div>
                )}

                {!loading && !error && orders.length === 0 && (
                    <div className="flex min-h-48 flex-col items-center justify-center gap-3 px-5 py-12 text-center">
                        <CheckCircle2 className="h-8 w-8 text-green-500" aria-hidden="true"/>
                        <div>
                            <p className="text-xs font-black uppercase tracking-wide text-on-surface">
                                Chưa có lịch sử Nhanh.vn
                            </p>
                            <p className="mt-1 text-xs font-semibold text-outline">
                                Các đơn local mới không được đưa vào danh sách này.
                            </p>
                        </div>
                    </div>
                )}
            </div>

            {!loading && !error && visibleOrders.length > 0 && (
                <>
                    <div className="space-y-3 p-4 md:hidden">
                        {visibleOrders.map(order => (
                            <article key={order.id} className="rounded-2xl border border-outline-variant/30 bg-surface-container-lowest p-4">
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <p className="text-xs font-black text-primary">#{order.orderCode || order.id}</p>
                                        <p className="mt-1 text-[10px] font-bold text-outline">
                                            Nhanh: {order.nhanhOrderCode || order.nhanhOrderId || 'Chưa có mã'}
                                        </p>
                                    </div>
                                    <span className={`rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-wide ${getNhanhSyncStatusStyle(order.syncStatus)}`}>
                                        {getNhanhSyncStatusText(order.syncStatus)}
                                    </span>
                                </div>
                                <dl className="mt-4 grid grid-cols-2 gap-3 text-[10px] font-semibold">
                                    <div>
                                        <dt className="text-outline">Milestone</dt>
                                        <dd className="mt-1 break-words font-bold text-on-surface">{order.nhanhSyncStage || 'NONE'}</dd>
                                    </div>
                                    <div>
                                        <dt className="text-outline">Lần sync cuối</dt>
                                        <dd className="mt-1 font-bold text-on-surface">{formatDate(order.lastSyncAt)}</dd>
                                    </div>
                                </dl>
                                {(order.lastSyncMessage || order.syncError) && (
                                    <p className="mt-3 rounded-xl border border-outline-variant/20 bg-surface p-3 text-[10px] font-semibold leading-relaxed text-on-surface-variant">
                                        {order.lastSyncMessage || order.syncError}
                                    </p>
                                )}
                                <Link
                                    to={`/admin/orders/${order.id}`}
                                    className="mt-4 inline-flex min-h-9 cursor-pointer items-center gap-2 rounded-xl border border-primary/20 px-3 py-2 text-[10px] font-black uppercase tracking-wide text-primary transition-colors hover:bg-primary/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                                    aria-label={`Xem chi tiết đơn ${order.orderCode || order.id}`}
                                >
                                    <Eye className="h-3.5 w-3.5" aria-hidden="true"/>
                                    Xem chi tiết
                                </Link>
                            </article>
                        ))}
                    </div>

                    <div className="hidden overflow-x-auto md:block">
                        <table className="w-full min-w-[840px] text-left text-xs">
                            <thead className="bg-surface-container/60 text-[10px] font-black uppercase tracking-wider text-outline">
                            <tr>
                                <th className="px-6 py-4">Đơn hàng</th>
                                <th className="px-4 py-4">Trạng thái</th>
                                <th className="px-4 py-4">Milestone</th>
                                <th className="px-4 py-4">Mã Nhanh</th>
                                <th className="px-4 py-4">Lần sync cuối</th>
                                <th className="px-6 py-4 text-right">Chi tiết</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-surface-container">
                            {visibleOrders.map(order => (
                                <tr key={order.id} className="transition-colors hover:bg-surface-container/20">
                                    <td className="px-6 py-4">
                                        <p className="font-black text-primary">#{order.orderCode || order.id}</p>
                                        {(order.lastSyncMessage || order.syncError) && (
                                            <span className="mt-1 block max-w-xs truncate text-[10px] font-semibold text-outline" title={order.lastSyncMessage || order.syncError}>
                                                {order.lastSyncMessage || order.syncError}
                                            </span>
                                        )}
                                    </td>
                                    <td className="px-4 py-4">
                                        <span className={`inline-block rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-wide ${getNhanhSyncStatusStyle(order.syncStatus)}`}>
                                            {getNhanhSyncStatusText(order.syncStatus)}
                                        </span>
                                    </td>
                                    <td className="px-4 py-4 font-bold text-on-surface">{order.nhanhSyncStage || 'NONE'}</td>
                                    <td className="px-4 py-4 font-semibold text-on-surface">{order.nhanhOrderCode || order.nhanhOrderId || '—'}</td>
                                    <td className="px-4 py-4 font-semibold text-outline">{formatDate(order.lastSyncAt)}</td>
                                    <td className="px-6 py-4 text-right">
                                        <Link
                                            to={`/admin/orders/${order.id}`}
                                            className="inline-flex cursor-pointer rounded-lg p-2 text-primary transition-colors hover:bg-primary/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                                            aria-label={`Xem chi tiết đơn ${order.orderCode || order.id}`}
                                        >
                                            <Eye className="h-4 w-4" aria-hidden="true"/>
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>

                    {totalPages > 1 && (
                        <div className="flex flex-col gap-3 border-t border-surface-container px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                            <span className="text-xs font-bold text-outline">
                                Trang {page + 1}/{totalPages} · {orders.length} đơn có lịch sử
                            </span>
                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => setPage(current => Math.max(current - 1, 0))}
                                    disabled={page === 0}
                                    className="cursor-pointer rounded-lg bg-surface-container p-2 text-on-surface transition-colors hover:bg-surface-container-high focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-40"
                                    aria-label="Trang lịch sử trước"
                                >
                                    <ChevronLeft className="h-4 w-4" aria-hidden="true"/>
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setPage(current => Math.min(current + 1, totalPages - 1))}
                                    disabled={page + 1 >= totalPages}
                                    className="cursor-pointer rounded-lg bg-surface-container p-2 text-on-surface transition-colors hover:bg-surface-container-high focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-40"
                                    aria-label="Trang lịch sử sau"
                                >
                                    <ChevronRight className="h-4 w-4" aria-hidden="true"/>
                                </button>
                            </div>
                        </div>
                    )}
                </>
            )}

            {!loading && !error && orders.length > 0 && totalPages <= 1 && (
                <div className="flex items-center gap-2 border-t border-surface-container px-4 py-3 text-[10px] font-bold text-outline sm:px-6">
                    <Clock3 className="h-3.5 w-3.5" aria-hidden="true"/>
                    {orders.length} đơn có lịch sử đồng bộ
                </div>
            )}
        </section>
    );
}
