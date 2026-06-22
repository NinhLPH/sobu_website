import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Eye, FileText, Loader2, Search } from 'lucide-react';
import { useRequestStore } from '../../store/useRequestStore';
import { RequestStatusBadge } from '../../components/request/RequestWorkflow';
import { REQUEST_STATUS_VIEWS } from '../../utils/request-workflow';
import { formatCurrency } from '../../utils/format';

const typeLabels: Record<string, string> = {
    NORMAL: 'Thông thường',
    CUSTOM: 'Custom theo yêu cầu',
    FINDING: 'Tìm đồ hộ',
    PREORDER: 'Đặt trước'
};

export default function AdminRequests() {
    const {
        adminRequests,
        adminRequestsPage,
        fetchAdminRequests,
        isLoading,
        error
    } = useRequestStore();
    const [page, setPage] = useState(0);
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('ALL');
    const [type, setType] = useState('ALL');

    useEffect(() => {
        void fetchAdminRequests({ page, size: 20 });
    }, [fetchAdminRequests, page]);

    const filteredRequests = useMemo(() => {
        const keyword = search.trim().toLowerCase();
        return adminRequests.filter((request) => {
            const matchesKeyword = !keyword || [
                request.id,
                request.requestCode,
                request.customerPhone,
                request.items?.map((item) => item.name).join(' ')
            ].some((value) => String(value || '').toLowerCase().includes(keyword));
            return matchesKeyword &&
                (status === 'ALL' || request.status === status) &&
                (type === 'ALL' || request.type === type);
        });
    }, [adminRequests, search, status, type]);

    return (
        <main className="space-y-6 pb-10 pt-6">
            <header className="flex flex-col justify-between gap-3 lg:flex-row lg:items-end">
                <div>
                    <p className="text-[10px] font-black uppercase tracking-[0.22em] text-primary">Request workflow</p>
                    <h1 className="mt-1 text-2xl font-black uppercase tracking-tight text-on-surface">Quản lý yêu cầu</h1>
                    <p className="mt-2 max-w-2xl text-xs font-semibold text-outline">
                        Theo dõi yêu cầu custom, tìm hàng và đặt trước theo đúng bước cần xử lý.
                    </p>
                </div>
            </header>

            <section className="rounded-2xl border border-outline-variant/30 bg-white p-4 shadow-sm">
                <div className="grid gap-3 lg:grid-cols-[minmax(240px,1fr)_220px_200px]">
                    <label className="flex items-center gap-2 rounded-xl bg-surface-container px-4 py-2.5">
                        <Search className="h-4 w-4 text-outline" />
                        <input
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            className="w-full border-0 bg-transparent text-xs font-semibold outline-none"
                            placeholder="Mã yêu cầu, SĐT hoặc sản phẩm..."
                            aria-label="Tìm trong trang hiện tại"
                        />
                    </label>
                    <select value={status} onChange={(event) => setStatus(event.target.value)} className="rounded-xl border-0 bg-surface-container px-4 py-2.5 text-xs font-bold">
                        <option value="ALL">Tất cả trạng thái</option>
                        {Object.entries(REQUEST_STATUS_VIEWS).map(([value, view]) => (
                            <option key={value} value={value}>{view.label}</option>
                        ))}
                    </select>
                    <select value={type} onChange={(event) => setType(event.target.value)} className="rounded-xl border-0 bg-surface-container px-4 py-2.5 text-xs font-bold">
                        <option value="ALL">Tất cả loại yêu cầu</option>
                        <option value="CUSTOM">Custom theo yêu cầu</option>
                        <option value="FINDING">Tìm đồ hộ</option>
                        <option value="PREORDER">Đặt trước</option>
                    </select>
                </div>
            </section>

            {error && <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">{error}</div>}

            <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="space-y-3 p-3 md:hidden">
                    {filteredRequests.map((request) => (
                        <article key={request.id} className="rounded-xl border border-outline-variant/30 p-4">
                            <div className="flex items-start justify-between gap-3">
                                <div>
                                    <p className="text-sm font-black text-primary">#{request.requestCode || request.id}</p>
                                    <p className="mt-1 text-xs font-bold text-on-surface">{request.customerPhone}</p>
                                </div>
                                <RequestStatusBadge status={request.status} />
                            </div>
                            <p className="mt-3 text-xs font-bold text-on-surface">{typeLabels[request.type]}</p>
                            <p className="mt-1 line-clamp-1 text-[11px] text-outline">{request.items.map((item) => item.name).join(', ')}</p>
                            <div className="mt-4 flex items-center justify-between border-t border-surface-container pt-3">
                                <span className="text-xs font-black text-on-surface">{request.totalAmount > 0 ? formatCurrency(request.totalAmount) : 'Chờ báo giá'}</span>
                                <Link to={`/admin/requests/${request.id}`} className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-3 py-2 text-[10px] font-black uppercase text-primary">
                                    <Eye className="h-4 w-4" /> Xử lý
                                </Link>
                            </div>
                        </article>
                    ))}
                </div>

                <div className="hidden overflow-x-auto md:block">
                    <table className="w-full min-w-[980px] text-left text-xs">
                        <thead className="bg-surface-container text-[10px] font-black uppercase tracking-wide text-on-surface-variant">
                            <tr>
                                <th className="px-5 py-4">Mã yêu cầu</th>
                                <th className="px-5 py-4">Khách hàng</th>
                                <th className="px-5 py-4">Loại</th>
                                <th className="px-5 py-4">Bước xử lý</th>
                                <th className="px-5 py-4 text-right">Báo giá</th>
                                <th className="px-5 py-4">Cập nhật</th>
                                <th className="px-5 py-4 text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-outline-variant/20">
                            {filteredRequests.map((request) => (
                                <tr key={request.id} className="hover:bg-surface-container/40">
                                    <td className="px-5 py-4 font-black text-primary">#{request.requestCode || request.id}</td>
                                    <td className="px-5 py-4 font-bold text-on-surface">{request.customerPhone}</td>
                                    <td className="px-5 py-4">
                                        <p className="font-bold text-on-surface">{typeLabels[request.type]}</p>
                                        <p className="mt-1 max-w-[220px] truncate text-[10px] text-outline">{request.items.map((item) => item.name).join(', ')}</p>
                                    </td>
                                    <td className="px-5 py-4"><RequestStatusBadge status={request.status} /></td>
                                    <td className="px-5 py-4 text-right font-black text-on-surface">{request.totalAmount > 0 ? formatCurrency(request.totalAmount) : 'Chờ báo giá'}</td>
                                    <td className="px-5 py-4 font-semibold text-outline">{new Date(request.updatedAt).toLocaleString('vi-VN')}</td>
                                    <td className="px-5 py-4 text-center">
                                        <Link to={`/admin/requests/${request.id}`} aria-label={`Xử lý yêu cầu ${request.requestCode}`} className="inline-flex rounded-lg p-2 text-primary hover:bg-primary/10">
                                            <Eye className="h-4 w-4" />
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {(isLoading || filteredRequests.length === 0) && (
                    <div className="flex flex-col items-center justify-center border-t border-outline-variant/20 py-16 text-outline">
                        {isLoading ? <Loader2 className="mb-3 h-8 w-8 animate-spin text-primary" /> : <FileText className="mb-3 h-10 w-10 opacity-30" />}
                        <p className="text-xs font-black uppercase">{isLoading ? 'Đang tải yêu cầu...' : 'Không có yêu cầu phù hợp'}</p>
                    </div>
                )}

                {adminRequestsPage.totalPages > 1 && (
                    <div className="flex items-center justify-between border-t border-outline-variant/20 px-5 py-4">
                        <span className="text-xs font-bold text-outline">Trang {adminRequestsPage.pageNumber + 1}/{adminRequestsPage.totalPages}</span>
                        <div className="flex gap-2">
                            <button onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={!adminRequestsPage.hasPrevious || isLoading} className="rounded-lg bg-surface-container p-2 disabled:opacity-40" aria-label="Trang trước"><ChevronLeft className="h-4 w-4" /></button>
                            <button onClick={() => setPage((value) => value + 1)} disabled={!adminRequestsPage.hasNext || isLoading} className="rounded-lg bg-surface-container p-2 disabled:opacity-40" aria-label="Trang sau"><ChevronRight className="h-4 w-4" /></button>
                        </div>
                    </div>
                )}
            </section>
        </main>
    );
}
