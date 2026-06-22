import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    Calendar,
    ChevronLeft,
    ChevronRight,
    Eye,
    FileText,
    Loader2,
    Plus
} from 'lucide-react';
import { useRequestStore } from '../store/useRequestStore';
import { formatCurrency } from '../utils/format';
import { RequestType } from '../enum/union-types';
import { ToastService } from '../service/toast.service';
import { RequestStatusBadge } from '../components/request/RequestWorkflow';

const typeLabels: Record<RequestType, string> = {
    NORMAL: 'Thông thường',
    PREORDER: 'Pre-order',
    FINDING: 'Tìm hàng',
    CUSTOM: 'Custom'
};

export default function MyRequests() {
    const {
        myRequests,
        myRequestsPage,
        fetchMyRequests,
        isLoading,
        error
    } = useRequestStore();
    const [page, setPage] = useState(0);

    useEffect(() => {
        fetchMyRequests({ page, size: 10 });
    }, [fetchMyRequests, page]);

    useEffect(() => {
        if (error) {
            ToastService.error(error);
        }
    }, [error]);

    return (
        <main className="w-full min-w-0 bg-surface px-4 pb-24 pt-28 sm:px-6 sm:pt-32">
            <nav className="flex items-center gap-2 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5" />
                <span className="text-primary">Yêu cầu của tôi</span>
            </nav>

            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                <div>
                    <h1 className="text-3xl font-black tracking-tight text-on-surface uppercase">
                        Danh sách yêu cầu
                    </h1>
                    <p className="mt-2 text-xs font-bold text-outline">
                        Theo dõi yêu cầu tìm hàng, pre-order và custom của bạn.
                    </p>
                </div>
                <Link
                    to="/requests/new"
                    className="flex items-center gap-2 px-6 py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl text-xs font-black uppercase tracking-widest shadow-sm"
                >
                    <Plus className="w-4 h-4" />
                    Gửi yêu cầu mới
                </Link>
            </div>

            {error && (
                <div className="mb-6 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                    {error}
                </div>
            )}

            <div className="overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm">
                <div className="space-y-3 p-3 md:hidden">
                    {isLoading && (
                        <div className="py-16 text-center">
                            <Loader2 className="mx-auto mb-3 h-8 w-8 animate-spin text-primary"/>
                            <span className="text-xs font-bold text-outline">Đang tải yêu cầu...</span>
                        </div>
                    )}
                    {!isLoading && myRequests.map((request) => (
                        <article key={request.id} className="rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4 shadow-sm">
                            <div className="flex items-start justify-between gap-3">
                                <div className="min-w-0">
                                    <p className="font-black text-primary">#{request.requestCode || request.id}</p>
                                    <p className="mt-1 text-xs font-bold text-on-surface">{typeLabels[request.type]}</p>
                                </div>
                                <RequestStatusBadge status={request.status} />
                            </div>
                            <p className="mt-4 line-clamp-2 text-xs font-bold text-on-surface">
                                {request.items?.map((item) => item.name).join(', ') || 'Yêu cầu dịch vụ'}
                            </p>
                            <div className="mt-4 flex items-end justify-between gap-3 border-t border-surface-container pt-3">
                                <div className="space-y-1 text-[10px] font-semibold text-outline">
                                    <span className="flex items-center gap-1.5"><Calendar className="h-3.5 w-3.5"/> {new Date(request.createdAt).toLocaleDateString('vi-VN')}</span>
                                    <p className="text-xs font-black text-on-surface">
                                        {request.totalAmount > 0 ? formatCurrency(request.totalAmount) : 'Chờ báo giá'}
                                    </p>
                                </div>
                                <Link
                                    to={`/requests/${request.id}`}
                                    className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-3 py-2 text-[10px] font-black uppercase text-primary"
                                    aria-label={`Xem yêu cầu ${request.requestCode}`}
                                >
                                    <Eye className="h-4 w-4"/> Chi tiết
                                </Link>
                            </div>
                        </article>
                    ))}
                    {!isLoading && myRequests.length === 0 && (
                        <div className="py-16 text-center">
                            <FileText className="mx-auto mb-3 h-10 w-10 text-outline/30"/>
                            <p className="font-black uppercase text-on-surface">Chưa có yêu cầu nào</p>
                            <p className="mt-1 text-xs text-outline">Tạo yêu cầu mới để SOBU hỗ trợ bạn.</p>
                        </div>
                    )}
                </div>

                <div className="hidden overflow-x-auto md:block">
                    <table className="w-full min-w-[820px] text-left text-xs">
                        <thead className="bg-surface-container text-on-surface-variant">
                            <tr>
                                <th className="px-6 py-4">Mã yêu cầu</th>
                                <th className="px-6 py-4">Loại</th>
                                <th className="px-6 py-4">Sản phẩm</th>
                                <th className="px-6 py-4">Ngày tạo</th>
                                <th className="px-6 py-4 text-right">Báo giá</th>
                                <th className="px-6 py-4 text-center">Trạng thái</th>
                                <th className="px-6 py-4 text-center">Chi tiết</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-outline-variant/20">
                            {isLoading ? (
                                <tr>
                                    <td colSpan={7} className="px-6 py-20 text-center">
                                        <Loader2 className="mx-auto mb-3 h-8 w-8 animate-spin text-primary" />
                                        <span className="font-bold text-outline">Đang tải yêu cầu...</span>
                                    </td>
                                </tr>
                            ) : myRequests.map((request) => (
                                <tr key={request.id} className="hover:bg-surface-container/40">
                                    <td className="px-6 py-4 font-black text-primary">
                                        #{request.requestCode || request.id}
                                    </td>
                                    <td className="px-6 py-4 font-bold text-on-surface">
                                        {typeLabels[request.type]}
                                    </td>
                                    <td className="max-w-xs px-6 py-4 text-on-surface">
                                        <p className="truncate font-bold">
                                            {request.items?.map((item) => item.name).join(', ') || 'Yêu cầu dịch vụ'}
                                        </p>
                                        <p className="mt-1 text-[10px] text-outline">
                                            {request.items?.length || 0} sản phẩm
                                        </p>
                                    </td>
                                    <td className="px-6 py-4 font-semibold text-on-surface-variant">
                                        <span className="flex items-center gap-1.5">
                                            <Calendar className="h-3.5 w-3.5" />
                                            {new Date(request.createdAt).toLocaleDateString('vi-VN')}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-right font-black text-on-surface">
                                        {request.totalAmount > 0
                                            ? formatCurrency(request.totalAmount)
                                            : 'Chờ báo giá'}
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                        <RequestStatusBadge status={request.status} />
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                        <Link
                                            to={`/requests/${request.id}`}
                                            className="inline-flex rounded-lg p-2 text-primary hover:bg-primary/10"
                                            aria-label={`Xem yêu cầu ${request.requestCode}`}
                                        >
                                            <Eye className="h-4 w-4" />
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                            {!isLoading && myRequests.length === 0 && (
                                <tr>
                                    <td colSpan={7} className="px-6 py-20 text-center">
                                        <FileText className="mx-auto mb-3 h-10 w-10 text-outline/30" />
                                        <p className="font-black uppercase text-on-surface">Chưa có yêu cầu nào</p>
                                        <p className="mt-1 text-outline">Tạo yêu cầu mới để SOBU hỗ trợ bạn.</p>
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>

                {myRequestsPage.totalPages > 1 && (
                    <div className="flex items-center justify-between border-t border-outline-variant/20 px-6 py-4">
                        <span className="text-xs font-bold text-outline">
                            Trang {myRequestsPage.pageNumber + 1}/{myRequestsPage.totalPages}
                        </span>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setPage((current) => Math.max(0, current - 1))}
                                disabled={!myRequestsPage.hasPrevious || isLoading}
                                className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                aria-label="Trang trước"
                            >
                                <ChevronLeft className="h-4 w-4" />
                            </button>
                            <button
                                onClick={() => setPage((current) => current + 1)}
                                disabled={!myRequestsPage.hasNext || isLoading}
                                className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                aria-label="Trang sau"
                            >
                                <ChevronRight className="h-4 w-4" />
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </main>
    );
}
