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
    Truck,
    User,
    XCircle
} from 'lucide-react';
import { useAdminStore } from '../../store/useAdminStore';
import { useIntegrationStore } from '../../store/useIntegrationStore';
import { OrderStatus } from '../../enum/union-types';
import {hasNhanhHistory} from '../../utils/order-sync';
import { formatCurrency } from '../../utils/format';
import {useConfirmDialog} from '../../components/common/ConfirmDialog';

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
        case 'FAILED':
            return 'border-red-200 bg-red-100 text-red-800';
        case 'RETURNED':
            return 'border-orange-200 bg-orange-100 text-orange-800';
        case 'CANCELLED':
            return 'border-red-200 bg-red-100 text-red-800';
        default:
            return 'border-outline-variant/40 bg-surface-container text-on-surface-variant';
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
        case 'FAILED':
            return 'Giao thất bại';
        case 'RETURNED':
            return 'Chuyển hoàn';
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
        case 'NEED_RECONCILE':
            return 'border-orange-200 bg-orange-50 text-orange-700';
        case 'DEAD':
            return 'border-outline-variant/40 bg-surface-container text-on-surface-variant';
        case 'PENDING':
            return 'border-amber-200 bg-amber-50 text-amber-700';
        default:
            return 'border-outline-variant/40 bg-surface-container-low text-on-surface-variant';
    }
};

const getSyncStatusText = (status?: string) => {
    switch (status) {
        case 'SYNCED':
            return 'Đã đồng bộ';
        case 'FAILED':
            return 'Đồng bộ thất bại';
        case 'NEED_RECONCILE':
            return 'Cần đối soát';
        case 'DEAD':
            return 'Đã dừng retry';
        case 'PENDING':
            return 'Chờ đồng bộ';
        default:
            return status || 'Chưa cập nhật';
    }
};

const canRetrySync = (status?: string) =>
    status === 'FAILED' || status === 'NEED_RECONCILE' || status === 'DEAD';

export default function AdminOrderDetail() {
    const confirm = useConfirmDialog();
    const { id } = useParams();
    const [manualPaymentCode, setManualPaymentCode] = useState('');
    const {
        currentOrderDetail: order,
        adminPayments,
        fetchOrderDetail,
        fetchOrderPayments,
        retryOrderSync,
        createPreorderFinalPayment,
        confirmMockPayment,
        clearCurrentOrder,
        clearOrdersError,
        clearOrderActionMessage,
        updateOrderStatus,
        isUpdatingOrderStatus,
        isOrderDetailLoading,
        isAdminPaymentsLoading,
        adminPaymentsError,
        retryingOrderIds,
        isCreatingFinalPayment,
        confirmingPaymentCode,
        ordersError,
        orderActionMessage
    } = useAdminStore();
    const isRetryingOrderSync = id ? retryingOrderIds.includes(Number(id)) : false;
    const nhanhEnabled = useIntegrationStore((state) => state.nhanhEnabled);
    const integrationLoaded = useIntegrationStore((state) => state.loaded);
    const ensureIntegrationLoaded = useIntegrationStore((state) => state.ensureLoaded);
    const canUseNhanh = integrationLoaded && nhanhEnabled;
    const showNhanhHistory = hasNhanhHistory(order);
    const showNhanhProductIds = canUseNhanh || Boolean(
        showNhanhHistory && order?.items?.some(item => item.nhanhProductId)
    );

    const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
    const [selectedTargetStatus, setSelectedTargetStatus] = useState<OrderStatus | ''>('');
    const [statusReason, setStatusReason] = useState('');
    const [statusTrackingCode, setStatusTrackingCode] = useState('');

    const openStatusModal = (target: OrderStatus) => {
        setSelectedTargetStatus(target);
        setStatusReason('');
        setStatusTrackingCode(order?.trackingCode || '');
        setIsStatusModalOpen(true);
    };

    const handleConfirmStatusUpdate = async () => {
        if (!order || !selectedTargetStatus) return;
        try {
            await updateOrderStatus(order.id, {
                status: selectedTargetStatus,
                reason: statusReason.trim() || undefined,
                trackingCode: statusTrackingCode.trim() || undefined
            });
            setIsStatusModalOpen(false);
        } catch {
            // Error captured in useAdminStore ordersError
        }
    };

    useEffect(() => {
        void ensureIntegrationLoaded();
    }, [ensureIntegrationLoaded]);

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

    const handleRetryPayments = async () => {
        if (!id) {
            return;
        }
        try {
            await fetchOrderPayments(id);
        } catch {
            // The admin store exposes the payment-specific error below.
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
        const confirmed = await confirm({
            title: 'Xác nhận thanh toán giả lập?',
            message: `Giao dịch ${normalizedCode} sẽ được đánh dấu đã thanh toán. Chỉ thực hiện với môi trường giả lập.`,
            confirmLabel: 'Xác nhận thanh toán',
            tone: 'warning'
        });
        if (!confirmed) {
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
    const discountAmount = order.discountAmount ?? 0;
    const shippingDiscountAmount = order.shippingDiscountAmount ?? 0;
    const originalSubtotal = Math.max(0, totalAmount + discountAmount + shippingDiscountAmount - shippingFee);
    const paidAmount = order.paidAmount ?? order.depositAmount ?? 0;
    const remainingAmount = order.remainingAmount ?? Math.max(0, totalAmount - paidAmount);

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-4">
                <Link
                    to="/admin/orders"
                    className="rounded-full border border-outline-variant/30 bg-surface p-2 transition-colors hover:bg-surface-variant"
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

            {showNhanhHistory && (order.lastSyncMessage || order.syncError) && (
                <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-xs font-medium leading-relaxed text-red-800">
                    <span className="mb-1 block font-black uppercase tracking-wide text-red-600">
                        Chi tiết đồng bộ{canUseNhanh ? '' : ' đã lưu'}
                    </span>
                    {order.lastSyncMessage || order.syncError}
                </div>
            )}

            {/* Status Management Bar */}
            <div className="rounded-2xl border border-outline-variant/30 bg-surface p-5 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-4">
                    <div className="space-y-1">
                        <p className="text-[10px] font-black uppercase tracking-wider text-outline">
                            Thao tác trạng thái đơn hàng
                        </p>
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-bold text-on-surface">Trạng thái hiện tại:</span>
                            <span className={`rounded-full border px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider ${getStatusColor(order.status)}`}>
                                {getStatusText(order.status)}
                            </span>
                            {order.trackingCode && (
                                <span className="rounded-lg bg-surface-container px-2 py-0.5 text-xs font-mono font-bold text-on-surface">
                                    Vận đơn: {order.trackingCode}
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                        {/* Quick action buttons for PROCESSING */}
                        {order.status === 'PROCESSING' && (
                            <>
                                <button
                                    type="button"
                                    onClick={() => openStatusModal('SHIPPED')}
                                    className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-3.5 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-primary/90"
                                >
                                    <Truck className="h-4 w-4" />
                                    Xuất giao hàng (SHIPPED)
                                </button>
                                <button
                                    type="button"
                                    onClick={() => openStatusModal('CANCELLED')}
                                    className="inline-flex items-center gap-1.5 rounded-xl border border-red-300 bg-red-50 px-3.5 py-2 text-xs font-bold text-red-700 transition hover:bg-red-100"
                                >
                                    <XCircle className="h-4 w-4" />
                                    Hủy đơn (CANCELLED)
                                </button>
                            </>
                        )}

                        {/* Allowed Next Transitions Selector */}
                        {order.allowedNextStatuses && order.allowedNextStatuses.length > 0 && (
                            <div className="flex items-center gap-2">
                                <span className="text-xs font-bold text-outline">Chuyển sang:</span>
                                <select
                                    value=""
                                    onChange={(e) => {
                                        if (e.target.value) {
                                            openStatusModal(e.target.value as OrderStatus);
                                        }
                                    }}
                                    className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-xs font-bold text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
                                >
                                    <option value="" disabled>-- Chọn trạng thái --</option>
                                    {order.allowedNextStatuses.map((st) => (
                                        <option key={st} value={st}>
                                            {getStatusText(st)} ({st})
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                <div className="space-y-6 lg:col-span-2">
                    <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface shadow-sm">
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
                                            {showNhanhProductIds && (
                                                <th className="pb-3 text-center">Nhanh ID</th>
                                            )}
                                            <th className="pb-3 text-center">SL</th>
                                            <th className="pb-3 text-right">Đơn giá</th>
                                            <th className="pb-3 text-right">Thành tiền</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/20 font-bold">
                                        {order.items.map((item) => (
                                            <tr key={item.id}>
                                                <td className="py-3 text-on-surface">{item.name}</td>
                                                {showNhanhProductIds && (
                                                    <td className="py-3 text-center text-outline">
                                                        {item.nhanhProductId || 'N/A'}
                                                    </td>
                                                )}
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
                                    <span>Tạm tính:</span>
                                    <span className="text-on-surface">{formatCurrency(originalSubtotal)}</span>
                                </div>
                                <div className="flex justify-between text-outline">
                                    <span>Phí vận chuyển gốc:</span>
                                    <span className="text-on-surface">{formatCurrency(shippingFee)}</span>
                                </div>
                                {discountAmount > 0 && <div className="flex justify-between text-emerald-700"><span>Giảm sản phẩm/toàn đơn:</span><span>-{formatCurrency(discountAmount)}</span></div>}
                                {shippingDiscountAmount > 0 && <div className="flex justify-between text-emerald-700"><span>Giảm phí vận chuyển:</span><span>-{formatCurrency(shippingDiscountAmount)}</span></div>}
                                {(order.discountVoucherCode || order.shippingVoucherCode) && <div className="flex flex-wrap gap-2 py-1" aria-label="Voucher đã áp dụng">
                                    {order.discountVoucherCode && <span className="rounded-full border border-primary/20 bg-primary/10 px-2.5 py-1 font-mono text-[10px] font-black text-primary">{order.discountVoucherCode}</span>}
                                    {order.shippingVoucherCode && <span className="rounded-full border border-primary/20 bg-primary/10 px-2.5 py-1 font-mono text-[10px] font-black text-primary">{order.shippingVoucherCode}</span>}
                                </div>}
                                <div className="flex justify-between border-t border-outline-variant/20 pt-2 font-black text-on-surface"><span>Tổng thanh toán:</span><span>{formatCurrency(totalAmount)}</span></div>
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

                    <section className="space-y-4 rounded-2xl border border-outline-variant/30 bg-surface p-6 shadow-sm">
                        <div className="flex items-center justify-between gap-4 border-b border-surface-container pb-3">
                            <div className="flex items-center gap-2">
                                <CreditCard className="h-5 w-5 text-primary" />
                                <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                    Lịch sử thanh toán
                                </h2>
                            </div>
                            <span className="text-[9px] font-black uppercase text-outline">
                                {isAdminPaymentsLoading
                                    ? 'Đang tải giao dịch...'
                                    : adminPaymentsError
                                        ? 'Chưa xác định'
                                        : `${adminPayments.length} giao dịch đã ghi nhận`}
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
                                    className="min-w-0 flex-1 rounded-xl border border-outline-variant/20 bg-surface px-3 py-2.5 text-xs font-bold text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
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

                        {isAdminPaymentsLoading && adminPayments.length === 0 && (
                            <div className="flex items-center justify-center gap-2 rounded-xl border border-outline-variant/20 p-4 text-xs font-bold text-outline">
                                <Loader2 className="h-4 w-4 animate-spin" />
                                Đang tải lịch sử thanh toán...
                            </div>
                        )}

                        {adminPaymentsError && (
                            <div className="flex flex-col gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error sm:flex-row sm:items-center sm:justify-between">
                                <span className="flex items-start gap-2">
                                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                                    {adminPaymentsError}
                                </span>
                                <button
                                    type="button"
                                    onClick={handleRetryPayments}
                                    disabled={isAdminPaymentsLoading}
                                    className="flex items-center justify-center gap-2 rounded-lg border border-error/20 px-3 py-2 text-[9px] font-black uppercase disabled:opacity-50"
                                >
                                    <RefreshCw className={`h-3.5 w-3.5 ${isAdminPaymentsLoading ? 'animate-spin' : ''}`} />
                                    Thử lại
                                </button>
                            </div>
                        )}

                        {adminPayments.length > 0 && (
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
                        )}

                        {!isAdminPaymentsLoading && !adminPaymentsError && adminPayments.length === 0 && (
                            <p className="rounded-xl border border-dashed border-outline-variant/30 p-4 text-xs font-medium leading-relaxed text-outline">
                                Chưa có giao dịch thanh toán nào được ghi nhận cho đơn hàng này.
                            </p>
                        )}
                    </section>

                    {(canUseNhanh || showNhanhHistory) && (
                        <section className="space-y-4 rounded-2xl border border-outline-variant/30 bg-surface p-6 shadow-sm">
                            <div className="flex items-center justify-between gap-4 border-b border-surface-container pb-3">
                                <div className="flex items-center gap-2">
                                    <RefreshCw className="h-5 w-5 text-primary" />
                                    <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                        Đồng bộ Nhanh.vn
                                    </h2>
                                </div>
                                <span className={`rounded-full border px-3 py-1 text-[9px] font-black uppercase tracking-wider ${getSyncStatusColor(order.syncStatus)}`}>
                                    {canUseNhanh ? getSyncStatusText(order.syncStatus) : 'Chỉ đọc'}
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

                            {canUseNhanh && canRetrySync(order.syncStatus) && (
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
                    )}
                </div>

                <aside className="space-y-6">
                    <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface shadow-sm">
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

                    <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface shadow-sm">
                        <header className="flex items-center gap-2 border-b border-outline-variant/20 bg-surface-container-lowest p-4">
                            <MapPin className="h-5 w-5 text-primary" />
                            <h2 className="text-xs font-black uppercase tracking-wider text-on-surface">
                                Giao hàng
                            </h2>
                        </header>
                        <div className="space-y-4 p-6 text-xs font-bold">
                            {order.trackingCode && (
                                <div>
                                    <p className="mb-1 text-[10px] uppercase text-outline">Mã vận đơn</p>
                                    <p className="font-mono font-black text-primary select-all">
                                        {order.trackingCode}
                                    </p>
                                    {order.trackingUrl && (
                                        <a
                                            href={order.trackingUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="mt-1 inline-block text-[11px] font-bold text-primary hover:underline"
                                        >
                                            Tra cứu vận đơn &rarr;
                                        </a>
                                    )}
                                </div>
                            )}
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

            {/* Status Update Confirmation Modal */}
            {isStatusModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="w-full max-w-md rounded-2xl border border-outline-variant/30 bg-surface p-6 shadow-xl">
                        <div className="mb-4 flex items-center justify-between border-b border-outline-variant/20 pb-3">
                            <h3 className="text-base font-black uppercase tracking-tight text-on-surface">
                                Cập nhật trạng thái đơn hàng
                            </h3>
                            <button
                                type="button"
                                onClick={() => setIsStatusModalOpen(false)}
                                className="rounded-full p-1 text-outline hover:bg-surface-variant hover:text-on-surface"
                            >
                                <XCircle className="h-5 w-5" />
                            </button>
                        </div>

                        <div className="space-y-4 text-xs font-bold">
                            <div className="flex items-center justify-between rounded-xl bg-surface-container/50 p-3">
                                <div>
                                    <p className="text-[10px] text-outline uppercase">Hiện tại</p>
                                    <p className="text-on-surface font-black">{getStatusText(order.status)}</p>
                                </div>
                                <span className="text-outline">&rarr;</span>
                                <div className="text-right">
                                    <p className="text-[10px] text-outline uppercase">Mục tiêu</p>
                                    <p className="text-primary font-black">{getStatusText(selectedTargetStatus)}</p>
                                </div>
                            </div>

                            {selectedTargetStatus === 'CANCELLED' && (
                                <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-amber-800">
                                    <p className="font-black">Lưu ý khi hủy đơn:</p>
                                    <p className="mt-0.5 font-medium leading-relaxed">
                                        Hệ thống sẽ tự động hoàn trả tồn kho khả dụng (stockAvailable) cho các sản phẩm trong đơn hàng.
                                    </p>
                                </div>
                            )}

                            {selectedTargetStatus === 'SHIPPED' && (
                                <div>
                                    <label className="mb-1 block text-[10px] font-black uppercase tracking-wider text-outline">
                                        Mã vận đơn (tracking code)
                                    </label>
                                    <input
                                        type="text"
                                        value={statusTrackingCode}
                                        onChange={(e) => setStatusTrackingCode(e.target.value)}
                                        placeholder="VD: VNPOST123456, GHTK897123..."
                                        className="w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-3 py-2.5 text-xs font-mono font-bold text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
                                    />
                                </div>
                            )}

                            <div>
                                <label className="mb-1 block text-[10px] font-black uppercase tracking-wider text-outline">
                                    Lý do / Ghi chú thay đổi (tùy chọn)
                                </label>
                                <textarea
                                    value={statusReason}
                                    onChange={(e) => setStatusReason(e.target.value)}
                                    rows={3}
                                    placeholder="Ghi chú lý do cập nhật trạng thái..."
                                    className="w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-3 py-2 text-xs font-normal text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
                                />
                            </div>

                            <div className="flex items-center justify-end gap-3 pt-2">
                                <button
                                    type="button"
                                    disabled={isUpdatingOrderStatus}
                                    onClick={() => setIsStatusModalOpen(false)}
                                    className="rounded-xl border border-outline-variant/30 px-4 py-2 text-xs font-bold text-outline hover:bg-surface-variant"
                                >
                                    Đóng
                                </button>
                                <button
                                    type="button"
                                    disabled={isUpdatingOrderStatus}
                                    onClick={handleConfirmStatusUpdate}
                                    className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-black uppercase tracking-wider text-white shadow hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                    {isUpdatingOrderStatus ? (
                                        <>
                                            <Loader2 className="h-4 w-4 animate-spin" />
                                            Đang cập nhật...
                                        </>
                                    ) : (
                                        'Xác nhận cập nhật'
                                    )}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
