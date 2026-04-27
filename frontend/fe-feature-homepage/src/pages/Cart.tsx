import { Link } from 'react-router-dom';
import { ChevronRight, Trash2, Minus, Plus, ArrowLeft, Truck, CreditCard, ShoppingCart } from 'lucide-react';
import { useCartStore } from '../store/useCartStore';
import {formatCurrency} from "../util/format";

export default function Cart() {
    const { items, removeFromCart, updateQuantity, getTotals } = useCartStore();
    const { subtotal, total, itemCount } = getTotals();

    if (items.length === 0) {
        return (
            <main className="max-w-7xl mx-auto px-6 py-32 flex flex-col items-center justify-center min-h-[60vh]">
                <div className="w-24 h-24 bg-surface-container rounded-full flex items-center justify-center mb-6">
                    <ShoppingCart className="w-10 h-10 text-outline" /> {/* Actually let's use a simpler Icon */}
                </div>
                <h2 className="text-2xl font-bold mb-4">Giỏ hàng trống</h2>
                <p className="text-on-surface-variant mb-8">Bạn chưa có sản phẩm nào trong giỏ hàng.</p>
                <Link to="/products" className="bg-primary text-white px-8 py-3 rounded-full font-bold shadow-md hover:brightness-110 transition-all">
                    Tiếp tục mua sắm
                </Link>
            </main>
        );
    }

    return (
        <main className="max-w-7xl mx-auto px-6 pt-32 pb-24">
            {/* Header */}
            <header className="mb-12">
                <div className="flex items-center gap-2 text-primary font-bold text-sm mb-4">
                    <Link to="/" className="hover:underline">Trang chủ</Link>
                    <ChevronRight className="w-4 h-4" />
                    <span className="text-on-surface-variant font-normal">Giỏ hàng của bạn</span>
                </div>
                <h1 className="text-4xl font-bold tracking-tight text-on-surface mb-2">Giỏ hàng</h1>
                <p className="text-on-surface-variant">Bạn đang có <span className="font-bold text-primary">{itemCount} sản phẩm</span> trong giỏ hàng.</p>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
                {/* Product List Section */}
                <div className="lg:col-span-8 space-y-6">
                    {/* Free Shipping Alert */}
                    {subtotal >= 2000000 && (
                        <div className="bg-primary-container p-6 rounded-lg flex items-center gap-4 border border-primary/10">
                            <div className="bg-primary p-2.5 rounded-full text-white">
                                <Truck className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="font-bold text-on-primary-container">Đơn hàng của bạn được Miễn Phí Vận Chuyển!</p>
                                <p className="text-sm text-on-primary-container opacity-80">Ưu đãi áp dụng cho đơn hàng trên 2.000.000đ.</p>
                            </div>
                        </div>
                    )}

                    {/* Cart Items */}
                    {items.map(({ product, quantity }) => (
                        <div key={product.id} className="bg-white p-6 rounded-xl flex gap-6 items-center border border-outline-variant/30 shadow-sm hover:shadow-md transition-all duration-300">
                            <div className="w-24 h-24 md:w-32 md:h-32 rounded-lg overflow-hidden bg-surface-container flex-shrink-0 p-2">
                                <img className="w-full h-full object-contain" src={product.imageUrl} alt={product.name} />
                            </div>

                            <div className="flex-grow flex flex-col h-full justify-between">
                                <div>
                                    <div className="flex justify-between items-start mb-1">
                                        <h3 className="font-bold text-base md:text-lg text-on-surface line-clamp-2 md:line-clamp-1 pr-4">{product.name}</h3>
                                        <button
                                            onClick={() => removeFromCart(product.id)}
                                            className="text-on-surface-variant hover:text-error transition-colors p-1"
                                        >
                                            <Trash2 className="w-5 h-5" />
                                        </button>
                                    </div>
                                    <p className="text-on-surface-variant text-[10px] md:text-sm mb-4">
                                        {product.scale ? `Tỷ lệ: ${product.scale} | ` : ''}
                                        {product.brand !== 'N/A' && `Thương hiệu: ${product.brand} | `}
                                        Mã: {product.id}
                                    </p>
                                </div>

                                <div className="flex justify-between items-end md:items-center mt-auto">
                                    <div className="flex items-center bg-surface-container-low border border-outline-variant/30 rounded-lg px-2 py-1">
                                        <button
                                            onClick={() => updateQuantity(product.id, quantity - 1)}
                                            className="w-6 h-6 md:w-8 md:h-8 flex items-center justify-center text-on-surface hover:text-primary disabled:opacity-50"
                                            disabled={quantity <= 1}
                                        >
                                            <Minus className="w-3 h-3 md:w-4 md:h-4" />
                                        </button>
                                        <span className="font-bold text-sm min-w-[1.5rem] md:min-w-[2rem] text-center">{quantity}</span>
                                        <button
                                            onClick={() => updateQuantity(product.id, quantity + 1)}
                                            className="w-6 h-6 md:w-8 md:h-8 flex items-center justify-center text-on-surface hover:text-primary"
                                        >
                                            <Plus className="w-3 h-3 md:w-4 md:h-4" />
                                        </button>
                                    </div>
                                    <p className="text-lg md:text-xl font-black text-primary">
                                        {formatCurrency(product.price * quantity)}
                                    </p>
                                </div>
                            </div>
                        </div>
                    ))}

                    <div className="pt-4">
                        <Link to="/products" className="flex items-center gap-2 text-primary font-bold hover:underline w-fit">
                            <ArrowLeft className="w-4 h-4" />
                            Tiếp tục mua sắm
                        </Link>
                    </div>
                </div>

                {/* Summary & Checkout Section */}
                <aside className="lg:col-span-4 lg:sticky lg:top-28">
                    <div className="bg-white p-8 rounded-xl border border-outline-variant/30 shadow-sm">
                        <h2 className="text-xl font-bold mb-6 text-on-surface">Chi tiết đơn hàng</h2>

                        <div className="space-y-4 mb-8">
                            <div className="flex justify-between text-on-surface-variant text-sm border-b border-outline-variant/20 pb-4">
                                <span>Tạm tính ({itemCount} sản phẩm)</span>
                                <span className="font-semibold text-on-surface">{formatCurrency(subtotal)}</span>
                            </div>
                            <div className="flex justify-between text-on-surface-variant text-sm border-b border-outline-variant/20 pb-4">
                                <span>Phí vận chuyển</span>
                                {subtotal >= 2000000 ? (
                                    <span className="text-primary font-bold uppercase text-[10px] tracking-wider">Miễn phí</span>
                                ) : (
                                    <span className="font-semibold text-on-surface">{formatCurrency(35000)}</span>
                                )}
                            </div>

                            <div className="pt-4 flex flex-col gap-2">
                                <div className="flex justify-between items-end">
                                    <span className="font-bold text-on-surface">Tổng cộng</span>
                                    <span className="text-2xl font-black text-primary">
                    {formatCurrency(subtotal >= 2000000 ? subtotal : subtotal + 35000)}
                  </span>
                                </div>
                                <p className="text-[10px] text-right text-on-surface-variant">(Đã bao gồm VAT 10%)</p>
                            </div>
                        </div>

                        <div className="space-y-4">
                            <button className="w-full py-4 px-8 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/20 hover:brightness-110 transition-all duration-300 flex justify-center items-center gap-3 active:scale-95">
                                <span>Thanh toán ngay</span>
                                <CreditCard className="w-5 h-5" />
                            </button>

                            <p className="text-[10px] text-on-surface-variant text-center px-4 leading-relaxed">
                                Bằng việc nhấn Thanh toán, bạn đồng ý với <Link to="#" className="underline hover:text-primary">Điều khoản</Link> & <Link to="#" className="underline hover:text-primary">Chính sách bảo mật</Link> của SOBU.
                            </p>
                        </div>
                    </div>
                </aside>
            </div>
        </main>
    );
}