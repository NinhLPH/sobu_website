import React, {useState} from 'react';
import {Link} from 'react-router-dom';
import {
    Search,
    Loader2,
    ArrowLeft,
    ChevronRight,
    Package,
    MapPin,
    Calendar,
    DollarSign,
    ShieldCheck
} from 'lucide-react';
import {CustomerService} from '../service/custom.service';
import {OrderResponseDto} from '../interface/order.model';
import {formatCurrency} from '../util/format';

export default function OrderTracking() {
    const [orderId, setOrderId] = useState('');
    const [orderDetail, setOrderDetail] = useState<OrderResponseDto | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [trackingError, setTrackingError] = useState<string | null>(null);

    const handleSearch = async (e: React.FormEvent) => {
        e.preventDefault();
        setTrackingError(null);
        setOrderDetail(null);

        if (!orderId.trim()) {
            setTrackingError('Vui lòng nhập mã đơn hàng!');
            return;
        }

        setIsLoading(true);
        try {
            const data = await CustomerService.getOrderByNhanhId(orderId.trim());
            setOrderDetail(data);
        } catch (err: any) {
            setTrackingError('Không tìm thấy đơn hàng với mã vừa nhập');
        } finally {
            setIsLoading(false);
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'NEW':
                return 'Mới nhận';
            case 'PENDING':
                return 'Chờ xác nhận';
            case 'PROCESSING':
                return 'Đang xử lý';
            case 'SHIPPED':
                return 'Đang giao hàng';
            case 'DELIVERED':
                return 'Đã giao hàng thành công';
            case 'CANCELLED':
                return 'Đã hủy đơn';
            default:
                return status;
        }
    };

    const getStatusStep = (status: string) => {
        switch (status) {
            case 'NEW':
                return 1;
            case 'PENDING':
                return 1;
            case 'PROCESSING':
                return 2;
            case 'SHIPPED':
                return 3;
            case 'DELIVERED':
                return 4;
            default:
                return 0;
        }
    };

    const currentStep = orderDetail ? getStatusStep(orderDetail.status) : 0;

    return (
        <main className="max-w-4xl mx-auto px-6 pt-32 pb-24 bg-surface">
            {/* Breadcrumb */}
            <nav className="flex items-center gap-2 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5"/>
                <span className="text-primary">Tra cứu đơn hàng</span>
            </nav>

            <div className="text-center max-w-xl mx-auto mb-10">
                <h1 className="text-3xl font-black text-on-surface uppercase tracking-tight mb-2">Tra Cứu Đơn Hàng
                    ERP</h1>
                <p className="text-xs text-outline font-bold">Nhập mã đơn hàng từ Nhanh.vn hoặc mã hệ thống SOBU để kiểm
                    tra tiến trình giao hàng và hóa đơn cọc của bạn.</p>
            </div>

            {/* Tracking Search Input Form */}
            <form onSubmit={handleSearch} className="max-w-xl mx-auto mb-12">
                <div
                    className="flex gap-3 bg-surface-container-lowest rounded-2xl p-2 shadow-md border border-surface-container/60">
                    <div className="flex-1 flex items-center gap-3 pl-3">
                        <Search className="text-outline w-5 h-5"/>
                        <input
                            type="text"
                            placeholder="Mã đơn hàng Nhanh ID hoặc Hệ thống ID..."
                            value={orderId}
                            onChange={(e) => setOrderId(e.target.value)}
                            disabled={isLoading}
                            className="bg-transparent border-none outline-none w-full text-xs font-semibold text-on-surface placeholder:text-outline/40"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="px-6 py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl text-xs font-black uppercase tracking-widest hover:scale-102 transition-transform disabled:opacity-50 flex items-center gap-1.5 cursor-pointer"
                    >
                        {isLoading ? (
                            <Loader2 className="w-4 h-4 animate-spin"/>
                        ) : (
                            <span>Tra cứu</span>
                        )}
                    </button>
                </div>

                {trackingError && (
                    <p className="text-error text-xs font-bold mt-3 text-center pl-1">
                        ⚠️ {trackingError}
                    </p>
                )}
            </form>

            {/* Order Detail View */}
            {orderDetail && (
                <div
                    className="bg-surface-container-lowest rounded-[2rem] p-6 sm:p-8 shadow-md border border-surface-container/60 space-y-8 animate-in fade-in zoom-in-95 duration-300">

                    {/* Header info */}
                    <div
                        className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-surface-container pb-6">
                        <div>
                            <span
                                className="text-[10px] bg-primary/10 text-primary font-black px-3 py-1 rounded-full uppercase tracking-wider">
                                Mã đơn: #{orderDetail.orderCode || orderDetail.id}
                            </span>
                            {orderDetail.nhanhOrderCode && (
                                <p className="text-xs text-outline font-bold mt-2">Mã vận đơn Nhanh.vn: <strong
                                    className="text-on-surface">{orderDetail.nhanhOrderCode}</strong></p>
                            )}
                        </div>
                        <div className="text-right">
                            <p className="text-[10px] text-outline font-black uppercase mb-1">Trạng thái hiện tại</p>
                            <span
                                className="text-xs bg-primary text-white font-black px-3 py-1.5 rounded-full uppercase tracking-wider">
                                {getStatusText(orderDetail.status)}
                            </span>
                        </div>
                    </div>

                    {/* Progress tracking bar */}
                    {orderDetail.status !== 'CANCELLED' && currentStep > 0 && (
                        <div className="relative py-4 max-w-2xl mx-auto">
                            <div
                                className="absolute top-1/2 left-0 w-full h-1 bg-surface-container -translate-y-1/2 z-0 rounded-full"/>
                            <div
                                className="absolute top-1/2 left-0 h-1 bg-primary -translate-y-1/2 z-0 rounded-full transition-all duration-500"
                                style={{width: `${((currentStep - 1) / 3) * 100}%`}}
                            />

                            <div className="relative z-10 flex justify-between">
                                {[
                                    {step: 1, label: 'Nhận đơn'},
                                    {step: 2, label: 'Đang xử lý'},
                                    {step: 3, label: 'Đang giao'},
                                    {step: 4, label: 'Đã nhận'}
                                ].map((s) => {
                                    const isCompleted = currentStep >= s.step;
                                    const isActive = currentStep === s.step;
                                    return (
                                        <div key={s.step} className="flex flex-col items-center">
                                            <div
                                                className={`w-8 h-8 rounded-full flex items-center justify-center font-black text-xs transition-all ${
                                                    isCompleted
                                                        ? 'bg-primary text-white ring-4 ring-primary/20'
                                                        : 'bg-surface-container text-outline'
                                                }`}
                                            >
                                                {s.step}
                                            </div>
                                            <span
                                                className={`text-[10px] font-black uppercase tracking-wider mt-2.5 ${isCompleted ? 'text-primary' : 'text-outline/60'}`}>
                                                {s.label}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {/* Content details split */}
                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 pt-4">

                        {/* Products list */}
                        <div className="lg:col-span-7 space-y-4">
                            <h3 className="text-xs font-black text-on-surface uppercase tracking-wider flex items-center gap-1.5">
                                <Package className="w-4 h-4 text-primary"/> Sản phẩm trong đơn</h3>
                            <div className="space-y-3">
                                {orderDetail.items?.map((item, idx) => (
                                    <div key={idx}
                                         className="p-4 bg-surface-container rounded-2xl flex justify-between items-center gap-4 font-bold text-xs">
                                        <div>
                                            <p className="text-on-surface leading-tight">{item.name}</p>
                                            {item.nhanhProductId &&
                                                <p className="text-[9px] text-outline font-medium mt-0.5">SKU: {item.nhanhProductId}</p>}
                                        </div>
                                        <div className="text-right shrink-0 flex flex-col items-end gap-0.5">
                                            <span
                                                className="text-primary font-black">{formatCurrency(item.price * item.quantity)}</span>
                                            <span className="text-[10px] text-outline">Số lượng: x{item.quantity}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Customer billing summary */}
                        <div className="lg:col-span-5 space-y-5">
                            <h3 className="text-xs font-black text-on-surface uppercase tracking-wider flex items-center gap-1.5">
                                <MapPin className="w-4 h-4 text-primary"/> Thông tin giao nhận</h3>
                            <div
                                className="bg-surface-container p-5 rounded-2xl text-xs space-y-3 font-bold text-outline">
                                <div>
                                    <p className="text-[9px] uppercase tracking-wider text-outline/70">Người nhận
                                        hàng</p>
                                    <p className="text-on-surface font-black text-sm mt-0.5">{orderDetail.customerName || 'Khách hàng SOBU'}</p>
                                </div>
                                <div>
                                    <p className="text-[9px] uppercase tracking-wider text-outline/70">Số điện thoại</p>
                                    <p className="text-on-surface font-bold mt-0.5">{orderDetail.customerMobile}</p>
                                </div>
                                <div>
                                    <p className="text-[9px] uppercase tracking-wider text-outline/70">Địa chỉ giao
                                        hàng</p>
                                    <p className="text-on-surface font-semibold mt-0.5 leading-relaxed">{orderDetail.customerAddress || 'Nhận tại xưởng SOBU'}</p>
                                </div>
                                <div className="border-t border-outline-variant/20 pt-3.5 space-y-2">
                                    <div className="flex justify-between font-bold">
                                        <span>Chi phí gốc đơn hàng:</span>
                                        <span
                                            className="text-on-surface">{formatCurrency(orderDetail.totalAmount)}</span>
                                    </div>
                                    <div className="flex justify-between font-bold">
                                        <span>Đã cọc trước (Deposit):</span>
                                        <span
                                            className="text-on-surface">{formatCurrency(orderDetail.depositAmount)}</span>
                                    </div>
                                    <div
                                        className="flex justify-between font-black text-primary text-base pt-2 border-t border-outline-variant/20">
                                        <span>Tổng tiền cần thanh toán:</span>
                                        <span>{formatCurrency(orderDetail.totalAmount - orderDetail.depositAmount)}</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            )}
        </main>
    );
}
