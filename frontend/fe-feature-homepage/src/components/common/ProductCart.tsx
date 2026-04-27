import React from 'react';
import { ShoppingCart } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';

import {useCartStore} from "../../store/useCartStore";
import {Product} from "../../interface/product";
import {formatCurrency} from "../../util/format";

interface ProductCardProps {
    product: Product;
    key?: React.Key;
}

export default function ProductCard({ product }: ProductCardProps) {
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
            className="group bg-surface-container-lowest rounded-lg overflow-hidden transition-all duration-500 hover:-translate-y-2 hover:shadow-xl hover:shadow-primary/5 border border-outline-variant/30 cursor-pointer h-full flex flex-col"
        >
            <div className="aspect-square relative p-4 bg-surface-variant flex items-center justify-center">
                <img
                    className="w-full h-full object-contain group-hover:scale-110 transition-transform duration-700"
                    src={product.imageUrl}
                    alt={product.name}
                />
                {product.isNew && (
                    <div className="absolute top-4 right-4 bg-tertiary-container text-on-tertiary-container text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-wider">
                        Mới
                    </div>
                )}
                {product.isHot && (
                    <div className="absolute top-4 left-4 bg-error text-white text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-wider">
                        Hot Trend
                    </div>
                )}
            </div>
            <div className="p-6 flex flex-col flex-1">
                <p className="text-xs text-outline font-bold mb-1 uppercase tracking-widest">{product.brand !== 'N/A' ? product.brand : product.category}</p>
                <h3 className="text-lg font-bold text-on-surface mb-4 line-clamp-2 group-hover:text-primary transition-colors flex-1">
                    {product.name}
                </h3>

                {product.originalPrice && (
                    <div className="flex items-baseline gap-2 mb-1">
                        <span className="text-outline line-through text-sm">{formatCurrency(product.originalPrice)}</span>
                    </div>
                )}
                <div className="flex items-center justify-between mt-auto">
          <span className="text-xl font-black text-secondary">
            {formatCurrency(product.price)}
          </span>
                    <button
                        onClick={handleAddToCart}
                        className="w-10 h-10 rounded-full bg-surface-container-high flex items-center justify-center text-primary hover:bg-primary hover:text-white transition-all shadow-sm"
                        aria-label="Thêm vào giỏ"
                    >
                        <ShoppingCart className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    );
}
