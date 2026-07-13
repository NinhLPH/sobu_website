import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Eye, FileText, Loader2, Search, SlidersHorizontal } from 'lucide-react';
import { CustomerService } from '../service/custom.service';
import { CustomerOrderListItemDto, CustomerOrderQueryParams } from '../interface/order.model';
import { PageResponse } from '../interface/api-response';
import { formatCurrency } from '../utils/format';

const emptyPage: PageResponse<CustomerOrderListItemDto> = {
    content: [],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false
};

const getStatusText = (status?: string) => {
    const labels: Record<string, string> = {
        NEW: 'Mới',
        PENDING: 'Mới',
        WAITING_DEPOSIT: 'Chờ đặt cọc',
        DEPOSIT_PAID: 'Đã đặt cọc',
        READY_FOR_FINAL_PAYMENT: 'Chờ thanh toán cuối',
        PROCESSING: 'Đang xử lý',
        SHIPPED: 'Đang giao',
        DELIVERED: 'Đã giao',
        CANCELLED: 'Đã hủy'
    };
    return labels[status || ''] || status || 'Chưa cập nhật';
};

const getStatusClass = (status?: string) => {
    if (status === 'DELIVERED') return 'bg-green-100 text-green-800';
    if (status === 'CANCELLED') return 'bg-red-100 text-red-800';
    if (status === 'SHIPPED') return 'bg-purple-100 text-purple-800';
    if (status === 'PROCESSING') return 'bg-blue-100 text-blue-800';
    return 'bg-amber-100 text-amber-800';
};

const getPaymentStatusText = (status?: string) => {
    const labels: Record<string, string> = {
        PENDING: 'Chờ thanh toán',
        PAID: 'Đã thanh toán',
        FAILED: 'Thanh toán lỗi',
        CANCELLED: 'Đã hủy',
        EXPIRED: 'Đã hết hạn',
        REFUNDED: 'Đã hoàn tiền'
    };
    return labels[status || ''] || status || 'Chưa cập nhật';
};

const formatDate = (value?: string) => value
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
    : 'Chưa cập nhật';

export default function MyOrders() {
    const navigate = useNavigate();
    const [ordersPage, setOrdersPage] = useState<PageResponse<CustomerOrderListItemDto>>(emptyPage);
    const [page, setPage] = useState(0);
    const [query, setQuery] = useState('');
    const [status, setStatus] = useState<CustomerOrderQueryParams['status']>('ALL');
    const [createdFrom, setCreatedFrom] = useState('');
    const [createdTo, setCreatedTo] = useState('');
    const [sortBy, setSortBy] = useState<CustomerOrderQueryParams['sortBy']>('createdAt');
    const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('DESC');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [lookupReference, setLookupReference] = useState('');
    const [lookupType, setLookupType] = useState<'internal' | 'nhanh'>('internal');

    useEffect(() => {
        let cancelled = false;
        const params: CustomerOrderQueryParams = {
            page,
            size: 10,
            query: query.trim() || undefined,
            status: status === 'ALL' ? undefined : status,
            createdFrom: createdFrom || undefined,
            createdTo: createdTo || undefined,
            sortBy,
            sortDirection
        };

        setIsLoading(true);
        setError(null);
        void CustomerService.getMyOrders(params)
            .then((response) => {
                if (!cancelled) {
                    setOrdersPage(response.data || emptyPage);
                }
            })
            .catch((requestError: any) => {
                if (!cancelled) {
                    setError(requestError?.response?.data?.message || requestError?.message || 'Không thể tải danh sách đơn hàng.');
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setIsLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [page, query, status, createdFrom, createdTo, sortBy, sortDirection]);

    const resetPage = () => setPage(0);

    const handleLookup = (event: FormEvent) => {
        event.preventDefault();
        const reference = lookupReference.trim();
        if (!reference) {
            return;
        }
        if (lookupType === 'nhanh') {
            navigate(`/orders/lookup?nhanhOrderId=${encodeURIComponent(reference)}`);
            return;
        }
        navigate(`/orders/${encodeURIComponent(reference)}`);
    };

    return (
        <main className="mx-auto w-full min-w-0 max-w-7xl bg-surface px-4 pb-24 pt-28 sm:px-6 sm:pt-32">
            <nav className="mb-6 flex items-center gap-2 text-xs font-bold text-on-surface-variant">
                <Link to="/" className="transition-colors hover:text-primary">Trang chủ</Link>
                <span>/</span>
                <span className="text-primary">Đơn hàng của tôi</span>
            </nav>

            <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
                <div>
                    <h1 className="text-3xl font-black uppercase tracking-tight text-on-surface">Đơn hàng của tôi</h1>
                    <p className="mt-1 text-xs font-semibold text-outline">Theo dõi, thanh toán và quản lý các đơn hàng thuộc tài khoản của bạn.</p>
                </div>
            </div>

            <form onSubmit={handleLookup} className="mb-5 rounded-2xl border border-outline-variant/30 bg-white p-4 shadow-sm">
                <div className="mb-3 flex gap-2 text-xs font-black">
                    <button type="button" onClick={() => setLookupType('internal')} className={`rounded-lg px-3 py-2 ${lookupType === 'internal' ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>Mã đơn SOBU</button>
                    <button type="button" onClick={() => setLookupType('nhanh')} className={`rounded-lg px-3 py-2 ${lookupType === 'nhanh' ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>Nhanh ID / code</button>
                </div>
                <div className="flex flex-col gap-2 sm:flex-row">
                    <label className="sr-only" htmlFor="my-order-lookup">Tra cứu theo mã đơn</label>
                    <input id="my-order-lookup" value={lookupReference} onChange={(event) => setLookupReference(event.target.value)} placeholder="Nhập mã đơn hàng" className="min-w-0 flex-1 rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary/20" />
                    <button type="submit" className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-black text-on-primary"><Search className="h-4 w-4" />Tra cứu</button>
                </div>
            </form>

            <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="border-b border-outline-variant/20 p-4">
                    <div className="mb-3 flex items-center gap-2 text-xs font-black text-outline"><SlidersHorizontal className="h-4 w-4" />Lọc đơn hàng</div>
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-5">
                        <input value={query} onChange={(event) => { setQuery(event.target.value); resetPage(); }} placeholder="Tìm mã đơn" className="rounded-lg bg-surface-container px-3 py-2 text-xs font-semibold outline-none" />
                        <select aria-label="Lọc trạng thái đơn hàng" value={status} onChange={(event) => { setStatus(event.target.value as CustomerOrderQueryParams['status']); resetPage(); }} className="rounded-lg bg-surface-container px-3 py-2 text-xs font-bold outline-none">
                            <option value="ALL">Tất cả trạng thái</option><option value="NEW">Mới</option><option value="WAITING_DEPOSIT">Chờ đặt cọc</option><option value="DEPOSIT_PAID">Đã đặt cọc</option><option value="READY_FOR_FINAL_PAYMENT">Chờ thanh toán cuối</option><option value="PROCESSING">Đang xử lý</option><option value="SHIPPED">Đang giao</option><option value="DELIVERED">Đã giao</option><option value="CANCELLED">Đã hủy</option>
                        </select>
                        <input aria-label="Từ ngày tạo" type="date" value={createdFrom} onChange={(event) => { setCreatedFrom(event.target.value); resetPage(); }} className="rounded-lg bg-surface-container px-3 py-2 text-xs font-bold outline-none" />
                        <input aria-label="Đến ngày tạo" type="date" min={createdFrom || undefined} value={createdTo} onChange={(event) => { setCreatedTo(event.target.value); resetPage(); }} className="rounded-lg bg-surface-container px-3 py-2 text-xs font-bold outline-none" />
                        <select aria-label="Sắp xếp đơn hàng" value={`${sortBy}-${sortDirection}`} onChange={(event) => { const [nextSortBy, nextDirection] = event.target.value.split('-') as [CustomerOrderQueryParams['sortBy'], 'ASC' | 'DESC']; setSortBy(nextSortBy); setSortDirection(nextDirection); resetPage(); }} className="rounded-lg bg-surface-container px-3 py-2 text-xs font-bold outline-none">
                            <option value="createdAt-DESC">Ngày tạo: mới nhất</option><option value="createdAt-ASC">Ngày tạo: cũ nhất</option><option value="totalAmount-DESC">Tổng tiền: cao nhất</option><option value="totalAmount-ASC">Tổng tiền: thấp nhất</option><option value="status-ASC">Trạng thái</option>
                        </select>
                    </div>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full min-w-[760px] text-left text-xs">
                        <thead className="bg-surface-variant font-bold text-on-surface-variant"><tr><th className="px-5 py-4">Mã đơn</th><th className="px-5 py-4">Ngày tạo</th><th className="px-5 py-4 text-right">Tổng tiền</th><th className="px-5 py-4">Thanh toán</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4 text-center">Chi tiết</th></tr></thead>
                        <tbody>
                            {isLoading ? <tr><td colSpan={6} className="px-5 py-16 text-center"><Loader2 className="mx-auto h-7 w-7 animate-spin text-primary" /></td></tr> : ordersPage.content.map((order) => <tr key={order.id} className="border-t border-outline-variant/20"><td className="px-5 py-4 font-black text-primary">#{order.orderCode || order.id}</td><td className="px-5 py-4 font-semibold text-on-surface-variant">{formatDate(order.createdAt)}</td><td className="px-5 py-4 text-right font-black">{formatCurrency(Number(order.totalAmount || 0))}</td><td className="px-5 py-4 font-semibold">{getPaymentStatusText(order.paymentStatus)}</td><td className="px-5 py-4"><span className={`rounded-full px-2 py-1 text-[10px] font-black ${getStatusClass(order.status)}`}>{getStatusText(order.status)}</span></td><td className="px-5 py-4 text-center"><Link aria-label={`Xem chi tiết đơn ${order.orderCode || order.id}`} to={`/orders/${order.id}`} className="inline-flex rounded-lg p-2 text-primary hover:bg-primary/10"><Eye className="h-4 w-4" /></Link></td></tr>)}
                            {!isLoading && ordersPage.content.length === 0 && <tr><td colSpan={6} className="px-5 py-16 text-center text-outline"><FileText className="mx-auto mb-2 h-8 w-8 opacity-40" />Chưa có đơn hàng phù hợp.</td></tr>}
                        </tbody>
                    </table>
                </div>
                {error && <div role="alert" className="border-t border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">{error}</div>}
                {ordersPage.totalPages > 1 && <div className="flex items-center justify-between border-t border-outline-variant/20 px-5 py-4"><span className="text-xs font-bold text-outline">Trang {ordersPage.pageNumber + 1}/{ordersPage.totalPages}</span><div className="flex gap-2"><button type="button" aria-label="Trang trước" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={!ordersPage.hasPrevious || isLoading} className="rounded-lg bg-surface-container p-2 disabled:opacity-40"><ChevronLeft className="h-4 w-4" /></button><button type="button" aria-label="Trang sau" onClick={() => setPage((current) => current + 1)} disabled={!ordersPage.hasNext || isLoading} className="rounded-lg bg-surface-container p-2 disabled:opacity-40"><ChevronRight className="h-4 w-4" /></button></div></div>}
            </section>
        </main>
    );
}
