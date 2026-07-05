import {Star} from 'lucide-react';

interface ProductRatingSummaryProps {
    rating?: number;
    reviewsCount?: number;
    className?: string;
    starClassName?: string;
    textClassName?: string;
}

export default function ProductRatingSummary({
    rating = 0,
    reviewsCount = 0,
    className = '',
    starClassName = 'h-3 w-3',
    textClassName = '',
}: ProductRatingSummaryProps) {
    const normalizedRating = Number.isFinite(rating) ? Math.max(0, Math.min(5, rating)) : 0;
    const roundedRating = Math.round(normalizedRating);
    const normalizedCount = Number.isFinite(reviewsCount) ? Math.max(0, reviewsCount) : 0;

    return (
        <div
            className={`flex items-center gap-1 ${className}`}
            aria-label={`${normalizedRating.toFixed(1)} trên 5 sao từ ${normalizedCount} đánh giá`}
        >
            <div className="flex items-center gap-0.5" aria-hidden="true">
                {Array.from({length: 5}).map((_, index) => (
                    <Star
                        key={index}
                        className={`${starClassName} ${
                            index < roundedRating
                                ? 'fill-[#FFB800] text-[#FFB800]'
                                : 'fill-transparent text-outline-variant'
                        }`}
                    />
                ))}
            </div>
            <span className={textClassName}>{normalizedRating.toFixed(1)}</span>
            <span className="font-bold text-outline">({normalizedCount})</span>
        </div>
    );
}
