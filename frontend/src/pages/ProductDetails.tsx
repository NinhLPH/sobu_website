import {createElement, useState, useEffect, useMemo} from 'react';
import {useParams, Link} from 'react-router-dom';
import {Check, ChevronRight, MessageCircle, Star, ShoppingBag, Truck, ShieldCheck, Minus, Plus} from 'lucide-react';
import {SiZalo} from 'react-icons/si';

import {useCartStore} from '../store/useCartStore';
import {formatCurrency} from "../utils/format";
import ProductSlider from "../components/common/ProductSlider";
import {useProductStore} from '../store/useProductStore';
import {usePublicUiStore} from '../store/usePublicUiStore';
import {PublicCatalogService} from '../service/public-catalog.service';
import {mapListItemToProductModel, mapDetailToProductModel, ProductModel} from '../interface/product.model';
import ProductReviewSection from '../components/reviews/ProductReviewSection';
import {parseJsonConfig} from '../utils/website-config';

type SocialLinks = Record<string, string>;

export default function ProductDetail() {
    const {id} = useParams();
    const [product, setProduct] = useState<ProductModel | null>(null);
    const [quantity, setQuantity] = useState(1);
    const addToCart = useCartStore(state => state.addToCart);
    const [mainImage, setMainImage] = useState('');
    const [loadingDetail, setLoadingDetail] = useState(true);
    const configMap = usePublicUiStore((state) => state.configMap);
    const socialLinks = parseJsonConfig<SocialLinks>(configMap, 'social_links', {});
    const facebookConsultationUrl = socialLinks.facebook?.trim() || '';
    const zaloConsultationUrl = socialLinks.zalo?.trim() || '';

    const { products, productsLoaded, fetchProducts } = useProductStore();

    useEffect(() => {
        if (!productsLoaded) {
            fetchProducts();
        }
    }, [productsLoaded, fetchProducts]);

    useEffect(() => {
        if (!id) return;
        setLoadingDetail(true);
        PublicCatalogService.getProductDetail(id)
            .then(dto => {
                const mapped = mapDetailToProductModel(dto);
                setProduct(mapped);
                setMainImage(mapped.imageUrl);
            })
            .catch(err => {
                console.error("Error loading product detail from API:", err);
            })
            .finally(() => {
                setLoadingDetail(false);
            });
    }, [id]);

    useEffect(() => {
        if (product) {
            setQuantity(1);
        }
    }, [id, product]);

    const relatedProducts = useMemo(() => {
        return products
            .filter(p => String(p.id) !== id)
            .map(mapListItemToProductModel);
    }, [products, id]);

    const decreaseQuantity = () => setQuantity(prev => Math.max(1, prev - 1));
    const increaseQuantity = () => setQuantity(prev => prev + 1);

    const handleAddToCart = () => {
        if (product) {
            addToCart(product, quantity);
        }
    };

    if (loadingDetail || !product) {
        return (
            <main className="flex min-h-[50vh] w-full min-w-0 flex-col items-center justify-center bg-surface px-4 pb-24 pt-28 sm:px-6 sm:pt-32">
                <div className="animate-spin text-primary w-10 h-10 border-4 border-current border-t-transparent rounded-full" />
                <p className="text-outline text-xs font-bold mt-4">Đang tải chi tiết sản phẩm...</p>
            </main>
        );
    }

    return (
        <main className="w-full min-w-0 bg-surface px-3 pb-14 pt-24 sm:px-6 sm:pb-16">
            <nav className="flex items-center gap-1.5 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5"/>
                <Link to="/products" className="hover:text-primary transition-colors">Sản phẩm</Link>
                <ChevronRight className="w-3.5 h-3.5"/>
                <span className="text-primary">{product.category}</span>
            </nav>

            <div className="grid grid-cols-1 items-start gap-7 lg:grid-cols-12 lg:gap-10">
                <div className="lg:col-span-7 space-y-4">
                    {/* KHUNG ẢNH CHÍNH */}
                    <div
                        className="relative flex aspect-square items-center justify-center overflow-hidden rounded-2xl bg-surface-container-lowest p-4 shadow-[0_20px_50px_-15px_rgba(14,48,78,0.12)] sm:aspect-[4/3] sm:p-8">
                        <img className="w-full h-full object-contain" src={mainImage} alt={product.name}/>
                        <div className="absolute top-6 left-6 flex flex-col gap-2">
                            {product.isNew && (
                                <span
                                    className="rounded-full bg-primary px-3 py-1 text-[9px] font-black uppercase tracking-widest text-on-primary shadow-md sm:px-3.5 sm:py-1.5 sm:text-[10px]">
                                    Mới
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="grid grid-cols-4 gap-2 sm:gap-4">
                        {Array.from(new Set([product.imageUrl, ...(product.thumbnailUrls || [])])).map((url, index) => (
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
                        <h1 className="mb-4 text-xl font-black leading-tight tracking-tight text-on-surface sm:text-2xl md:text-3xl">
                            {product.name}
                        </h1>
                        <div className="flex items-center gap-4 text-xs font-bold">
                            <span className="bg-surface-container px-3 py-1.5 rounded-full text-on-surface">
                                Mã: {product.id}
                            </span>
                            <div className="flex items-center gap-1 bg-surface-container px-3 py-1.5 rounded-full">
                                <Star className="text-[#FFB800] w-3.5 h-3.5 fill-[#FFB800]"/>
                                <span>{(product.rating ?? 0).toFixed(1)}</span>
                                <span className="text-outline">({product.reviewsCount || 0})</span>
                            </div>
                        </div>
                    </div>
                    <div className="flex items-baseline gap-3">
                        <span className="text-2xl font-black leading-none tracking-tight text-primary sm:text-3xl">
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
                            <p className="mb-0.5 text-[9px] font-black uppercase tracking-widest text-outline">Tỷ lệ</p>
                            <p className="font-bold text-on-surface text-sm">{product.scale || 'N/A'}</p>
                        </div>
                        <div className="bg-surface-container-low p-3.5 rounded-xl">
                            <p className="mb-0.5 text-[9px] font-black uppercase tracking-widest text-outline">Tình
                                trạng</p>
                            <p className="font-bold text-on-surface text-sm">{product.stock > 0 ? 'Còn hàng' : 'Hết hàng'}</p>
                        </div>
                    </div>
                    <p className="text-on-surface-variant text-sm font-medium leading-relaxed">
                        {product.description}
                    </p>
                    <div className="flex flex-col gap-3 pt-4 sm:flex-row sm:items-center sm:gap-4">
                        <div className="flex items-center bg-surface-container rounded-full px-1.5 py-1.5">
                            <button onClick={decreaseQuantity}
                                    className="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container-lowest text-on-surface shadow-sm transition-colors hover:text-primary disabled:opacity-50 sm:h-9 sm:w-9">
                                <Minus className="w-4 h-4"/>
                            </button>
                            <span className="w-10 text-center text-sm font-black leading-none text-on-surface sm:w-12 sm:text-base">{quantity}</span>
                            <button onClick={increaseQuantity}
                                    className="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container-lowest text-on-surface shadow-sm transition-colors hover:text-primary sm:h-9 sm:w-9">
                                <Plus className="w-4 h-4"/>
                            </button>
                        </div>
                        <button onClick={handleAddToCart}
                                className="flex h-11 w-full flex-1 items-center justify-center gap-2 rounded-full bg-gradient-to-br from-primary to-primary-container text-sm font-black text-on-primary shadow-[0_10px_20px_-5px_rgba(75,186,254,0.3)] transition-transform hover:scale-[1.01] focus-visible:ring-2 focus-visible:ring-primary/40 sm:h-12">
                            <ShoppingBag className="w-4.5 h-4.5"/> Thêm vào giỏ
                        </button>
                    </div>
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <a
                            href={facebookConsultationUrl || undefined}
                            target={facebookConsultationUrl ? '_blank' : undefined}
                            rel={facebookConsultationUrl ? 'noreferrer' : undefined}
                            aria-disabled={!facebookConsultationUrl}
                            tabIndex={facebookConsultationUrl ? undefined : -1}
                            className={`group inline-flex h-11 items-center justify-center gap-2 rounded-full px-4 text-sm font-black shadow-[0_12px_24px_-8px_rgba(24,119,242,0.5)] transition-all duration-200 focus-visible:ring-2 focus-visible:ring-primary/40 ${
                                facebookConsultationUrl
                                    ? 'cursor-pointer bg-[#1877F2] text-white hover:-translate-y-0.5 hover:bg-[#166FE5] hover:shadow-[0_16px_30px_-8px_rgba(24,119,242,0.62)] active:translate-y-0'
                                    : 'pointer-events-none bg-surface-container text-outline shadow-none'
                            }`}
                        >
                            <MessageCircle className="h-4 w-4 transition-transform duration-200 group-hover:scale-110"/>
                            {'Tư vấn Messenger'}
                        </a>
                        <a
                            href={zaloConsultationUrl || undefined}
                            target={zaloConsultationUrl ? '_blank' : undefined}
                            rel={zaloConsultationUrl ? 'noreferrer' : undefined}
                            aria-disabled={!zaloConsultationUrl}
                            tabIndex={zaloConsultationUrl ? undefined : -1}
                            className={`group inline-flex h-11 items-center justify-center gap-2 rounded-full px-4 text-sm font-black shadow-[0_12px_24px_-8px_rgba(0,104,255,0.5)] transition-all duration-200 focus-visible:ring-2 focus-visible:ring-primary/40 ${
                                zaloConsultationUrl
                                    ? 'cursor-pointer bg-[#0068FF] text-white hover:-translate-y-0.5 hover:bg-[#005CE0] hover:shadow-[0_16px_30px_-8px_rgba(0,104,255,0.62)] active:translate-y-0'
                                    : 'pointer-events-none bg-surface-container text-outline shadow-none'
                            }`}
                        >
                            <span className="flex h-4 w-4 items-center justify-center text-base leading-none transition-transform duration-200 group-hover:scale-110">
                                {createElement(SiZalo as any, {'aria-hidden': true})}
                            </span>
                            {'Tư vấn Zalo'}
                        </a>
                    </div>
                    <ul className="flex flex-col gap-3 border-t border-outline-variant/20 pt-5 text-sm font-bold text-on-surface">
                        {[
                            'Bảo hành chính hãng.',
                            'Hoàn trả đổi mới trong vòng 7 ngày.',
                            'Miễn phí giao hàng toàn quốc.',
                        ].map((item) => (
                            <li key={item} className="flex items-start gap-2.5">
                                <Check className="mt-0.5 h-4 w-4 flex-none stroke-[3] text-on-surface"/>
                                <span>{item}</span>
                            </li>
                        ))}
                    </ul>
                    <div className="hidden">
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
                    <a
                        href="#product-reviews"
                        className="text-sm font-bold text-outline hover:text-on-surface pb-3 uppercase tracking-wider transition-colors">
                        Đánh giá khách hàng
                    </a>
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

            <ProductReviewSection product={product}/>

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
