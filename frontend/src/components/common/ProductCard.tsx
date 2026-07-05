import React from 'react';
import {ShoppingCart} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

import {useCartStore} from "../../store/useCartStore";
import {ProductModel} from "../../interface/product.model";
import {formatCurrency} from "../../utils/format";
import ProductRatingSummary from './ProductRatingSummary';

interface ProductCardProps {
    product: ProductModel;
    key?: React.Key;
}

export default function ProductCard({product}: ProductCardProps) {
    const addToCart = useCartStore(state => state.addToCart);
    const navigate = useNavigate();

    const handleAddToCart = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        addToCart(product);
    };

    return (
        <div
            onClick={() => navigate(`/product/${product.id}`)}
            className="group flex h-full cursor-pointer flex-col overflow-hidden rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-1.5 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-primary/20 hover:shadow-[0_12px_34px_-16px_rgba(14,48,78,0.28)] sm:rounded-[2rem] sm:p-2"
        >
            <div
                className="relative flex aspect-square w-full items-center justify-center overflow-hidden rounded-xl bg-surface-container-low sm:aspect-[4/3] sm:rounded-[1.5rem]">
                <img
                    className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                    src={product.imageUrl}
                    alt={product.name}
                />
                {product.isNew && (
                    <div
                        className="absolute right-1.5 top-1.5 rounded-full bg-tertiary-container px-2 py-0.5 text-[8px] font-black uppercase leading-4 tracking-wider text-on-tertiary-container shadow-sm sm:right-2.5 sm:top-2.5 sm:px-2.5 sm:py-1 sm:text-[9px]">
                        Mới
                    </div>
                )}
                {product.isHot && (
                    <div
                        className="absolute left-1.5 top-1.5 rounded-full bg-error px-2 py-0.5 text-[8px] font-black uppercase leading-4 tracking-wider text-on-error shadow-sm sm:left-2.5 sm:top-2.5 sm:px-2.5 sm:py-1 sm:text-[9px]">
                        Hot
                    </div>
                )}
            </div>

            <div className="mt-0.5 flex flex-1 flex-col p-2 sm:mt-1 sm:p-2.5">
                <p className="mb-1 truncate text-[8px] font-bold uppercase leading-3 tracking-wider text-outline sm:text-[9px] sm:tracking-widest">
                    {product.brand !== 'N/A' ? product.brand : product.category}
                </p>
                <h3 className="mb-1.5 line-clamp-2 min-h-[34px] flex-1 text-xs font-bold leading-snug text-on-surface transition-colors group-hover:text-primary sm:mb-2 sm:min-h-[40px] sm:text-sm sm:leading-tight">
                    {product.name}
                </h3>
                <ProductRatingSummary
                    rating={product.rating}
                    reviewsCount={product.reviewsCount}
                    className="mb-2 text-[10px] font-black text-on-surface sm:text-xs"
                    starClassName="h-2.5 w-2.5 sm:h-3 sm:w-3"
                />
                {product.originalPrice && (
                    <div className="mb-0.5 flex items-baseline gap-2">
                        <span className="text-[10px] leading-none text-outline line-through sm:text-xs">
                            {formatCurrency(product.originalPrice)}
                        </span>
                    </div>
                )}

                <div className="mt-auto flex items-center justify-between gap-2 pt-1">
                    <span className="text-[13px] font-black leading-none text-on-surface sm:text-base">
                        {formatCurrency(product.price)}
                    </span>
                    <button
                        onClick={handleAddToCart}
                        className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-sm shadow-primary/10 transition-all hover:scale-105 focus-visible:ring-2 focus-visible:ring-primary/40 sm:h-9 sm:w-9 sm:shadow-md"
                        aria-label="Thêm vào giỏ"
                    >
                        <ShoppingCart className="h-3.5 w-3.5 sm:h-4 sm:w-4"/>
                    </button>
                </div>
            </div>
        </div>
    );
}
