import { FormEvent, useEffect, useState } from 'react';
import {
    CheckCircle2,
    ChevronLeft,
    ChevronRight,
    Loader2,
    MessageSquareReply,
    RefreshCw,
    Star,
    Trash2,
    XCircle
} from 'lucide-react';
import { ReviewResponseDto, ReviewStatus } from '../../interface/review.model';
import { ToastService } from '../../service/toast.service';
import { useAdminReviewStore } from '../../store/useAdminReviewStore';

const REVIEW_STATUS_FILTERS: Array<{ value: ReviewStatus | 'ALL'; label: string }> = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'PENDING', label: 'Chờ duyệt' },
    { value: 'APPROVED', label: 'Đã duyệt' },
    { value: 'REJECTED', label: 'Từ chối' }
];

const getStatusText = (status?: ReviewStatus) => {
    switch (status) {
        case 'PENDING':
            return 'Chờ duyệt';
        case 'APPROVED':
            return 'Đã duyệt';
        case 'REJECTED':
            return 'Từ chối';
        default:
            return 'Chưa rõ';
    }
};

const getStatusClassName = (status?: ReviewStatus) => {
    switch (status) {
        case 'PENDING':
            return 'bg-amber-100 text-amber-800';
        case 'APPROVED':
            return 'bg-green-100 text-green-800';
        case 'REJECTED':
            return 'bg-red-100 text-red-800';
        default:
            return 'bg-gray-100 text-gray-800';
    }
};

const formatDate = (value?: string) => {
    if (!value) return 'N/A';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return 'N/A';
    return parsed.toLocaleString('vi-VN');
};

const RatingStars = ({ rating }: { rating: number }) => (
    <div className="flex items-center gap-0.5" aria-label={`${rating}/5 sao`}>
        {Array.from({ length: 5 }).map((_, index) => (
            <Star
                key={index}
                className={`h-3.5 w-3.5 ${
                    index < rating ? 'fill-[#FFB800] text-[#FFB800]' : 'text-outline-variant'
                }`}
            />
        ))}
    </div>
);

export default function AdminReviews() {
    const {
        reviews,
        reviewsPage,
        activeStatus,
        isLoading,
        error,
        actionReviewIds,
        setActiveStatus,
        fetchReviews,
        updateReviewStatus,
        replyToReview,
        deleteReview
    } = useAdminReviewStore();
    const [page, setPage] = useState(0);
    const [replyingReview, setReplyingReview] = useState<ReviewResponseDto | null>(null);
    const [replyText, setReplyText] = useState('');
    const [replyError, setReplyError] = useState<string | null>(null);

    useEffect(() => {
        void fetchReviews({
            status: activeStatus === 'ALL' ? undefined : activeStatus,
            page,
            size: 20,
            sortBy: 'createdAt',
            sortDirection: 'DESC'
        });
    }, [activeStatus, fetchReviews, page]);

    const refresh = () => {
        void fetchReviews({
            status: activeStatus === 'ALL' ? undefined : activeStatus,
            page,
            size: 20,
            sortBy: 'createdAt',
            sortDirection: 'DESC'
        });
    };

    const handleStatusFilterChange = (status: ReviewStatus | 'ALL') => {
        setActiveStatus(status);
        setPage(0);
    };

    const handleStatusAction = async (
        reviewId: number,
        status: Extract<ReviewStatus, 'APPROVED' | 'REJECTED'>
    ) => {
        try {
            await updateReviewStatus(reviewId, status);
            ToastService.success(status === 'APPROVED' ? 'Đã duyệt đánh giá.' : 'Đã từ chối đánh giá.');
            refresh();
        } catch {
            // The store exposes the backend error above the table.
        }
    };

    const handleDelete = async (review: ReviewResponseDto) => {
        const confirmed = window.confirm(`Xóa vĩnh viễn đánh giá #${review.id}?`);
        if (!confirmed) return;

        try {
            await deleteReview(review.id);
            ToastService.success('Đã xóa đánh giá.');
            refresh();
        } catch {
            // The store exposes the backend error above the table.
        }
    };

    const openReplyModal = (review: ReviewResponseDto) => {
        setReplyingReview(review);
        setReplyText(review.adminReply || '');
        setReplyError(null);
    };

    const closeReplyModal = () => {
        setReplyingReview(null);
        setReplyText('');
        setReplyError(null);
    };

    const handleReplySubmit = async (event: FormEvent) => {
        event.preventDefault();
        if (!replyingReview) return;
        if (!replyText.trim()) {
            setReplyError('Vui lòng nhập nội dung phản hồi.');
            return;
        }

        try {
            await replyToReview(replyingReview.id, replyText.trim());
            ToastService.success('Đã lưu phản hồi đánh giá.');
            closeReplyModal();
        } catch {
            setReplyError('Không thể lưu phản hồi đánh giá.');
        }
    };

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                        Pre-moderation
                    </p>
                    <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface">
                        Quản lý đánh giá
                    </h1>
                </div>
                <button
                    type="button"
                    onClick={refresh}
                    disabled={isLoading}
                    className="inline-flex items-center justify-center gap-2 rounded-xl bg-surface-container px-4 py-2.5 text-xs font-black text-on-surface transition-colors hover:bg-surface-container-high disabled:opacity-50"
                >
                    <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
                    Làm mới
                </button>
            </div>

            <div className="flex flex-wrap gap-2 rounded-2xl border border-outline-variant/30 bg-white p-3 shadow-sm">
                {REVIEW_STATUS_FILTERS.map((item) => (
                    <button
                        key={item.value}
                        type="button"
                        onClick={() => handleStatusFilterChange(item.value)}
                        className={`rounded-xl px-4 py-2 text-xs font-black transition-colors ${
                            activeStatus === item.value
                                ? 'bg-primary text-on-primary'
                                : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                        }`}
                    >
                        {item.label}
                    </button>
                ))}
            </div>

            {error && (
                <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">
                    {error}
                </div>
            )}

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                        <thead className="bg-surface-variant font-bold text-on-surface-variant">
                            <tr>
                                <th className="px-5 py-4">Đánh giá</th>
                                <th className="px-5 py-4">Sản phẩm</th>
                                <th className="px-5 py-4">Khách hàng</th>
                                <th className="px-5 py-4 text-center">Rating</th>
                                <th className="px-5 py-4 text-center">Trạng thái</th>
                                <th className="px-5 py-4">Ngày tạo</th>
                                <th className="px-5 py-4 text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {isLoading ? (
                                <tr>
                                    <td colSpan={7} className="px-6 py-16 text-center">
                                        <Loader2 className="mx-auto mb-2 h-8 w-8 animate-spin text-primary" />
                                        <p className="text-[10px] font-bold text-outline">
                                            Đang tải đánh giá...
                                        </p>
                                    </td>
                                </tr>
                            ) : reviews.map((review) => {
                                const isActionLoading = actionReviewIds.includes(review.id);
                                return (
                                    <tr
                                        key={review.id}
                                        className="border-b border-outline-variant/20 align-top hover:bg-surface-container-lowest/50"
                                    >
                                        <td className="max-w-sm px-5 py-4">
                                            <p className="line-clamp-3 text-sm font-semibold leading-relaxed text-on-surface">
                                                {review.content}
                                            </p>
                                            {review.adminReply && (
                                                <p className="mt-2 rounded-lg bg-primary-container/10 px-3 py-2 text-[11px] font-semibold text-primary">
                                                    Phản hồi: {review.adminReply}
                                                </p>
                                            )}
                                        </td>
                                        <td className="px-5 py-4 font-black text-primary">
                                            #{review.productId}
                                            {review.orderId && (
                                                <span className="mt-1 block text-[10px] font-semibold text-outline">
                                                    Order #{review.orderId}
                                                </span>
                                            )}
                                        </td>
                                        <td className="px-5 py-4 font-bold text-on-surface">
                                            {review.customerName || 'Khách hàng'}
                                        </td>
                                        <td className="px-5 py-4">
                                            <div className="flex justify-center">
                                                <RatingStars rating={review.rating} />
                                            </div>
                                        </td>
                                        <td className="px-5 py-4 text-center">
                                            <span className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${getStatusClassName(review.status)}`}>
                                                {getStatusText(review.status)}
                                            </span>
                                        </td>
                                        <td className="px-5 py-4 font-semibold text-on-surface-variant">
                                            {formatDate(review.createdAt)}
                                        </td>
                                        <td className="px-5 py-4">
                                            <div className="flex items-center justify-center gap-2">
                                                <button
                                                    type="button"
                                                    onClick={() => void handleStatusAction(review.id, 'APPROVED')}
                                                    disabled={isActionLoading || review.status === 'APPROVED'}
                                                    className="rounded-lg bg-green-50 p-2 text-green-700 transition-colors hover:bg-green-100 disabled:opacity-40"
                                                    aria-label={`Duyệt đánh giá ${review.id}`}
                                                >
                                                    {isActionLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                                                </button>
                                                <button
                                                    type="button"
                                                    onClick={() => void handleStatusAction(review.id, 'REJECTED')}
                                                    disabled={isActionLoading || review.status === 'REJECTED'}
                                                    className="rounded-lg bg-red-50 p-2 text-red-700 transition-colors hover:bg-red-100 disabled:opacity-40"
                                                    aria-label={`Từ chối đánh giá ${review.id}`}
                                                >
                                                    <XCircle className="h-4 w-4" />
                                                </button>
                                                <button
                                                    type="button"
                                                    onClick={() => openReplyModal(review)}
                                                    disabled={isActionLoading}
                                                    className="rounded-lg bg-surface-container p-2 text-on-surface transition-colors hover:bg-surface-container-high disabled:opacity-40"
                                                    aria-label={`Phản hồi đánh giá ${review.id}`}
                                                >
                                                    <MessageSquareReply className="h-4 w-4" />
                                                </button>
                                                <button
                                                    type="button"
                                                    onClick={() => void handleDelete(review)}
                                                    disabled={isActionLoading}
                                                    className="rounded-lg bg-error/10 p-2 text-error transition-colors hover:bg-error/20 disabled:opacity-40"
                                                    aria-label={`Xóa đánh giá ${review.id}`}
                                                >
                                                    <Trash2 className="h-4 w-4" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                            {!isLoading && reviews.length === 0 && (
                                <tr>
                                    <td colSpan={7} className="px-6 py-16 text-center font-bold text-outline">
                                        <Star className="mx-auto mb-2 h-8 w-8 text-outline/30" />
                                        Không có đánh giá nào trong bộ lọc này.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>

                {reviewsPage.totalPages > 1 && (
                    <div className="flex items-center justify-between border-t border-outline-variant/20 px-6 py-4">
                        <span className="text-xs font-bold text-outline">
                            Trang {reviewsPage.pageNumber + 1}/{reviewsPage.totalPages}
                        </span>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={() => setPage((current) => Math.max(0, current - 1))}
                                disabled={!reviewsPage.hasPrevious || isLoading}
                                className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                aria-label="Trang trước"
                            >
                                <ChevronLeft className="h-4 w-4" />
                            </button>
                            <button
                                type="button"
                                onClick={() => setPage((current) => current + 1)}
                                disabled={!reviewsPage.hasNext || isLoading}
                                className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                aria-label="Trang sau"
                            >
                                <ChevronRight className="h-4 w-4" />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {replyingReview && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
                    <form
                        onSubmit={handleReplySubmit}
                        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
                    >
                        <h2 className="text-lg font-black text-on-surface">Phản hồi đánh giá</h2>
                        <p className="mt-1 text-xs font-semibold text-outline">
                            Review #{replyingReview.id}
                        </p>
                        <textarea
                            value={replyText}
                            onChange={(event) => {
                                setReplyText(event.target.value);
                                setReplyError(null);
                            }}
                            rows={5}
                            maxLength={1000}
                            className="mt-4 w-full resize-none rounded-xl border border-outline-variant/30 bg-surface px-4 py-3 text-sm font-medium text-on-surface outline-none transition-colors focus:border-primary"
                            placeholder="Nhập phản hồi của cửa hàng..."
                            aria-label="Nội dung phản hồi"
                        />
                        {replyError && (
                            <div className="mt-3 rounded-xl bg-error/10 p-3 text-xs font-bold text-error">
                                {replyError}
                            </div>
                        )}
                        <div className="mt-5 flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={closeReplyModal}
                                className="rounded-xl bg-surface-container px-4 py-2.5 text-xs font-black text-on-surface transition-colors hover:bg-surface-container-high"
                            >
                                Hủy
                            </button>
                            <button
                                type="submit"
                                disabled={actionReviewIds.includes(replyingReview.id)}
                                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-black text-on-primary transition-colors hover:bg-primary-container disabled:opacity-50"
                            >
                                {actionReviewIds.includes(replyingReview.id) && (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                )}
                                Lưu phản hồi
                            </button>
                        </div>
                    </form>
                </div>
            )}
        </div>
    );
}
