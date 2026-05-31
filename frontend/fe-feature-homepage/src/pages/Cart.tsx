import {useState, useEffect} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {Trash2, Minus, Plus, ShoppingBag} from 'lucide-react';
import {useCartStore} from '../store/useCartStore';
import {useAuthStore} from '../store/useAuthStore';
import {formatCurrency} from "../util/format";

interface QuantityControllerProps {
    quantity: number;
    onIncrease: () => void;
    onDecrease: () => void;
    onChange: (value: number) => void;
}

function QuantityController({quantity, onIncrease, onDecrease, onChange}: QuantityControllerProps) {
    const [inputValue, setInputValue] = useState(quantity.toString());

    useEffect(() => {
        setInputValue(quantity.toString());
    }, [quantity]);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        // Chỉ cho phép nhập chuỗi rỗng (khi xóa) hoặc các ký tự là số tự nhiên
        if (val === '' || /^\d+$/.test(val)) {
            setInputValue(val);
            const num = parseInt(val, 10);
            if (!isNaN(num) && num > 0) {
                onChange(num);
            }
        }
    };

    const handleInputBlur = () => {
        const num = parseInt(inputValue, 10);
        // Nếu click ra ngoài khi đang để trống hoặc số không hợp lệ -> reset về 1
        if (isNaN(num) || num <= 0) {
            setInputValue('1');
            onChange(1);
        }
    };

    return (
        <div
            className="flex items-center bg-surface-container rounded-full px-1 py-1 w-fit border border-surface-container-high/40">
            <button
                onClick={onDecrease}
                className="w-6 h-6 bg-white rounded-full flex items-center justify-center shadow-sm text-on-surface hover:text-primary transition-colors disabled:opacity-50"
                disabled={quantity <= 1}
            >
                <Minus className="w-3 h-3"/>
            </button>
            <input
                type="text"
                value={inputValue}
                onChange={handleInputChange}
                onBlur={handleInputBlur}
                className="w-9 text-center font-black text-xs bg-transparent border-none outline-none focus:ring-0 p-0 text-on-surface"
            />
            <button
                onClick={onIncrease}
                className="w-6 h-6 bg-white rounded-full flex items-center justify-center shadow-sm text-on-surface hover:text-primary transition-colors"
            >
                <Plus className="w-3 h-3"/>
            </button>
        </div>
    );
}

export default function Cart() {
    const {items, removeFromCart, updateQuantity, getTotals} = useCartStore();
    const {subtotal, itemCount} = getTotals();
    const {isAuthenticated} = useAuthStore();
    const navigate = useNavigate();

    if (items.length === 0) {
        return (
            <main
                className="max-w-screen-2xl mx-auto px-6 py-32 flex flex-col items-center justify-center min-h-[60vh] bg-surface">
                <div
                    className="w-24 h-24 bg-surface-container rounded-full flex items-center justify-center mb-6 shadow-inner">
                    <ShoppingBag className="w-10 h-10 text-primary opacity-40"/>
                </div>
                <h2 className="text-2xl font-black mb-2 text-on-surface">Giỏ hàng trống</h2>
                <p className="text-on-surface-variant font-medium text-xs mb-8">Bạn chưa chọn siêu phẩm nào vào bộ sưu
                    tập.</p>
                <Link to="/products"
                      className="bg-primary text-white px-8 py-3 rounded-xl font-bold shadow-md hover:scale-[1.02] transition-transform uppercase tracking-wider text-xs">
                    Khám phá cửa hàng
                </Link>
            </main>
        );
    }

    return (
        <main className="max-w-screen-2xl mx-auto px-6 pt-24 pb-16 bg-surface">
            <header className="mb-8 flex items-center gap-3">
                <h1 className="text-2xl md:text-3xl font-black tracking-tight text-on-surface uppercase">Giỏ hàng</h1>
                <div className="w-[1px] h-6 bg-surface-container-highest"></div>
                <span className="text-primary font-black text-sm">{itemCount} Siêu phẩm</span>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">

                {/* Form thông tin Checkout */}
                <div className="lg:col-span-7 space-y-8">
                    <section>
                        <h2 className="text-base font-black mb-4 uppercase tracking-tight text-on-surface">Thông tin
                            liên hệ</h2>
                        <div className="space-y-3">
                            <input type="text" placeholder="Họ và tên khách hàng"
                                   className="w-full bg-surface-container-lowest border border-surface-container px-4 py-2.5 rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium"/>
                            <input type="tel" placeholder="Số điện thoại di động"
                                   className="w-full bg-surface-container-lowest border border-surface-container px-4 py-2.5 rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium"/>
                        </div>
                    </section>

                    <section>
                        <h2 className="text-base font-black mb-4 uppercase tracking-tight text-on-surface">Thông tin vận
                            chuyển</h2>
                        <div className="space-y-3">
                            <input type="text" placeholder="Địa chỉ giao hàng chi tiết (Số nhà, tên đường...)"
                                   className="w-full bg-surface-container-lowest border border-surface-container px-4 py-2.5 rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium"/>
                            <div className="grid grid-cols-2 gap-3">
                                <input type="text" placeholder="Tỉnh / Thành phố"
                                       className="w-full bg-surface-container-lowest border border-surface-container px-4 py-2.5 rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium"/>
                                <input type="text" placeholder="Quận / Huyện"
                                       className="w-full bg-surface-container-lowest border border-surface-container px-4 py-2.5 rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium"/>
                            </div>
                            <textarea placeholder="Ghi chú đóng gói hoặc lưu ý giao hàng cho SPX..."
                                      className="w-full bg-surface-container-lowest border border-surface-container px-4 py-2.5 rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium min-h-[90px]"/>
                        </div>
                    </section>
                </div>

                {/* Tóm tắt đơn hàng & Danh sách Item sửa đổi số lượng */}
                <aside className="lg:col-span-5">
                    <div
                        className="bg-surface-container-lowest p-5 rounded-2xl border border-surface-container/60 shadow-sm">
                        <h3 className="text-sm font-black uppercase tracking-wider mb-4 pb-3 border-b border-surface-container-high">Đơn
                            hàng của bạn</h3>
                        <div className="space-y-4 mb-5 max-h-[320px] overflow-y-auto pr-1 scrollbar-hide">
                            {items.map(({product, quantity}) => (
                                <div key={product.id} className="flex gap-3.5 items-center">
                                    <div
                                        className="w-14 h-14 rounded-xl bg-surface-container-low p-1.5 flex-shrink-0 flex items-center justify-center">
                                        <img src={product.imageUrl} alt={product.name}
                                             className="w-full h-full object-contain"/>
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <h4 className="font-bold text-on-surface text-xs line-clamp-1 leading-tight">{product.name}</h4>
                                        <div className="mt-1.5">
                                            <QuantityController
                                                quantity={quantity}
                                                onIncrease={() => updateQuantity(product.id, quantity + 1)}
                                                onDecrease={() => updateQuantity(product.id, Math.max(1, quantity - 1))}
                                                onChange={(num) => updateQuantity(product.id, num)}
                                            />
                                        </div>
                                    </div>
                                    <div className="text-right shrink-0 flex flex-col items-end justify-between h-12">
                                        <p className="font-black text-primary text-xs">{formatCurrency(product.price * quantity)}</p>
                                        <button onClick={() => removeFromCart(product.id)}
                                                className="text-[10px] text-error uppercase font-black hover:underline p-0.5 flex items-center gap-1">
                                            <Trash2 className="w-3 h-3"/> Xóa
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                        <div className="space-y-2.5 pt-4 border-t border-surface-container-high text-xs font-bold">
                            <div className="flex justify-between text-on-surface-variant">
                                <span>Tạm tính</span>
                                <span>{formatCurrency(subtotal)}</span>
                            </div>
                            <div className="flex justify-between text-on-surface-variant">
                                <span>Phí giao hàng (Ước tính)</span>
                                <span className="text-primary uppercase text-[10px] tracking-wider">Miễn phí</span>
                            </div>
                            <div
                                className="flex justify-between items-end pt-3 border-t border-dashed border-surface-container-high mt-2">
                                <span className="text-sm font-black uppercase">Tổng thanh toán</span>
                                <span
                                    className="text-2xl font-black text-primary tracking-tight">{formatCurrency(subtotal)}</span>
                            </div>
                        </div>

                        <button
                            onClick={(e) => {
                                if (!isAuthenticated) {
                                    e.preventDefault();
                                    navigate('/login');
                                }
                            }}
                            className="w-full mt-6 py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl font-black uppercase tracking-widest text-xs shadow-md shadow-primary/10 hover:scale-[1.01] transition-transform cursor-pointer"
                        >
                            Xác nhận đặt hàng
                        </button>

                        <div className="mt-3 text-center">
                            <Link to="/products"
                                  className="text-xs font-bold text-outline hover:text-primary transition-colors underline">
                                Tiếp tục mua mô hình khác
                            </Link>
                        </div>
                    </div>
                </aside>
            </div>
        </main>
    );
}