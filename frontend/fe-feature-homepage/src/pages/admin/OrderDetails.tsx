import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
    AlertCircle,
    ArrowLeft,
    Banknote,
    CheckCircle2,
    CreditCard,
    Loader2,
    MapPin,
    Package,
    RefreshCw,
    User
} from 'lucide-react';
import { useAdminStore } from '../../store/useAdminStore';
import { formatCurrency } from '../../utils/format';

const getStatusColor = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
            return 'border-amber-200 bg-amber-100 text-amber-800';
        case 'WAITING_DEPOSIT':
            return 'border-yellow-200 bg-yellow-100 text-yellow-800';
        case 'DEPOSIT_PAID':
            return 'border-cyan-200 bg-cyan-100 text-cyan-800';
        case 'READY_FOR_FINAL_PAYMENT':
            return 'border-indigo-200 bg-indigo-100 text-indigo-800';
        case 'PROCESSING':
            return 'border-blue-200 bg-blue-100 text-blue-800';
        case 'SHIPPED':
            return 'border-purple-200 bg-purple-100 text-purple-800';
        case 'DELIVERED':
            return 'border-green-200 bg-green-100 text-green-800';
        case 'CANCELLED':
            return 'border-red-200 bg-red-100 text-red-800';
        default:
            return 'border-gray-200 bg-gray-100 text-gray-800';
    }
};

const getStatusText = (status?: string) => {
    switch (status) {
        case 'PENDING':
        case 'NEW':
            return 'Mới';
        case 'WAITING_DEPOSIT':
            return 'Chờ đặt cọc';
        case 'DEPOSIT_PAID':
            return 'Đã nhận cọc';
        case 'READY_FOR_FINAL_PAYMENT':
            return 'Chờ thanh toán cuối';
        case 'PROCESSING':
            return 'Đang xử lý';
        case 'SHIPPED':
            return 'Đang giao';
        case 'DELIVERED':
            return 'Đã giao';
        case 'CANCELLED':
            return 'Đã hủy';
        default:
            return status || 'Chưa cập nhật';
    }
};

const getSyncStatusColor = (status?: string) => {
    switch (status) {
        case 'SYNCED':
            return 'border-green-200 bg-green-50 text-green-700';
        case 'FAILED':
            return 'border-red-200 bg-red-50 text-red-700';
        case 'PENDING':
            return 'border-amber-200 bg-amber-50 text-amber-700';
        default:
            return 'border-gray-200 bg-gray-50 text-gray-700';
    }
};

const getSyncStatusText = (status?: string) => {
    switch (status) {
        case 'SYNCED':
            return 'Đã đồng bộ';
        case 'FAILED':
            return 'Đồng bộ thất bại';
        case 'PENDING':
            return 'Chờ đồng bộ';
        default:
            return status || 'Chưa cập nhật';
    }
};

export default function AdminOrderDetail() {
    const { id } = useParams();
    const [manualPaymentCode, setManualPaymentCode] = useState('');
    const {
        currentOrderDetail: order,
        adminPayments,
        fetchOrderDetail,
        retryOrderSync,
        createPreorderFinalPayment,
        confirmMockPayment,
        clearCurrentOrder,
        clearOrdersError,
        clearOrderActionMessage,
        isOrderDetailLoading,
        isRetryingOrderSync,
        isCreatingFinalPayment,
        confirmingPaymentCode,
        ordersError,
        orderActionMessage
    } = useAdminStore();

    useEffect(() => {
        if (id) {
            fetchOrderDetail(id);
        }
        return () => {
            clearCurrentOrder();
        };
    }, [id, fetchOrderDetail, clearCurrentOrder]);

    const handleRetrySync = async () => {
        if (!id) {
            return;
        }
        clearOrdersError();
        clearOrderActionMessage();
        try {
            await retryOrderSync(id);
        } catch {
            // The admin store exposes the backend message through ordersError.
        }
    };

    const handleCreateFinalPayment = async () => {
        if (!id) {
            return;
        }
        clearOrdersError();
        clearOrderActionMessage();
        try {
            const payment = await createPreorderFinalPayment(id);
            setManualPaymentCode(payment.paymentCode);
        } catch {
            // The admin store exposes the backend message through ordersError.
        }
    };

    const handleConfirmPayment = async (paymentCode: string) => {
        const normalizedCode = paymentCode.trim();
        if (!normalizedCode) {
            return;
        }
        clearOrdersError();
        clearOrderActionMessage();
        try {
            await confirmMockPayment(normalizedCode);
            setManualPaymentCode('');
        } catch {
            // The admin store exposes the backend message through ordersError.
        }
    };

    if (isOrderDetailLoading && !order) {
        return (
            <div className="flex flex-col items-center justify-center py-24">
                <Loader2 className="mb-4 h-10 w-10 animate-spin text-primary" />
                <p className="text-xs font-bold text-outline">Đang tải chi tiết đơn hàng...</p>
            </div>
        );
    }

    if (!order) {
        return (
            <div className="flex min-h-[50vh] flex-col items-center justify-center p-8 text-center">
                <AlertCircle className="mb-4 h-12 w-12 text-error" />
                <h2 className="text-lg font-black uppercase text-on-surface">
                    Không thể hiển thị đơn hàng
                </h2>
                {ordersError && (
                    <p className="mt-2 text-xs font-bold text-error">{ordersError}</p>
                )}
                <Link to="/admin/orders" className="mt-4 font-bold text-primary hover:underline">
                    Quay lại danh sách đơn hàng
                </Link>
            </div>
        );
    }

    const totalAmount = order.totalAmount ?? 0;
    const shippingFee = order.shippingFee ?? 0;
    const paidAmount = order.paidAmount ?? order.depositAmount ?? 0;
    const remainingAmount = order.remainingAmount ?? Math.max(0, totalAmount - paidAmount);

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-4">
                <Link
                    to="/admin/orders"
                    className="rounded-full border border-outline-variant/30 bg-white p-2 transition-colors hover:bg-surface-variant"
                    aria-label="Quay lại danh sách đơn hàng"
                >
                    <ArrowLeft className="h-5 w-5 text-on-surface" />
                </Link>
                <div>
                    <div className="flex flex-wrap items-center gap-3">
                        <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface">
                            Chi tiết đơn #{order.orderCode || order.id}
                        </h1>
                        <span className={`rounded-full border px-3 py-1 text-[10px] font-black uppercase tracking-wider ${getStatusColor(order.status)}`}>
                            {getStatusText(order.status)}
                        </span>
                    </div>
                    {(order.nhanhOrderCode || order.nhanhOrderId) && (
                        <p className="mt-1 text-[10px] font-bold text-outline">
                            Nhanh.vn:{' '}
                            <strong className="text-on-surface">
                                {order.nhanhOrderCode || order.nhanhOrderId}
                            </strong>
                        </p>
                    )}
                </div>
            </div>

            {orderActionMessage && (
                <div className="flex items-center gap-3 rounded-2xl border border-green-200 bg-green-50 p-4 text-xs font-bold text-green-800">
                    <CheckCircle2 className="h-5 w-5 shrink-0 text-green-600" />
                    {orderActionMessage}
                </div>
            )}

            {ordersError && (
                <div className="flex items-start gap-3 rounded-2xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                    <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
                    {ordersError}
                </div>
            )}

            {order.syncStatus === 'FAILED' && (order.lastSyncMessage || order.syncError) && (
                <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-xs font-medium leading-relaxed text-red-800">
                    <span className="mb-1 block font-black uppercase tracking-wide text-red-600">
                        Chi tiết lỗi đồng bộ
                    </span>
                    {order.lastSyncMessage || order.syncError}
                </div>
            )}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                <div className="space-y-6 lg:col-span-2">
                    <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                        <header className="flex items-center gap-2 border-b border-outline-variant/20 bg-surface-container-lowest p-4">
                            <Package className="h-5 w-5 text-primary" />
                            <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                Sản phẩm
                            </h2>
                        </header>
                        <div className="overflow-x-auto p-4">
                            {order.items && order.items.length > 0 ? (
                                <table className="w-full text-left text-xs">
                                    <thead className="text-[10px] font-bold uppercase tracking-wider text-outline">
                                        <tr>
                                            <th className="pb-3">Sản phẩm</th>
                                            <th className="pb-3 text-center">Nhanh ID</th>
                                            <th className="pb-3 text-center">SL</th>
                                            <th className="pb-3 text-right">Đơn giá</th>
                                            <th className="pb-3 text-right">Thành tiền</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/20 font-bold">
                                        {order.items.map((item) => (
                                            <tr key={item.id}>
                                                <td className="py-3 text-on-surface">{item.name}</td>
                                                <td className="py-3 text-center text-outline">
                                                    {item.nhanhProductId || 'N/A'}
                                                </td>
                                                <td className="py-3 text-center">{item.quantity}</td>
                                                <td className="py-3 text-right">
                                                    {formatCurrency(item.price)}
                                                </td>
                                                <td className="py-3 text-right font-black text-primary">
                                                    {formatCurrency(item.price * item.quantity)}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            ) : (
                                <p className="py-6 text-center text-xs font-bold text-outline">
                                    API chưa trả chi tiết sản phẩm.
                                </p>
                            )}

                            <div className="ml-auto mt-6 w-full space-y-2 border-t border-outline-variant/20 pt-4 text-xs font-bold sm:w-1/2">
                                <div className="flex justify-between text-outline">
                                    <span>Tổng đơn:</span>
                                    <span className="text-on-surface">{formatCurrency(totalAmount)}</span>
                                </div>
                                <div className="flex justify-between text-outline">
                                    <span>Phí vận chuyển:</span>
                                    <span className="text-on-surface">{formatCurrency(shippingFee)}</span>
                                </div>
                                <div className="flex justify-between text-outline">
                                    <span>Đã thanh toán:</span>
                                    <span className="text-on-surface">{formatCurrency(paidAmount)}</span>
                                </div>
                                <div className="flex justify-between border-t border-outline-variant/20 pt-2 text-base font-black text-primary">
                                    <span>Còn lại:</span>
                                    <span>{formatCurrency(remainingAmount)}</span>
                                </div>
                            </div>
                        </div>
                    </section>

                    <section className="space-y-4 rounded-2xl border border-outline-variant/30 bg-white p-6 shadow-sm">
                        <div className="flex items-center justify-between gap-4 border-b border-surface-container pb-3">
                            <div className="flex items-center gap-2">
                                <CreditCard className="h-5 w-5 text-primary" />
                                <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                    Lịch sử thanh toán
                                </h2>
                            </div>
                            <span className="text-[9px] font-black uppercase text-outline">
                                {adminPayments.length} giao dịch trong phiên
                            </span>
                        </div>

                        {order.type === 'PREORDER' && order.status === 'DEPOSIT_PAID' && (
                            <button
                                type="button"
                                onClick={handleCreateFinalPayment}
                                disabled={isCreatingFinalPayment || Boolean(confirmingPaymentCode)}
                                className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-[10px] font-black uppercase tracking-wider text-white disabled:opacity-50"
                            >
                                {isCreatingFinalPayment
                                    ? <Loader2 className="h-4 w-4 animate-spin" />
                                    : <CreditCard className="h-4 w-4" />}
                                {isCreatingFinalPayment
                                    ? 'Đang tạo thanh toán cuối...'
                                    : 'Tạo thanh toán đợt cuối'}
                            </button>
                        )}

                        <div className="rounded-xl bg-surface-container/50 p-4">
                            <label className="text-[10px] font-black uppercase text-outline">
                                Xác nhận thanh toán thủ công
                            </label>
                            <div className="mt-2 flex flex-col gap-2 sm:flex-row">
                                <input
                                    type="text"
                                    value={manualPaymentCode}
                                    onChange={(event) => setManualPaymentCode(event.target.value)}
                                    disabled={Boolean(confirmingPaymentCode)}
                                    placeholder="Nhập paymentCode"
                                    className="min-w-0 flex-1 rounded-xl border border-outline-variant/20 bg-white px-3 py-2.5 text-xs font-bold text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                />
                                <button
                                    type="button"
                                    onClick={() => handleConfirmPayment(manualPaymentCode)}
                                    disabled={!manualPaymentCode.trim() || Boolean(confirmingPaymentCode)}
                                    className="flex items-center justify-center gap-2 rounded-xl bg-on-surface px-4 py-2.5 text-[10px] font-black uppercase text-white disabled:opacity-50"
                                >
                                    {confirmingPaymentCode
                                        ? <Loader2 className="h-4 w-4 animate-spin" />
                                        : <Banknote className="h-4 w-4" />}
                                    Xác nhận đã nhận tiền
                                </button>
                            </div>
                        </div>

                        {adminPayments.length > 0 ? (
                            <div className="space-y-3">
                                {adminPayments.map(payment => (
                                    <div
                                        key={payment.id}
                                        className="flex flex-col justify-between gap-3 rounded-xl border border-outline-variant/20 p-4 text-xs sm:flex-row sm:items-center"
                                    >
                                        <div>
                                            <div className="flex flex-wrap items-center gap-2">
                                                <span className="font-black text-on-surface">
                                                    {payment.type} · {formatCurrency(payment.amount)}
                                                </span>
                                                <span className={`rounded-full px-2 py-1 text-[9px] font-black uppercase ${
                                                    payment.status === 'PAID'
                                                        ? 'bg-green-100 text-green-700'
                                                        : payment.status === 'PENDING'
                                                            ? 'bg-amber-100 text-amber-700'
                                                            : 'bg-red-100 text-red-700'
                                                }`}>
                                                    {payment.status}
                                                </span>
                                            </div>
                                            <p className="mt-1 font-medium text-outline">
                                                {payment.paymentCode} · {payment.paymentMethod}
                                            </p>
                                        </div>
                                        {payment.status === 'PENDING' && (
                                            <button
                                                type="button"
                                                onClick={() => handleConfirmPayment(payment.paymentCode)}
                                                disabled={Boolean(confirmingPaymentCode)}
                                                className="flex items-center justify-center gap-2 rounded-lg border border-primary/20 px-3 py-2 text-[9px] font-black uppercase text-primary disabled:opacity-50"
                                            >
                                                {confirmingPaymentCode === payment.paymentCode
                                                    ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                                    : <Banknote className="h-3.5 w-3.5" />}
                                                Xác nhận
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="rounded-xl border border-dashed border-outline-variant/30 p-4 text-xs font-medium leading-relaxed text-outline">
                                Backend hiện chưa có API Admin để tải lịch sử thanh toán cũ.
                                Các giao dịch vừa tạo hoặc xác nhận trong màn hình này sẽ xuất hiện tại đây.
                            </p>
                        )}
                    </section>

                    <section className="space-y-4 rounded-2xl border border-outline-variant/30 bg-white p-6 shadow-sm">
                        <div className="flex items-center justify-between gap-4 border-b border-surface-container pb-3">
                            <div className="flex items-center gap-2">
                                <RefreshCw className="h-5 w-5 text-primary" />
                                <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                    Đồng bộ Nhanh.vn
                                </h2>
                            </div>
                            <span className={`rounded-full border px-3 py-1 text-[9px] font-black uppercase tracking-wider ${getSyncStatusColor(order.syncStatus)}`}>
                                {getSyncStatusText(order.syncStatus)}
                            </span>
                        </div>

                        <div className="space-y-3 rounded-xl border border-outline-variant/10 bg-surface-container/30 p-4 text-xs font-bold">
                            <div className="flex justify-between gap-4">
                                <span className="text-outline">Milestone:</span>
                                <span className="text-right text-on-surface">
                                    {order.nhanhSyncStage || 'NONE'}
                                </span>
                            </div>
                            <div className="flex justify-between gap-4">
                                <span className="text-outline">Nhanh ID:</span>
                                <span className="select-all text-on-surface">
                                    {order.nhanhOrderId || 'Chưa kết nối'}
                                </span>
                            </div>
                            <div className="flex justify-between gap-4">
                                <span className="text-outline">Nhanh code:</span>
                                <span className="select-all text-on-surface">
                                    {order.nhanhOrderCode || 'Chưa kết nối'}
                                </span>
                            </div>
                            {order.lastSyncAt && (
                                <div className="flex justify-between gap-4">
                                    <span className="text-outline">Lần sync cuối:</span>
                                    <span className="text-on-surface">
                                        {new Date(order.lastSyncAt).toLocaleString('vi-VN')}
                                    </span>
                                </div>
                            )}
                        </div>

                        {order.syncStatus === 'FAILED' && (
                            <button
                                type="button"
                                onClick={handleRetrySync}
                                disabled={isRetryingOrderSync}
                                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-br from-primary to-primary-container py-3 text-xs font-black uppercase tracking-widest text-white shadow-md disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {isRetryingOrderSync
                                    ? <Loader2 className="h-4 w-4 animate-spin" />
                                    : <RefreshCw className="h-4 w-4" />}
                                {isRetryingOrderSync ? 'Đang đồng bộ lại...' : 'Retry đồng bộ'}
                            </button>
                        )}
                    </section>
                </div>

                <aside className="space-y-6">
                    <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                        <header className="flex items-center gap-2 border-b border-outline-variant/20 bg-surface-container-lowest p-4">
                            <User className="h-5 w-5 text-primary" />
                            <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                Khách hàng
                            </h2>
                        </header>
                        <div className="space-y-4 p-6 text-xs font-bold">
                            <div>
                                <p className="mb-1 text-[10px] uppercase text-outline">Họ tên</p>
                                <p className="text-sm font-black text-on-surface">
                                    {order.customerName || 'N/A'}
                                </p>
                            </div>
                            <div>
                                <p className="mb-1 text-[10px] uppercase text-outline">Điện thoại</p>
                                <p className="font-black text-on-surface">
                                    {order.customerMobile || 'N/A'}
                                </p>
                            </div>
                            {order.customerEmail && (
                                <div>
                                    <p className="mb-1 text-[10px] uppercase text-outline">Email</p>
                                    <p className="break-all text-on-surface">{order.customerEmail}</p>
                                </div>
                            )}
                            {order.requestCode && (
                                <div>
                                    <p className="mb-1 text-[10px] uppercase text-outline">Yêu cầu liên quan</p>
                                    <Link to="/admin/requests" className="font-black text-primary hover:underline">
                                        #{order.requestCode}
                                    </Link>
                                </div>
                            )}
                        </div>
                    </section>

                    <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                        <header className="flex items-center gap-2 border-b border-outline-variant/20 bg-surface-container-lowest p-4">
                            <MapPin className="h-5 w-5 text-primary" />
                            <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                Giao hàng
                            </h2>
                        </header>
                        <div className="space-y-4 p-6 text-xs font-bold">
                            <div>
                                <p className="mb-1 text-[10px] uppercase text-outline">Địa chỉ</p>
                                <p className="font-semibold leading-relaxed text-on-surface">
                                    {[
                                        order.customerAddress,
                                        order.customerWardName,
                                        order.customerDistrictName,
                                        order.customerCityName
                                    ].filter(Boolean).join(', ') || 'Chưa cập nhật'}
                                </p>
                            </div>
                            <div>
                                <p className="mb-1 text-[10px] uppercase text-outline">Thời gian đặt</p>
                                <p className="font-semibold text-on-surface">
                                    {order.createdAt
                                        ? new Date(order.createdAt).toLocaleString('vi-VN')
                                        : 'N/A'}
                                </p>
                            </div>
                            {order.description && (
                                <div>
                                    <p className="mb-1 text-[10px] uppercase text-outline">Ghi chú</p>
                                    <p className="font-semibold leading-relaxed text-on-surface">
                                        {order.description}
                                    </p>
                                </div>
                            )}
                        </div>
                    </section>
                </aside>
            </div>
        </div>
    );
}
