import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import {
    CreditCard,
    ChevronRight,
    ExternalLink,
    Loader2,
    MapPin,
    Package,
    XCircle,
    RefreshCw,
    Search
} from 'lucide-react';
import { CustomerService } from '../service/custom.service';
import {
    OrderPaymentResponseDto,
    OrderPaymentType,
    OrderResponseDto
} from '../interface/order.model';
import { PaymentMethod } from '../enum/union-types';
import { usePaymentStore } from '../store/usePaymentStore';
import { formatCurrency } from '../utils/format';
import { redirectToPaymentCheckout } from '../utils/payment-session';

type TrackingType = 'internal' | 'nhanh';

const getStatusText = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
            return 'Mới nhận';
        case 'WAITING_DEPOSIT':
            return 'Chờ thanh toán đặt cọc';
        case 'DEPOSIT_PAID':
            return 'Đã nhận cọc';
        case 'READY_FOR_FINAL_PAYMENT':
            return 'Chờ thanh toán cuối';
        case 'PROCESSING':
            return 'Đang xử lý';
        case 'SHIPPED':
            return 'Đang giao hàng';
        case 'DELIVERED':
            return 'Đã giao hàng';
        case 'CANCELLED':
            return 'Đã hủy đơn';
        default:
            return status || 'Chưa cập nhật';
    }
};

const getStatusStep = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
        case 'WAITING_DEPOSIT':
            return 1;
        case 'DEPOSIT_PAID':
        case 'READY_FOR_FINAL_PAYMENT':
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

const getPaymentStatusText = (status: string) => {
    switch (status) {
        case 'PENDING':
            return 'Chờ thanh toán';
        case 'PAID':
            return 'Đã thanh toán';
        case 'FAILED':
            return 'Thất bại';
        case 'CANCELLED':
            return 'Đã hủy';
        case 'EXPIRED':
            return 'Hết hạn';
        case 'REFUNDED':
            return 'Đã hoàn tiền';
        default:
            return status;
    }
};

const getPaymentStatusColor = (status: string) => {
    switch (status) {
        case 'PAID':
            return 'bg-green-100 text-green-700';
        case 'PENDING':
            return 'bg-amber-100 text-amber-700';
        case 'FAILED':
        case 'CANCELLED':
        case 'EXPIRED':
            return 'bg-red-100 text-red-700';
        default:
            return 'bg-surface-container text-outline';
    }
};

export const getAvailablePaymentTypes = (order: OrderResponseDto): OrderPaymentType[] => {
    if (order.status === 'CANCELLED' || order.paymentStatus === 'PAID') {
        return [];
    }
    if (order.type === 'PREORDER') {
        if (order.status === 'WAITING_DEPOSIT') {
            return ['DEPOSIT'];
        }
        if (order.status === 'READY_FOR_FINAL_PAYMENT') {
            return ['FINAL'];
        }
        return [];
    }
    return order.type === 'NORMAL' ? ['FULL'] : [];
};

const canCancelOrder = (order: OrderResponseDto) => {
    return !['SHIPPED', 'DELIVERED', 'CANCELLED'].includes(order.status || '');
};

const paymentTypeLabels: Record<OrderPaymentType, string> = {
    FULL: 'Toàn bộ',
    DEPOSIT: 'Đặt cọc',
    FINAL: 'Phần còn lại',
    REFUND: 'Hoàn tiền'
};

export default function OrderTracking() {
    const { orderId: routeOrderId } = useParams<{ orderId?: string }>();
    const [searchParams] = useSearchParams();
    const initialOrderId = routeOrderId || searchParams.get('orderId') || '';
    const initialNhanhOrderId = searchParams.get('nhanhOrderId') || '';
    const initialReference = initialOrderId || initialNhanhOrderId;
    const initialTrackingType: TrackingType = initialOrderId ? 'internal' : 'nhanh';
    const initialPaymentSetup = searchParams.get('paymentSetup');
    const isPaymentSetupFailed = initialPaymentSetup === 'failed';
    const [reference, setReference] = useState(initialReference);
    const [trackingType, setTrackingType] = useState<TrackingType>(
        initialTrackingType
    );
    const [orderDetail, setOrderDetail] = useState<OrderResponseDto | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [trackingError, setTrackingError] = useState<string | null>(null);
    const [paymentType, setPaymentType] = useState<OrderPaymentType>('FULL');
    const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('ONLINE');
    const [paymentMessage, setPaymentMessage] = useState<string | null>(() => {
        if (initialPaymentSetup === 'cod') {
            return 'Đã ghi nhận phương thức COD cho đơn hàng.';
        }
        if (isPaymentSetupFailed) {
            return 'Đơn hàng đã được tạo nhưng chưa thể khởi tạo thanh toán. Giỏ hàng vẫn được giữ nguyên; vui lòng thử lại bên dưới.';
        }
        return null;
    });
    const [isCancellingOrder, setIsCancellingOrder] = useState(false);
    const [cancelMessage, setCancelMessage] = useState<string | null>(null);
    const autoTrackedReference = useRef<string | null>(null);
    const {
        payments,
        isLoadingPayments,
        isCreatingPayment,
        paymentError,
        fetchPayments,
        createPayment,
        clearPaymentError,
        clearPayments
    } = usePaymentStore();

    const trackOrder = useCallback(async (value: string, type: TrackingType) => {
        setIsLoading(true);
        setTrackingError(null);
        setOrderDetail(null);
        setCancelMessage(null);
        clearPayments();

        try {
            const response = type === 'internal'
                ? await CustomerService.getMyOrder(value)
                : await CustomerService.getOrderByNhanhId(value);
            setOrderDetail(response.data);
            const availableTypes = getAvailablePaymentTypes(response.data);
            if (availableTypes.length > 0) {
                setPaymentType(availableTypes[0]);
                setPaymentMethod('ONLINE');
            }
            await fetchPayments(response.data.id);
        } catch (error: any) {
            const status = error?.response?.status;
            setTrackingError(
                status === 404
                    ? 'Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn.'
                    : error?.response?.data?.message ||
                        error?.message ||
                        'Không thể tra cứu đơn hàng lúc này.'
            );
        } finally {
            setIsLoading(false);
        }
    }, [clearPayments, fetchPayments]);

    useEffect(() => {
        if (!initialReference || autoTrackedReference.current === initialReference) {
            return;
        }
        autoTrackedReference.current = initialReference;
        trackOrder(initialReference, initialTrackingType);
    }, [initialReference, initialTrackingType, trackOrder]);

    useEffect(() => () => {
        clearPayments();
    }, [clearPayments]);

    const handleSearch = async (event: FormEvent) => {
        event.preventDefault();
        const value = reference.trim();
        if (!value) {
            setTrackingError('Vui lòng nhập mã đơn hàng.');
            return;
        }
        await trackOrder(value, trackingType);
    };

    const handleCreatePayment = async () => {
        if (!orderDetail) {
            return;
        }
        setPaymentMessage(null);
        clearPaymentError();

        const pendingPayment = payments.find(payment =>
            payment.type === paymentType && payment.status === 'PENDING'
        );
        if (pendingPayment?.checkoutUrl) {
            redirectToPaymentCheckout(pendingPayment);
            return;
        }
        if (pendingPayment) {
            setPaymentMessage('Đơn hàng đã có một giao dịch đang chờ xử lý.');
            return;
        }

        try {
            const payment = await createPayment(orderDetail.id, {
                type: paymentType,
                paymentMethod
            });
            if (paymentMethod === 'ONLINE' && payment.checkoutUrl) {
                redirectToPaymentCheckout(payment);
                return;
            }
            setPaymentMessage(
                paymentMethod === 'COD'
                    ? 'Đã ghi nhận phương thức COD cho đơn hàng.'
                    : 'Đã tạo phiên thanh toán.'
            );
            await trackOrder(String(orderDetail.id), 'internal');
        } catch {
            // The payment store exposes the backend error below.
        }
    };

    const handleCancelOrder = async () => {
        if (!orderDetail || !window.confirm('Bạn chắc chắn muốn hủy đơn hàng này?')) {
            return;
        }

        setIsCancellingOrder(true);
        setCancelMessage(null);
        setTrackingError(null);
        try {
            const response = await CustomerService.cancelOrder(orderDetail.id);
            setOrderDetail(response.data);
            setCancelMessage('Đơn hàng đã được hủy.');
            await fetchPayments(response.data.id);
        } catch (error: any) {
            try {
                const refreshed = await CustomerService.getMyOrder(orderDetail.id);
                if (refreshed.data.status === 'CANCELLED') {
                    setOrderDetail(refreshed.data);
                    setCancelMessage('Đơn hàng đã được hủy.');
                    try {
                        await fetchPayments(refreshed.data.id);
                    } catch {
                        // Payment history exposes its own error state.
                    }
                    return;
                }
            } catch {
                // Preserve the original cancellation error when reconciliation is unavailable.
            }
            setCancelMessage(
                error?.response?.data?.message ||
                error?.message ||
                'Không thể hủy đơn hàng lúc này.'
            );
        } finally {
            setIsCancellingOrder(false);
        }
    };

    const handleRefreshPayments = async () => {
        if (!orderDetail) {
            return;
        }
        setPaymentMessage(null);
        clearPaymentError();
        try {
            await trackOrder(String(orderDetail.id), 'internal');
        } catch {
            // trackOrder exposes the error state.
        }
    };

    const currentStep = getStatusStep(orderDetail?.status);
    const totalAmount = orderDetail?.totalAmount ?? 0;
    const shippingFee = orderDetail?.shippingFee ?? 0;
    const paidAmount = orderDetail?.paidAmount ?? orderDetail?.depositAmount ?? 0;
    const remainingAmount = orderDetail?.remainingAmount ?? Math.max(0, totalAmount - paidAmount);
    const availablePaymentTypes = orderDetail
        ? getAvailablePaymentTypes(orderDetail)
        : [];
    const selectedPendingPayment = payments.find(payment =>
        payment.type === paymentType &&
        payment.status === 'PENDING'
    );

    return (
        <main className="w-full min-w-0 bg-surface px-4 pb-24 pt-28 sm:px-6 sm:pt-32">
            <nav className="mb-6 flex items-center gap-2 text-xs font-bold text-on-surface-variant">
                <Link to="/" className="transition-colors hover:text-primary">Trang chủ</Link>
                <ChevronRight className="h-3.5 w-3.5" />
                <Link to="/orders" className="transition-colors hover:text-primary">Đơn hàng của tôi</Link>
                <ChevronRight className="h-3.5 w-3.5" />
                <span className="text-primary">Chi tiết đơn hàng</span>
            </nav>

            <div className="mb-10 grid gap-6 lg:grid-cols-2 lg:items-end">
                <div className="text-left">
                    <h1 className="text-3xl font-black uppercase tracking-tight text-on-surface">
                        Tra cứu đơn hàng
                    </h1>
                </div>


            </div>
            <form onSubmit={handleSearch} className="space-y-3 mb-10">
                <div className="grid grid-cols-2 gap-2 rounded-xl bg-surface-container p-1">
                    <button
                        type="button"
                        onClick={() => {
                            setTrackingType('internal');
                            setTrackingError(null);
                        }}
                        disabled={isLoading}
                        className={`rounded-lg px-3 py-2 text-xs font-black transition-colors ${
                            trackingType === 'internal'
                                ? 'bg-white text-primary shadow-sm'
                                : 'text-outline'
                        }`}
                    >
                        ID đơn SOBU
                    </button>
                    <button
                        type="button"
                        onClick={() => {
                            setTrackingType('nhanh');
                            setTrackingError(null);
                        }}
                        disabled={isLoading}
                        className={`rounded-lg px-3 py-2 text-xs font-black transition-colors ${
                            trackingType === 'nhanh'
                                ? 'bg-white text-primary shadow-sm'
                                : 'text-outline'
                        }`}
                    >
                        Nhanh ID / code
                    </button>
                </div>

                <div className="flex gap-3 rounded-2xl border border-surface-container/60 bg-surface-container-lowest p-2 shadow-md">
                    <div className="flex flex-1 items-center gap-3 pl-3">
                        <Search className="h-5 w-5 text-outline" />
                        <input
                            type="text"
                            value={reference}
                            onChange={(event) => {
                                setReference(event.target.value);
                                setTrackingError(null);
                            }}
                            disabled={isLoading}
                            placeholder={trackingType === 'internal' ? 'Ví dụ: 123' : 'Ví dụ: NH001'}
                            className="w-full border-none bg-transparent text-xs font-semibold text-on-surface outline-none placeholder:text-outline/40"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={isLoading || !reference.trim()}
                        className="flex items-center gap-1.5 rounded-xl bg-gradient-to-br from-primary to-primary-container px-6 py-3 text-xs font-black uppercase tracking-widest text-white disabled:opacity-50"
                    >
                        {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Tra cứu'}
                    </button>
                </div>

                {trackingError && (
                    <p className="text-left text-xs font-bold text-error">{trackingError}</p>
                )}
            </form>

            {orderDetail && (
                <section className="space-y-8 rounded-[2rem] border border-surface-container/60 bg-surface-container-lowest p-6 shadow-md sm:p-8">
                    <div className="flex flex-col items-start justify-between gap-4 border-b border-surface-container pb-6 sm:flex-row sm:items-center">
                        <div>
                            <span className="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-primary">
                                Mã đơn: #{orderDetail.orderCode || orderDetail.id}
                            </span>
                            {(orderDetail.nhanhOrderCode || orderDetail.nhanhOrderId) && (
                                <p className="mt-2 text-xs font-bold text-outline">
                                    Nhanh.vn:{' '}
                                    <strong className="text-on-surface">
                                        {orderDetail.nhanhOrderCode || orderDetail.nhanhOrderId}
                                    </strong>
                                </p>
                            )}
                        </div>
                        <div className="text-left sm:text-right">
                            <p className="mb-1 text-[10px] font-black uppercase text-outline">
                                Trạng thái hiện tại
                            </p>
                            <span className="rounded-full bg-primary px-3 py-1.5 text-xs font-black uppercase tracking-wider text-white">
                                {getStatusText(orderDetail.status)}
                            </span>
                        </div>
                    </div>

                    <div className="flex flex-col gap-3 rounded-2xl bg-surface-container p-4 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                            <p className="text-xs font-black uppercase tracking-wider text-on-surface">
                                Hủy đơn hàng
                            </p>
                            <p className="mt-1 text-[11px] font-medium text-outline">
                                Bạn có thể hủy đơn trước khi đơn chuyển sang trạng thái đang giao hàng.
                            </p>
                            {cancelMessage && (
                                <p className={`mt-2 text-xs font-bold ${
                                    orderDetail.status === 'CANCELLED' ? 'text-green-700' : 'text-error'
                                }`}>
                                    {cancelMessage}
                                </p>
                            )}
                        </div>
                        <button
                            type="button"
                            onClick={handleCancelOrder}
                            disabled={!canCancelOrder(orderDetail) || isCancellingOrder}
                            className="flex items-center justify-center gap-2 rounded-xl border border-error/20 px-4 py-2.5 text-[10px] font-black uppercase tracking-wider text-error transition-colors hover:bg-error/10 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {isCancellingOrder ? <Loader2 className="h-4 w-4 animate-spin"/> : <XCircle className="h-4 w-4"/>}
                            {orderDetail.status === 'CANCELLED' ? 'Đã hủy đơn' : 'Hủy đơn'}
                        </button>
                    </div>

                    {orderDetail.status !== 'CANCELLED' && currentStep > 0 && (
                        <div className="relative mx-auto max-w-2xl py-4">
                            <div className="absolute left-0 top-1/2 z-0 h-1 w-full -translate-y-1/2 rounded-full bg-surface-container" />
                            <div
                                className="absolute left-0 top-1/2 z-0 h-1 -translate-y-1/2 rounded-full bg-primary transition-all"
                                style={{ width: `${((currentStep - 1) / 3) * 100}%` }}
                            />
                            <div className="relative z-10 flex justify-between">
                                {[
                                    { step: 1, label: 'Nhận đơn' },
                                    { step: 2, label: 'Xử lý' },
                                    { step: 3, label: 'Đang giao' },
                                    { step: 4, label: 'Đã nhận' }
                                ].map(({ step, label }) => {
                                    const completed = currentStep >= step;
                                    return (
                                        <div key={step} className="flex flex-col items-center">
                                            <div className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-black ${
                                                completed
                                                    ? 'bg-primary text-white ring-4 ring-primary/20'
                                                    : 'bg-surface-container text-outline'
                                            }`}>
                                                {step}
                                            </div>
                                            <span className={`mt-2.5 text-[10px] font-black uppercase ${
                                                completed ? 'text-primary' : 'text-outline/60'
                                            }`}>
                                                {label}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    <div className="grid grid-cols-1 gap-8 pt-4 lg:grid-cols-12">
                        <div className="space-y-4 lg:col-span-7">
                            <h2 className="flex items-center gap-1.5 text-xs font-black uppercase tracking-wider text-on-surface">
                                <Package className="h-4 w-4 text-primary" /> Sản phẩm trong đơn
                            </h2>
                            {orderDetail.items && orderDetail.items.length > 0 ? (
                                <div className="space-y-3">
                                    {orderDetail.items.map((item) => (
                                        <div
                                            key={item.id}
                                            className="flex items-center justify-between gap-4 rounded-2xl bg-surface-container p-4 text-xs font-bold"
                                        >
                                            <div>
                                                <p className="text-on-surface">{item.name}</p>
                                                {item.nhanhProductId && (
                                                    <p className="mt-0.5 text-[9px] font-medium text-outline">
                                                        Nhanh ID: {item.nhanhProductId}
                                                    </p>
                                                )}
                                            </div>
                                            <div className="shrink-0 text-right">
                                                <p className="font-black text-primary">
                                                    {formatCurrency(item.price * item.quantity)}
                                                </p>
                                                <p className="text-[10px] text-outline">x{item.quantity}</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="rounded-xl bg-surface-container p-4 text-xs font-semibold text-outline">
                                    API chưa trả chi tiết sản phẩm cho đơn hàng này.
                                </p>
                            )}
                        </div>

                        <div className="space-y-5 lg:col-span-5">
                            <h2 className="flex items-center gap-1.5 text-xs font-black uppercase tracking-wider text-on-surface">
                                <MapPin className="h-4 w-4 text-primary" /> Thông tin giao nhận
                            </h2>
                            <div className="space-y-3 rounded-2xl bg-surface-container p-5 text-xs font-bold text-outline">
                                <div>
                                    <p className="text-[9px] uppercase">Người nhận</p>
                                    <p className="mt-0.5 text-sm font-black text-on-surface">
                                        {orderDetail.customerName || 'Chưa cập nhật'}
                                    </p>
                                </div>
                                <div>
                                    <p className="text-[9px] uppercase">Số điện thoại</p>
                                    <p className="mt-0.5 text-on-surface">
                                        {orderDetail.customerMobile || 'Chưa cập nhật'}
                                    </p>
                                </div>
                                <div>
                                    <p className="text-[9px] uppercase">Địa chỉ</p>
                                    <p className="mt-0.5 leading-relaxed text-on-surface">
                                        {orderDetail.customerAddress || 'Chưa cập nhật'}
                                    </p>
                                </div>
                                <div className="space-y-2 border-t border-outline-variant/20 pt-3.5">
                                    <div className="flex justify-between">
                                        <span>Tổng đơn:</span>
                                        <span className="text-on-surface">{formatCurrency(totalAmount)}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Phí vận chuyển:</span>
                                        <span className="text-on-surface">{formatCurrency(shippingFee)}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Đã thanh toán:</span>
                                        <span className="text-on-surface">{formatCurrency(paidAmount)}</span>
                                    </div>
                                    <div className="flex justify-between border-t border-outline-variant/20 pt-2 text-base font-black text-primary">
                                        <span>Còn lại:</span>
                                        <span>{formatCurrency(remainingAmount)}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <section className="space-y-5 border-t border-surface-container pt-6">
                        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                            <div>
                                <h2 className="flex items-center gap-2 text-sm font-black uppercase tracking-wider text-on-surface">
                                    <CreditCard className="h-5 w-5 text-primary" />
                                    Lịch sử thanh toán
                                </h2>
                                <p className="mt-1 text-[11px] font-medium text-outline">
                                    Trạng thái đơn hàng được cập nhật từ backend và webhook PayOS.
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={handleRefreshPayments}
                                disabled={isLoadingPayments || isCreatingPayment}
                                className="flex items-center justify-center gap-2 rounded-xl border border-primary/20 px-4 py-2 text-[10px] font-black uppercase text-primary disabled:opacity-50"
                            >
                                <RefreshCw className={`h-3.5 w-3.5 ${isLoadingPayments ? 'animate-spin' : ''}`} />
                                Làm mới
                            </button>
                        </div>

                        {(paymentError || paymentMessage) && (
                            <div className={`rounded-xl px-4 py-3 text-xs font-bold ${
                                paymentError
                                    ? 'border border-error/20 bg-error/10 text-error'
                                    : 'border border-green-200 bg-green-50 text-green-700'
                            }`}>
                                {paymentError || paymentMessage}
                            </div>
                        )}

                        {isPaymentSetupFailed && (
                            <div className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-900">
                                <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
                                <div>
                                    <p className="font-black">Phiên thanh toán chưa được tạo</p>
                                    <p className="mt-1 font-medium">
                                        Đơn hàng đã được lưu và giỏ hàng chưa bị xóa. Bạn có thể tạo lại phiên PayOS ngay tại đây.
                                    </p>
                                </div>
                            </div>
                        )}

                        {availablePaymentTypes.length > 0 && (
                            <div className="grid gap-3 rounded-2xl bg-surface-container p-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
                                <label className="space-y-1.5 text-[10px] font-black uppercase text-outline">
                                    Loại thanh toán
                                    <select
                                        value={paymentType}
                                        onChange={(event) => {
                                            const nextType = event.target.value as OrderPaymentType;
                                            setPaymentType(nextType);
                                            if (nextType === 'DEPOSIT') {
                                                setPaymentMethod('ONLINE');
                                            }
                                            clearPaymentError();
                                        }}
                                        disabled={isCreatingPayment}
                                        className="w-full rounded-xl border border-outline-variant/20 bg-white px-3 py-2.5 text-xs font-bold normal-case text-on-surface outline-none"
                                    >
                                        {availablePaymentTypes.map(type => (
                                            <option key={type} value={type}>
                                                {paymentTypeLabels[type]}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label className="space-y-1.5 text-[10px] font-black uppercase text-outline">
                                    Phương thức
                                    <select
                                        value={paymentMethod}
                                        onChange={(event) => {
                                            setPaymentMethod(event.target.value as PaymentMethod);
                                            clearPaymentError();
                                        }}
                                        disabled={
                                            isCreatingPayment ||
                                            paymentType === 'DEPOSIT' ||
                                            Boolean(selectedPendingPayment)
                                        }
                                        className="w-full rounded-xl border border-outline-variant/20 bg-white px-3 py-2.5 text-xs font-bold normal-case text-on-surface outline-none disabled:opacity-60"
                                    >
                                        <option value="ONLINE">ONLINE - PayOS</option>
                                        {paymentType !== 'DEPOSIT' && (
                                            <option value="COD">COD</option>
                                        )}
                                    </select>
                                </label>
                                <button
                                    type="button"
                                    onClick={handleCreatePayment}
                                    disabled={isCreatingPayment || isLoadingPayments}
                                    className="flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-[10px] font-black uppercase tracking-wider text-white disabled:opacity-50"
                                >
                                    {isCreatingPayment
                                        ? <Loader2 className="h-4 w-4 animate-spin" />
                                        : selectedPendingPayment?.checkoutUrl
                                            ? <ExternalLink className="h-4 w-4" />
                                            : <CreditCard className="h-4 w-4" />}
                                    {selectedPendingPayment?.checkoutUrl
                                        ? 'Tiếp tục thanh toán'
                                        : paymentMethod === 'COD'
                                            ? 'Chọn COD'
                                            : isPaymentSetupFailed
                                                ? 'Thử tạo lại thanh toán'
                                                : 'Thanh toán ngay'}
                                </button>
                            </div>
                        )}

                        {isLoadingPayments && payments.length === 0 ? (
                            <div className="flex items-center justify-center gap-2 py-6 text-xs font-bold text-outline">
                                <Loader2 className="h-4 w-4 animate-spin" />
                                Đang tải lịch sử thanh toán...
                            </div>
                        ) : payments.length > 0 ? (
                            <div className="space-y-3">
                                {payments.map((payment: OrderPaymentResponseDto) => (
                                    <div
                                        key={payment.id}
                                        className="grid gap-3 rounded-2xl border border-surface-container bg-white p-4 text-xs sm:grid-cols-[1fr_auto] sm:items-center"
                                    >
                                        <div>
                                            <div className="flex flex-wrap items-center gap-2">
                                                <span className="font-black text-on-surface">
                                                    {paymentTypeLabels[payment.type]}
                                                </span>
                                                <span className={`rounded-full px-2.5 py-1 text-[9px] font-black uppercase ${getPaymentStatusColor(payment.status)}`}>
                                                    {getPaymentStatusText(payment.status)}
                                                </span>
                                            </div>
                                            <p className="mt-1.5 font-medium text-outline">
                                                {payment.paymentCode} · {payment.paymentMethod} · {formatCurrency(payment.amount)}
                                            </p>
                                            {payment.failureReason && (
                                                <p className="mt-1 text-[10px] font-bold text-error">
                                                    {payment.failureReason}
                                                </p>
                                            )}
                                        </div>
                                        <div className="text-left text-[10px] font-medium text-outline sm:text-right">
                                            <p>
                                                {new Date(payment.createdAt).toLocaleString('vi-VN')}
                                            </p>
                                            {payment.status === 'PENDING' && payment.checkoutUrl && (
                                                <button
                                                    type="button"
                                                    onClick={() => redirectToPaymentCheckout(payment)}
                                                    className="mt-2 inline-flex items-center gap-1 font-black uppercase text-primary hover:underline"
                                                >
                                                    Mở PayOS <ExternalLink className="h-3 w-3" />
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="rounded-xl bg-surface-container p-4 text-xs font-semibold text-outline">
                                Đơn hàng chưa có giao dịch thanh toán.
                            </p>
                        )}
                    </section>
                </section>
            )}
        </main>
    );
}
