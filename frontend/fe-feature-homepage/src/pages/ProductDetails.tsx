import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ChevronRight, Star, ShoppingBag, Clock, Edit, Truck, ShieldCheck, Minus, Plus } from 'lucide-react';

import { mockProducts } from '../data/mockData';

import { useCartStore } from '../store/useCartStore';
import {formatCurrency} from "../util/format";

export default function ProductDetail() {
    const { id } = useParams();
    const product = mockProducts.find(p => p.id === id) || mockProducts[0]; // fallback for demo
    const [quantity, setQuantity] = useState(1);
    const addToCart = useCartStore(state => state.addToCart);

    const [mainImage, setMainImage] = useState(product.imageUrl);

    const decreaseQuantity = () => setQuantity(prev => Math.max(1, prev - 1));
    const increaseQuantity = () => setQuantity(prev => prev + 1);

    const handleAddToCart = () => {
        addToCart(product, quantity);
    };

    return (
        <main className="max-w-7xl mx-auto px-6 pt-32 pb-24">
            {/* Breadcrumb */}
            <nav className="flex items-center space-x-2 text-sm text-on-surface-variant mb-8 font-medium">
                <Link to="/" className="hover:text-primary">Trang chủ</Link>
                <ChevronRight className="w-4 h-4" />
                <Link to="/products" className="hover:text-primary">Sản phẩm</Link>
                <ChevronRight className="w-4 h-4" />
                <span className="text-primary">{product.category}</span>
            </nav>

            {/* Product Detail Section */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
                {/* Left: Product Image Gallery */}
                <div className="lg:col-span-7 space-y-6">
                    <div className="relative rounded-xl overflow-hidden bg-white shadow-sm border border-outline-variant/30 aspect-[4/3] flex items-center justify-center p-8">
                        <img
                            className="w-full h-full object-contain"
                            src={mainImage}
                            alt={product.name}
                        />
                        <div className="absolute top-6 left-6 flex flex-col gap-3">
                            {product.isNew && (
                                <span className="bg-primary text-white px-4 py-1 rounded-full text-xs font-bold tracking-wider uppercase shadow-sm">
                  Hàng mới
                </span>
                            )}
                            {product.scale && (
                                <span className="bg-secondary-container text-on-secondary-container px-4 py-1 rounded-full text-xs font-bold tracking-wider uppercase shadow-sm">
                  Tỉ lệ {product.scale}
                </span>
                            )}
                        </div>
                    </div>

                    {/* Thumbnails Grid */}
                    <div className="grid grid-cols-4 gap-4">
                        {product.thumbnailUrls && product.thumbnailUrls.map((url, index) => (
                            <div
                                key={index}
                                onClick={() => setMainImage(url)}
                                className={`aspect-square rounded-lg bg-white shadow-sm overflow-hidden border-2 cursor-pointer transition-all p-2 flex items-center justify-center
                  ${mainImage === url ? 'border-primary opacity-100' : 'border-transparent opacity-60 hover:opacity-100'}`}
                            >
                                <img className="w-full h-full object-contain" src={url} alt={`Thumbnail ${index + 1}`} />
                            </div>
                        ))}
                    </div>
                </div>

                {/* Right: Product Information */}
                <div className="lg:col-span-5 space-y-8 lg:sticky lg:top-36 flex flex-col">
                    <div>
                        <p className="text-sm font-bold text-secondary mb-2 tracking-widest uppercase">Phòng trưng bày SOBU</p>
                        <h1 className="text-4xl md:text-5xl font-black text-on-surface tracking-tighter leading-tight mb-4">
                            {product.name}
                        </h1>

                        <div className="flex flex-wrap items-center gap-4 text-on-surface-variant text-sm font-medium">
                            <span>Mã: <span className="text-on-surface">{product.id}</span></span>
                            <span className="w-1.5 h-1.5 rounded-full bg-outline-variant hidden sm:block"></span>
                            <div className="flex items-center">
                                <Star className="text-[#FFB800] w-4 h-4 fill-[#FFB800]" />
                                <span className="ml-1 text-on-surface font-bold">{product.rating || 5.0}</span>
                                <span className="ml-1">({product.reviewsCount || 0} đánh giá)</span>
                            </div>
                        </div>
                    </div>

                    <div className="bg-surface-container-low p-6 rounded-xl border-l-4 border-primary shadow-sm">
                        <p className="text-on-surface-variant text-sm font-medium mb-1">Giá bán hiện tại</p>
                        <div className="flex items-baseline gap-3">
              <span className="text-3xl font-black text-primary tracking-tight">
                {formatCurrency(product.price)}
              </span>
                            {product.originalPrice && (
                                <span className="text-lg text-on-surface-variant line-through font-medium">
                  {formatCurrency(product.originalPrice)}
                </span>
                            )}
                        </div>
                    </div>

                    <div className="space-y-4">
                        <h3 className="text-sm font-bold text-on-surface-variant uppercase tracking-wider">Thông số kỹ thuật</h3>
                        <div className="grid grid-cols-2 gap-4">
                            <div className="p-3 bg-white rounded-lg border border-outline-variant/30">
                                <p className="text-[10px] text-on-surface-variant uppercase font-bold">Thương hiệu</p>
                                <p className="text-sm font-semibold text-on-surface">{product.brand}</p>
                            </div>
                            <div className="p-3 bg-white rounded-lg border border-outline-variant/30">
                                <p className="text-[10px] text-on-surface-variant uppercase font-bold">Danh mục</p>
                                <p className="text-sm font-semibold text-on-surface">{product.category}</p>
                            </div>
                        </div>
                        <p className="text-sm text-on-surface-variant leading-relaxed mt-4">
                            {product.description}
                        </p>
                    </div>

                    {/* Action Controls */}
                    <div className="space-y-6 pt-4">
                        <div className="flex items-center gap-6">
                            <span className="text-sm font-bold text-on-surface-variant uppercase tracking-wider">Số lượng</span>
                            <div className="flex items-center bg-white border border-outline-variant/30 rounded-full px-2 py-1 shadow-sm">
                                <button
                                    onClick={decreaseQuantity}
                                    className="w-10 h-10 flex items-center justify-center text-on-surface hover:text-primary transition-colors disabled:opacity-50"
                                    disabled={quantity <= 1}
                                >
                                    <Minus className="w-4 h-4" />
                                </button>
                                <span className="w-12 text-center font-bold text-lg">{quantity}</span>
                                <button
                                    onClick={increaseQuantity}
                                    className="w-10 h-10 flex items-center justify-center text-on-surface hover:text-primary transition-colors"
                                >
                                    <Plus className="w-4 h-4" />
                                </button>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            <button
                                onClick={handleAddToCart}
                                className="bg-primary text-white py-4 px-6 rounded-lg font-bold flex items-center justify-center gap-2 shadow-lg shadow-primary/20 transition-transform active:scale-95 hover:brightness-110"
                            >
                                <ShoppingBag className="w-5 h-5" />
                                <span>Thêm vào giỏ</span>
                            </button>
                            <button className="bg-surface-container-highest text-on-surface py-4 px-6 rounded-lg font-bold flex items-center justify-center gap-2 border border-outline-variant/30 transition-transform active:scale-95 hover:bg-outline-variant/20">
                                <Clock className="w-5 h-5" />
                                <span>Đặt trước</span>
                            </button>
                        </div>
                    </div>

                    <div className="flex items-center justify-between py-6 border-t border-outline-variant/30 mt-auto">
                        <div className="flex items-center gap-2 text-on-surface-variant">
                            <Truck className="text-primary w-5 h-5" />
                            <span className="text-sm font-medium">Giao miễn phí</span>
                        </div>
                        <div className="flex items-center gap-2 text-on-surface-variant">
                            <ShieldCheck className="text-primary w-5 h-5" />
                            <span className="text-sm font-medium">Bảo hành 12th</span>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    );
}
