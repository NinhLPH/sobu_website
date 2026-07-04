import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    CheckCircle2,
    ChevronLeft,
    ChevronRight,
    ImagePlus,
    Loader2,
    LockKeyhole,
    MessageSquareReply,
    ShieldCheck,
    Star
} from 'lucide-react';
import { BASE_URL } from '../../api/api-client';
import { ProductModel } from '../../interface/product.model';
import { ReviewResponseDto } from '../../interface/review.model';
import { useAuthStore } from '../../store/useAuthStore';
import { useReviewStore } from '../../store/useReviewStore';

interface ProductReviewSectionProps {
    product: ProductModel;
}

const MAX_REVIEW_FILES = 10;
const MAX_REVIEW_FILE_SIZE = 5 * 1024 * 1024;

const getReviewImageSrc = (url: string) => {
    if (!url) return '';
    if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
        return url;
    }
    return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;
};

const formatReviewDate = (value?: string) => {
    if (!value) return 'Vừa cập nhật';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return 'Vừa cập nhật';
    return parsed.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
};

const RatingStars = ({ rating, size = 'h-4 w-4' }: { rating: number; size?: string }) => (
    <div className="flex items-center gap-0.5" aria-label={`${rating}/5 sao`}>
        {Array.from({ length: 5 }).map((_, index) => (
            <Star
                key={index}
                className={`${size} ${index < rating ? 'fill-[#FFB800] text-[#FFB800]' : 'text-outline-variant'}`}
            />
        ))}
    </div>
);

const ReviewCard = ({ review }: { review: ReviewResponseDto }) => (
    <article className="rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-4 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
                <p className="text-sm font-black text-on-surface">
                    {review.customerName || 'Khách hàng SOBU'}
                </p>
                <p className="mt-1 text-[11px] font-bold text-outline">
                    {formatReviewDate(review.createdAt)}
                </p>
            </div>
            <RatingStars rating={review.rating} />
        </div>
        <p className="mt-3 text-sm font-medium leading-relaxed text-on-surface-variant">
            {review.content}
        </p>
        {(review.imageUrls || []).length > 0 && (
            <div className="mt-4 grid grid-cols-3 gap-2 sm:grid-cols-5">
                {(review.imageUrls || []).map((url, index) => (
                    <a
                        key={`${url}-${index}`}
                        href={getReviewImageSrc(url)}
                        target="_blank"
                        rel="noreferrer"
                        className="aspect-square overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container transition-colors hover:border-primary"
                    >
                        <img
                            src={getReviewImageSrc(url)}
                            alt={`Ảnh đánh giá ${index + 1}`}
                            className="h-full w-full object-cover"
                        />
                    </a>
                ))}
            </div>
        )}
        {review.adminReply && (
            <div className="mt-4 rounded-xl border border-primary/15 bg-primary-container/10 p-3">
                <div className="mb-1 flex items-center gap-2 text-[11px] font-black uppercase tracking-wider text-primary">
                    <MessageSquareReply className="h-3.5 w-3.5" />
                    Phản hồi từ SOBU
                </div>
                <p className="text-xs font-semibold leading-relaxed text-on-surface-variant">
                    {review.adminReply}
                </p>
            </div>
        )}
    </article>
);

export default function ProductReviewSection({ product }: ProductReviewSectionProps) {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const {
        reviews,
        reviewsPage,
        isReviewsLoading,
        reviewsError,
        fetchPublicReviews,
        verifiedOrder,
        reviewEligibility,
        isCheckingEligibility,
        eligibilityError,
        checkReviewEligibility,
        resetVerification,
        isSubmittingReview,
        submitError,
        submitSuccessMessage,
        submitReview,
        clearSubmitState
    } = useReviewStore();

    const [rating, setRating] = useState(5);
    const [content, setContent] = useState('');
    const [files, setFiles] = useState<File[]>([]);
    const [formError, setFormError] = useState<string | null>(null);

    useEffect(() => {
        void fetchPublicReviews(product.id, 0);
        resetVerification();
        setRating(5);
        setContent('');
        setFiles([]);
        setFormError(null);
        if (isAuthenticated) {
            void checkReviewEligibility(product.id);
        }
    }, [product.id, isAuthenticated, fetchPublicReviews, resetVerification, checkReviewEligibility]);

    const fallbackAverageRating = useMemo(() => {
        if (!reviews.length) return 0;
        return reviews.reduce((total, review) => total + review.rating, 0) / reviews.length;
    }, [reviews]);

    const averageRating = product.rating ?? fallbackAverageRating;
    const totalReviews = product.reviewsCount ?? reviewsPage.totalElements;

    const filePreviews = useMemo(
        () => files.map(file => ({
            file,
            url: URL.createObjectURL(file)
        })),
        [files]
    );

    useEffect(() => () => {
        filePreviews.forEach(preview => URL.revokeObjectURL(preview.url));
    }, [filePreviews]);

    const selectedFileText = files.length
        ? `${files.length}/${MAX_REVIEW_FILES} ảnh đã chọn`
        : 'Chọn ảnh đính kèm';

    const validateFiles = (nextFiles: File[]) => {
        if (nextFiles.length > MAX_REVIEW_FILES) {
            return `Chỉ được tải tối đa ${MAX_REVIEW_FILES} ảnh cho một đánh giá.`;
        }
        const invalidType = nextFiles.find(file => !file.type.startsWith('image/'));
        if (invalidType) {
            return 'Vui lòng chỉ chọn tệp hình ảnh.';
        }
        const oversized = nextFiles.find(file => file.size > MAX_REVIEW_FILE_SIZE);
        if (oversized) {
            return 'Mỗi ảnh đính kèm không được vượt quá 5MB.';
        }
        return null;
    };

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        const nextFiles = Array.from(event.target.files || []);
        const error = validateFiles(nextFiles);
        setFormError(error);
        setFiles(error ? [] : nextFiles);
    };

    const handleSubmitReview = async (event: FormEvent) => {
        event.preventDefault();
        clearSubmitState();
        setFormError(null);

        if (!verifiedOrder || !reviewEligibility?.canReview) {
            setFormError('Hãy mua hàng rồi mới đăng review.');
            return;
        }
        if (rating < 1 || rating > 5) {
            setFormError('Vui lòng chọn số sao từ 1 đến 5.');
            return;
        }
        if (!content.trim()) {
            setFormError('Vui lòng nhập nội dung đánh giá.');
            return;
        }
        const fileError = validateFiles(files);
        if (fileError) {
            setFormError(fileError);
            return;
        }

        try {
            await submitReview(product.id, verifiedOrder.id, rating, content, files);
            setRating(5);
            setContent('');
            setFiles([]);
        } catch {
            // The store exposes submitError below.
        }
    };

    const lockedReason = reviewEligibility?.alreadyReviewed
        ? 'Bạn đã đánh giá sản phẩm này.'
        : (eligibilityError || reviewEligibility?.reason || 'Hãy mua hàng rồi mới đăng review.');

    return (
        <section id="product-reviews" className="mt-14 border-t border-surface-container-high pt-10">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                        Đánh giá khách hàng
                    </p>
                    <h2 className="mt-1 text-xl font-black tracking-tight text-on-surface">
                        Trải nghiệm thực tế sau khi mua
                    </h2>
                </div>
                <div className="flex items-center gap-3 rounded-full bg-surface-container px-4 py-2 text-xs font-black text-on-surface">
                    <RatingStars rating={Math.round(averageRating || 0)} size="h-3.5 w-3.5" />
                    <span>{averageRating ? averageRating.toFixed(1) : '0.0'}</span>
                    <span className="text-outline">({totalReviews} đánh giá)</span>
                </div>
            </div>

            <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
                <div className="space-y-4">
                    {reviewsError && (
                        <div className="rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                            {reviewsError}
                        </div>
                    )}
                    {isReviewsLoading ? (
                        <div className="flex min-h-40 flex-col items-center justify-center rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-6 text-outline">
                            <Loader2 className="mb-2 h-8 w-8 animate-spin text-primary" />
                            <p className="text-xs font-bold">Đang tải đánh giá...</p>
                        </div>
                    ) : reviews.length > 0 ? (
                        reviews.map(review => <ReviewCard key={review.id} review={review} />)
                    ) : (
                        <div className="rounded-2xl border border-dashed border-outline-variant/40 bg-surface-container-lowest p-8 text-center">
                            <Star className="mx-auto mb-3 h-8 w-8 text-outline-variant" />
                            <p className="text-sm font-black text-on-surface">
                                Chưa có đánh giá hiển thị
                            </p>
                            <p className="mt-1 text-xs font-medium text-outline">
                                Hãy là người đầu tiên chia sẻ trải nghiệm sau khi đơn hàng được giao.
                            </p>
                        </div>
                    )}

                    {reviewsPage.totalPages > 1 && (
                        <div className="flex items-center justify-between rounded-xl bg-surface-container-lowest px-4 py-3">
                            <span className="text-xs font-bold text-outline">
                                Trang {reviewsPage.pageNumber + 1}/{reviewsPage.totalPages}
                            </span>
                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => void fetchPublicReviews(product.id, Math.max(0, reviewsPage.pageNumber - 1))}
                                    disabled={!reviewsPage.hasPrevious || isReviewsLoading}
                                    className="rounded-lg bg-surface-container p-2 text-on-surface transition-colors hover:bg-surface-container-high disabled:opacity-40"
                                    aria-label="Trang đánh giá trước"
                                >
                                    <ChevronLeft className="h-4 w-4" />
                                </button>
                                <button
                                    type="button"
                                    onClick={() => void fetchPublicReviews(product.id, reviewsPage.pageNumber + 1)}
                                    disabled={!reviewsPage.hasNext || isReviewsLoading}
                                    className="rounded-lg bg-surface-container p-2 text-on-surface transition-colors hover:bg-surface-container-high disabled:opacity-40"
                                    aria-label="Trang đánh giá sau"
                                >
                                    <ChevronRight className="h-4 w-4" />
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                <aside className="space-y-4 rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-4 shadow-sm lg:sticky lg:top-28">
                    {!isAuthenticated ? (
                        <div className="space-y-3">
                            <div className="flex items-center gap-2 text-sm font-black text-on-surface">
                                <ShieldCheck className="h-4 w-4 text-primary" />
                                Đánh giá sau khi mua
                            </div>
                            <p className="text-xs font-medium leading-relaxed text-on-surface-variant">
                                Vui lòng đăng nhập để SOBU kiểm tra đơn hàng đã giao và mở phần đánh giá.
                            </p>
                            <Link
                                to="/login"
                                className="inline-flex w-full items-center justify-center rounded-xl bg-primary px-4 py-3 text-xs font-black text-on-primary transition-colors hover:bg-primary-container"
                            >
                                Đăng nhập
                            </Link>
                        </div>
                    ) : isCheckingEligibility ? (
                        <div className="flex min-h-32 flex-col items-center justify-center text-center text-outline">
                            <Loader2 className="mb-3 h-7 w-7 animate-spin text-primary" />
                            <p className="text-xs font-bold">Đang kiểm tra đơn hàng đã giao...</p>
                        </div>
                    ) : reviewEligibility?.canReview && verifiedOrder ? (
                        <form onSubmit={handleSubmitReview} className="space-y-4">
                            <div>
                                <div className="flex items-center gap-2 text-sm font-black text-on-surface">
                                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                                    Gửi đánh giá
                                </div>
                                <p className="mt-1 text-xs font-medium text-outline">
                                    Đơn hàng đã giao hợp lệ. Đánh giá sẽ được đăng công khai ngay sau khi gửi.
                                </p>
                            </div>
                            <div>
                                <span className="mb-2 block text-[11px] font-black uppercase tracking-wider text-outline">
                                    Rating
                                </span>
                                <div className="flex gap-1">
                                    {Array.from({ length: 5 }).map((_, index) => {
                                        const value = index + 1;
                                        return (
                                            <button
                                                key={value}
                                                type="button"
                                                onClick={() => setRating(value)}
                                                className="rounded-lg p-1 text-[#FFB800] transition-colors hover:bg-surface-container"
                                                aria-label={`${value} sao`}
                                            >
                                                <Star
                                                    className={`h-7 w-7 ${value <= rating ? 'fill-[#FFB800]' : 'fill-transparent'}`}
                                                />
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                            <label className="block">
                                <span className="mb-1 block text-[11px] font-black uppercase tracking-wider text-outline">
                                    Nội dung
                                </span>
                                <textarea
                                    value={content}
                                    onChange={(event) => setContent(event.target.value)}
                                    rows={5}
                                    maxLength={1000}
                                    className="w-full resize-none rounded-xl border border-outline-variant/30 bg-surface px-4 py-3 text-sm font-medium text-on-surface outline-none transition-colors focus:border-primary"
                                    placeholder="Chia sẻ cảm nhận của bạn về sản phẩm..."
                                />
                                <span className="mt-1 block text-right text-[10px] font-bold text-outline">
                                    {content.trim().length}/1000
                                </span>
                            </label>
                            <label className="flex cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed border-outline-variant/40 bg-surface px-4 py-3 text-xs font-black text-on-surface transition-colors hover:border-primary hover:text-primary">
                                <ImagePlus className="h-4 w-4" />
                                {selectedFileText}
                                <input
                                    type="file"
                                    accept="image/*"
                                    multiple
                                    onChange={handleFileChange}
                                    className="sr-only"
                                />
                            </label>
                            {files.length > 0 && (
                                <div className="grid grid-cols-5 gap-2">
                                    {filePreviews.map(({ file, url }) => (
                                        <div
                                            key={`${file.name}-${file.lastModified}`}
                                            className="aspect-square overflow-hidden rounded-lg border border-outline-variant/20 bg-surface-container"
                                            title={file.name}
                                        >
                                            <img
                                                src={url}
                                                alt={file.name}
                                                className="h-full w-full object-cover"
                                            />
                                        </div>
                                    ))}
                                </div>
                            )}
                            {(formError || submitError) && (
                                <div className="rounded-xl bg-error/10 p-3 text-xs font-bold text-error">
                                    {formError || submitError}
                                </div>
                            )}
                            <button
                                type="submit"
                                disabled={isSubmittingReview}
                                className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-xs font-black text-on-primary transition-colors hover:bg-primary-container disabled:opacity-50"
                            >
                                {isSubmittingReview && <Loader2 className="h-4 w-4 animate-spin" />}
                                Gửi đánh giá
                            </button>
                        </form>
                    ) : (
                        <div className="space-y-3">
                            <div className="flex items-center gap-2 text-sm font-black text-on-surface">
                                <LockKeyhole className="h-4 w-4 text-outline" />
                                Đánh giá sau khi mua
                            </div>
                            {submitSuccessMessage && (
                                <div className="rounded-xl bg-green-50 p-3 text-xs font-bold text-green-700">
                                    {submitSuccessMessage}
                                </div>
                            )}
                            <div className="rounded-xl border border-dashed border-outline-variant/40 bg-surface p-4">
                                <p className="text-sm font-black text-on-surface">
                                    {reviewEligibility?.alreadyReviewed ? 'Bạn đã đánh giá sản phẩm này' : 'Hãy mua hàng rồi mới đăng review'}
                                </p>
                                <p className="mt-1 text-xs font-medium leading-relaxed text-on-surface-variant">
                                    {lockedReason}
                                </p>
                            </div>
                        </div>
                    )}
                </aside>
            </div>
        </section>
    );
}
