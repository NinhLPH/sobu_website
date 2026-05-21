import {useState, useEffect} from 'react';
import {useParams, Link} from 'react-router-dom';
import {ChevronRight, Star, ShoppingBag, Truck, ShieldCheck, Minus, Plus} from 'lucide-react';

import {mockProducts} from '../data/mockData';
import {useCartStore} from '../store/useCartStore';
import {formatCurrency} from "../util/format";
import ProductSlider from "../components/common/ProductSlider";

export default function ProductDetail() {
    const {id} = useParams();
    const product = mockProducts.find(p => p.id === id) || mockProducts[0];
    const [quantity, setQuantity] = useState(1);
    const addToCart = useCartStore(state => state.addToCart);
    const [mainImage, setMainImage] = useState(product.imageUrl);

    useEffect(() => {
        setMainImage(product.imageUrl);
        setQuantity(1);
    }, [id, product]);

    const relatedProducts = mockProducts.filter(p => p.id !== id);
    const decreaseQuantity = () => setQuantity(prev => Math.max(1, prev - 1));
    const increaseQuantity = () => setQuantity(prev => prev + 1);

    const handleAddToCart = () => {
        addToCart(product, quantity);
    };

    return (
        <main className="max-w-screen-2xl mx-auto px-6 pt-24 pb-16 bg-surface">
            <nav className="flex items-center gap-1.5 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5"/>
                <Link to="/products" className="hover:text-primary transition-colors">Sản phẩm</Link>
                <ChevronRight className="w-3.5 h-3.5"/>
                <span className="text-primary">{product.category}</span>
            </nav>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
                <div className="lg:col-span-7 space-y-4">
                    <div
                        className="relative rounded-2xl overflow-hidden bg-surface-container-lowest shadow-[0_20px_50px_-15px_rgba(14,48,78,0.06)] aspect-[4/3] flex items-center justify-center p-8">
                        <img className="w-full h-full object-contain" src={mainImage} alt={product.name}/>
                        <div className="absolute top-6 left-6 flex flex-col gap-2">
                            {product.isNew && (
                                <span
                                    className="bg-primary text-white px-3.5 py-1.5 rounded-full text-[10px] font-black tracking-widest uppercase shadow-md">
                                    Mới
                                </span>
                            )}
                        </div>
                    </div>
                    <div className="grid grid-cols-4 gap-4">
                        {product.thumbnailUrls?.map((url, index) => (
                            <div
                                key={index}
                                onClick={() => setMainImage(url)}
                                className={`aspect-square rounded-xl bg-surface-container-lowest shadow-sm overflow-hidden cursor-pointer transition-all duration-300 p-2.5 flex items-center justify-center
                                ${mainImage === url ? 'ring-2 ring-primary scale-95' : 'hover:scale-105 hover:shadow-md'}`}
                            >
                                <img className="w-full h-full object-contain" src={url} alt={`Thumb ${index}`}/>
                            </div>
                        ))}
                    </div>
                </div>
                <div className="lg:col-span-5 space-y-6 lg:sticky lg:top-28">
                    <div>
                        <p className="text-[10px] font-black text-outline mb-2 tracking-widest uppercase">{product.brand}</p>
                        <h1 className="text-2xl md:text-3xl font-black text-on-surface tracking-tight leading-tight mb-4">
                            {product.name}
                        </h1>
                        <div className="flex items-center gap-4 text-xs font-bold">
                            <span className="bg-surface-container px-3 py-1.5 rounded-full text-on-surface">
                                Mã: {product.id}
                            </span>
                            <div className="flex items-center gap-1 bg-surface-container px-3 py-1.5 rounded-full">
                                <Star className="text-[#FFB800] w-3.5 h-3.5 fill-[#FFB800]"/>
                                <span>{product.rating || 5.0}</span>
                                <span className="text-outline">({product.reviewsCount || 0})</span>
                            </div>
                        </div>
                    </div>
                    <div className="flex items-baseline gap-3">
                        <span className="text-3xl font-black text-primary tracking-tight">
                            {formatCurrency(product.price)}
                        </span>
                        {product.originalPrice && (
                            <span className="text-sm text-outline line-through font-bold">
                                {formatCurrency(product.originalPrice)}
                            </span>
                        )}
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div className="bg-surface-container-low p-3.5 rounded-xl">
                            <p className="text-[9px] text-outline uppercase font-black tracking-widest mb-0.5">Tỷ lệ</p>
                            <p className="font-bold text-on-surface text-sm">{product.scale || 'N/A'}</p>
                        </div>
                        <div className="bg-surface-container-low p-3.5 rounded-xl">
                            <p className="text-[9px] text-outline uppercase font-black tracking-widest mb-0.5">Tình
                                trạng</p>
                            <p className="font-bold text-on-surface text-sm">{product.stock > 0 ? 'Còn hàng' : 'Hết hàng'}</p>
                        </div>
                    </div>
                    <p className="text-on-surface-variant text-sm font-medium leading-relaxed">
                        {product.description}
                    </p>
                    <div className="flex items-center gap-4 pt-4">
                        <div className="flex items-center bg-surface-container rounded-full px-1.5 py-1.5">
                            <button onClick={decreaseQuantity}
                                    className="w-9 h-9 bg-white rounded-full flex items-center justify-center shadow-sm hover:text-primary transition-colors disabled:opacity-50">
                                <Minus className="w-4 h-4"/>
                            </button>
                            <span className="w-12 text-center font-black text-base">{quantity}</span>
                            <button onClick={increaseQuantity}
                                    className="w-9 h-9 bg-white rounded-full flex items-center justify-center shadow-sm hover:text-primary transition-colors">
                                <Plus className="w-4 h-4"/>
                            </button>
                        </div>
                        <button onClick={handleAddToCart}
                                className="flex-1 bg-gradient-to-br from-primary to-primary-container text-white h-12 rounded-full font-black text-sm flex items-center justify-center gap-2 shadow-[0_10px_20px_-5px_rgba(75,186,254,0.3)] hover:scale-[1.01] transition-transform">
                            <ShoppingBag className="w-4.5 h-4.5"/> Thêm vào giỏ
                        </button>
                    </div>
                    <div className="flex items-center gap-6 pt-5 border-t border-outline-variant/20">
                        <div className="flex items-center gap-2.5 text-on-surface font-bold text-xs">
                            <div
                                className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-primary">
                                <Truck className="w-4 h-4"/>
                            </div>
                            Giao hàng toàn quốc
                        </div>
                        <div className="flex items-center gap-2.5 text-on-surface font-bold text-xs">
                            <div
                                className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-primary">
                                <ShieldCheck className="w-4 h-4"/>
                            </div>
                            Bảo hành chính hãng
                        </div>
                    </div>

                </div>
            </div>

            <div className="mt-16 pt-12 border-t border-surface-container-high max-w-4xl mx-auto">
                <div className="flex gap-8 border-b border-surface-container mb-6">
                    <button
                        className="text-sm font-black text-primary border-b-2 border-primary pb-3 uppercase tracking-wider">
                        Mô tả chi tiết
                    </button>
                    <button
                        className="text-sm font-bold text-outline hover:text-on-surface pb-3 uppercase tracking-wider transition-colors cursor-not-allowed">
                        Đánh giá khách hàng
                    </button>
                </div>
                <div className="space-y-6">
                    <p className="text-sm font-bold text-on-surface uppercase tracking-wide">
                        Giới thiệu về dòng sản phẩm {product.name}
                    </p>
                    <p className="text-xs text-on-surface-variant leading-relaxed font-medium text-justify">
                        {product.description}
                    </p>
                    <div
                        className="mt-8 bg-surface-container-lowest rounded-2xl p-4 shadow-sm border border-surface-container/60">
                        <span className="text-[11px] font-black uppercase tracking-wider text-outline mb-4 block">
                            Thông số kỹ thuật sản phẩm
                        </span>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-3.5 text-xs font-bold">
                            <div className="flex justify-between py-2 border-b border-surface-container-low">
                                <span className="text-outline font-medium">Mã sản phẩm:</span>
                                <span className="text-on-surface font-black">{product.id}</span>
                            </div>
                            <div className="flex justify-between py-2 border-b border-surface-container-low">
                                <span className="text-outline font-medium">Thương hiệu sản xuất:</span>
                                <span
                                    className="text-on-surface font-black">{product.brand !== 'N/A' ? product.brand : 'Chính hãng SOBU'}</span>
                            </div>
                            <div className="flex justify-between py-2 border-b border-surface-container-low">
                                <span className="text-outline font-medium">Tỷ lệ mô hình:</span>
                                <span className="text-on-surface font-black">{product.scale || 'N/A'}</span>
                            </div>
                            <div className="flex justify-between py-2 border-b border-surface-container-low">
                                <span className="text-outline font-medium">Phân nhóm danh mục:</span>
                                <span className="text-on-surface font-black">{product.category}</span>
                            </div>
                        </div>
                    </div>
                    <div
                        className="mt-6 bg-primary-container/10 border border-primary/10 rounded-2xl p-4 text-[11px] text-on-surface-variant font-medium leading-relaxed">
                        <span className="font-black text-primary uppercase tracking-wide block mb-1">⚠️ Khuyến nghị từ SOBU Workshop:</span>
                        Sản phẩm là mô hình tĩnh cao cấp có nhiều chi tiết cơ khí nhỏ, vui lòng tránh tầm tay trẻ em
                        dưới 3 tuổi. Bảo quản nơi khô ráo, tránh ánh nắng mặt trời trực tiếp để giữ màu sơn tĩnh điện
                        luôn ở trạng thái bóng bẩy nhất.
                    </div>
                </div>
            </div>

            <div className="mt-20 pt-12 border-t border-surface-container-high w-full">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-lg font-black text-on-surface uppercase tracking-tight">
                        Sản phẩm có thể bạn sẽ thích
                    </h2>
                    <Link to="/products" className="text-xs font-bold text-primary hover:underline transition-colors">
                        Xem tất cả
                    </Link>
                </div>
                <ProductSlider products={relatedProducts}/>
            </div>
        </main>
    );
}