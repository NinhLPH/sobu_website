import { useRef } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Product } from '../../interface/product';
import ProductCard from './ProductCard';

export default function ProductSlider({ products }: { products: Product[] }) {
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
                className="absolute -left-5 top-1/2 -translate-y-1/2 z-10 w-11 h-11 bg-white rounded-full flex items-center justify-center shadow-[0_10px_30px_rgba(14,48,78,0.1)] text-on-surface hover:text-primary transition-colors opacity-0 group-hover:opacity-100 hidden md:flex border border-surface-container"
            >
                <ChevronLeft className="w-5 h-5" />
            </button>

            <div
                ref={sliderRef}
                className="flex gap-5 overflow-x-auto snap-x snap-mandatory py-4 px-1 scrollbar-hide"
                style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
            >
                {products.map(product => (
                    <div
                        key={product.id}
                        className="w-[75vw] sm:w-[calc(33.333%-14px)] lg:w-[calc(20%-16px)] flex-shrink-0 snap-start"
                    >
                        <ProductCard product={product} />
                    </div>
                ))}
            </div>
            <button
                onClick={() => scroll('right')}
                className="absolute -right-5 top-1/2 -translate-y-1/2 z-10 w-11 h-11 bg-white rounded-full flex items-center justify-center shadow-[0_10px_30px_rgba(14,48,78,0.1)] text-on-surface hover:text-primary transition-colors opacity-0 group-hover:opacity-100 hidden md:flex border border-surface-container"
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