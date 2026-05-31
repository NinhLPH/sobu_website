import React from 'react';
import {ShoppingCart} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

import {useCartStore} from "../../store/useCartStore";
import {ProductModel} from "../../interface/product.model";
import {formatCurrency} from "../../util/format";

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
            className="group bg-surface-container-lowest rounded-[2rem] overflow-hidden transition-all duration-500 hover:-translate-y-1.5 hover:shadow-[0_15px_45px_-10px_rgba(14,48,78,0.06)] cursor-pointer h-full flex flex-col p-2"
        >
            <div
                className="aspect-[4/3] w-full relative bg-surface-container-low rounded-[1.5rem] flex items-center justify-center overflow-hidden">
                <img
                    className="w-full h-full object-cover group-hover:scale-108 transition-transform duration-700"
                    src={product.imageUrl}
                    alt={product.name}
                />
                {product.isNew && (
                    <div
                        className="absolute top-2.5 right-2.5 bg-tertiary-container text-on-tertiary-container text-[9px] font-black px-2.5 py-1 rounded-full uppercase tracking-wider shadow-sm">
                        Mới
                    </div>
                )}
                {product.isHot && (
                    <div
                        className="absolute top-2.5 left-2.5 bg-error text-white text-[9px] font-black px-2.5 py-1 rounded-full uppercase tracking-wider shadow-sm">
                        Hot
                    </div>
                )}
            </div>

            <div className="p-2.5 flex flex-col flex-1 mt-1">
                <p className="text-[9px] text-outline font-bold mb-1 uppercase tracking-widest truncate">
                    {product.brand !== 'N/A' ? product.brand : product.category}
                </p>
                <h3 className="text-sm font-bold text-on-surface mb-2 line-clamp-2 group-hover:text-primary transition-colors flex-1 leading-tight min-h-[40px]">
                    {product.name}
                </h3>
                {product.originalPrice && (
                    <div className="flex items-baseline gap-2 mb-0.5">
                        <span className="text-outline line-through text-xs">
                            {formatCurrency(product.originalPrice)}
                        </span>
                    </div>
                )}

                <div className="flex items-center justify-between mt-auto pt-1">
                    <span className="text-base font-black text-on-surface">
                        {formatCurrency(product.price)}
                    </span>
                    <button
                        onClick={handleAddToCart}
                        className="w-9 h-9 rounded-full bg-gradient-to-br from-primary to-primary-container flex items-center justify-center text-white hover:scale-105 transition-all shadow-md shadow-primary/10"
                        aria-label="Thêm vào giỏ"
                    >
                        <ShoppingCart className="w-4 h-4"/>
                    </button>
                </div>
            </div>
        </div>
    );
}