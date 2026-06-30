import { useRef } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { ProductModel } from '../../interface/product.model';
import ProductCard from './ProductCard';

export default function ProductSlider({ products }: { products: ProductModel[] }) {
    const sliderRef = useRef<HTMLDivElement>(null);

    const scroll = (direction: 'left' | 'right') => {
        if (sliderRef.current) {
            const scrollAmount = direction === 'left' ? -sliderRef.current.offsetWidth : sliderRef.current.offsetWidth;
            sliderRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' });
        }
    };

    return (
        <div className="relative group">
            <button
                onClick={() => scroll('left')}
                className="absolute -left-5 top-1/2 z-10 hidden h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full border border-surface-container bg-surface-container-lowest text-on-surface opacity-0 shadow-[0_10px_30px_rgba(14,48,78,0.1)] transition-colors hover:text-primary group-hover:opacity-100 md:flex"
            >
                <ChevronLeft className="w-5 h-5" />
            </button>

            <div
                ref={sliderRef}
                className="scrollbar-hide flex snap-x snap-mandatory gap-3 overflow-x-auto px-1 py-2 sm:gap-5 sm:py-4"
                style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
            >
                {products.map(product => (
                    <div
                        key={product.id}
                        className="w-[42vw] max-w-[180px] flex-shrink-0 snap-start min-[420px]:w-[31vw] sm:w-[calc(33.333%-14px)] sm:max-w-none lg:w-[calc(20%-16px)]"
                    >
                        <ProductCard product={product} />
                    </div>
                ))}
            </div>
            <button
                onClick={() => scroll('right')}
                className="absolute -right-5 top-1/2 z-10 hidden h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full border border-surface-container bg-surface-container-lowest text-on-surface opacity-0 shadow-[0_10px_30px_rgba(14,48,78,0.1)] transition-colors hover:text-primary group-hover:opacity-100 md:flex"
            >
                <ChevronRight className="w-5 h-5" />
            </button>
            <style>{`
                .scrollbar-hide::-webkit-scrollbar {
                    display: none;
                }
            `}</style>
        </div>
    );
}
