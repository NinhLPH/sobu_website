import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
    AlertCircle,
    CheckCircle2,
    Clock3,
    Loader2,
    ReceiptText
} from 'lucide-react';
import { PaymentStatus } from '../enum/union-types';
import { usePaymentStore } from '../store/usePaymentStore';
import { paymentSession } from '../utils/payment-session';

type ResultStatus = PaymentStatus | 'SUCCESS' | 'UNKNOWN';

const normalizeQueryStatus = (
    status: string | null,
    code: string | null,
    cancelled: string | null
): ResultStatus => {
    const normalizedStatus = status?.toUpperCase();
    if (normalizedStatus === 'SUCCESS') {
        return 'SUCCESS';
    }
    if (
        normalizedStatus === 'PAID' ||
        normalizedStatus === 'FAILED' ||
        normalizedStatus === 'CANCELLED' ||
        normalizedStatus === 'EXPIRED' ||
        normalizedStatus === 'REFUNDED' ||
        normalizedStatus === 'PENDING'
    ) {
        return normalizedStatus;
    }
    if (cancelled === 'true') {
        return 'FAILED';
    }
    if (code === '00') {
        return 'SUCCESS';
    }
    return 'UNKNOWN';
};

export default function PaymentResult() {
    const [searchParams] = useSearchParams();
    const storedContext = useMemo(() => paymentSession.get(), []);
    const orderId = searchParams.get('orderId') || storedContext?.orderId || '';
    const paymentCode = searchParams.get('paymentCode') || storedContext?.paymentCode || '';
    const [resultStatus, setResultStatus] = useState<ResultStatus>(() =>
        normalizeQueryStatus(
            searchParams.get('status'),
            searchParams.get('code'),
            searchParams.get('cancel')
        )
    );
    const [isChecking, setIsChecking] = useState(Boolean(orderId));
    const {
        fetchPayments,
        paymentError,
        clearPaymentError
    } = usePaymentStore();

    useEffect(() => {
        if (!orderId) {
            setIsChecking(false);
            return;
        }

        let disposed = false;
        let timer: ReturnType<typeof setTimeout> | undefined;
        let attempt = 0;

        const refreshStatus = async () => {
            attempt += 1;
            try {
                const payments = await fetchPayments(orderId);
                const payment = payments.find(item => item.paymentCode === paymentCode)
                    || payments[payments.length - 1];
                if (disposed) {
                    return;
                }
                if (payment && payment.status !== 'PENDING') {
                    setResultStatus(payment.status);
                    setIsChecking(false);
                    return;
                }
                if (attempt < 4) {
                    timer = setTimeout(refreshStatus, 2000);
                    return;
                }
            } catch {
                // Keep the redirect result visible when status refresh is unavailable.
            }
            if (!disposed) {
                setIsChecking(false);
            }
        };

        clearPaymentError();
        refreshStatus();

        return () => {
            disposed = true;
            if (timer) {
                clearTimeout(timer);
            }
        };
    }, [clearPaymentError, fetchPayments, orderId, paymentCode]);

    const isSuccess = resultStatus === 'SUCCESS' || resultStatus === 'PAID';
    const isFailure = ['FAILED', 'CANCELLED', 'EXPIRED'].includes(resultStatus);

    return (
        <main className="mx-auto flex min-h-[70vh] max-w-3xl items-center justify-center bg-surface px-6 py-28">
            <section className="w-full rounded-[2rem] border border-surface-container bg-white p-8 text-center shadow-lg sm:p-12">
                <div className={`mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full ${
                    isSuccess
                        ? 'bg-green-100 text-green-600'
                        : isFailure
                            ? 'bg-red-100 text-red-600'
                            : 'bg-amber-100 text-amber-600'
                }`}>
                    {isSuccess
                        ? <CheckCircle2 className="h-11 w-11" />
                        : isFailure
                            ? <AlertCircle className="h-11 w-11" />
                            : <Clock3 className="h-11 w-11" />}
                </div>

                <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface sm:text-3xl">
                    {isSuccess
                        ? 'Thanh toán thành công'
                        : isFailure
                            ? 'Thanh toán chưa thành công'
                            : 'Đang xác nhận giao dịch'}
                </h1>
                <p className="mx-auto mt-3 max-w-lg text-sm font-medium leading-relaxed text-outline">
                    {isSuccess
                        ? 'Giao dịch đã được ghi nhận. Trạng thái đơn hàng sẽ được cập nhật theo webhook PayOS.'
                        : isFailure
                            ? 'Giao dịch đã thất bại hoặc bị hủy. Bạn có thể quay lại đơn hàng để tạo phiên thanh toán mới.'
                            : 'PayOS đã chuyển bạn về cửa hàng. Hệ thống đang chờ backend đối soát trạng thái cuối cùng.'}
                </p>

                {(paymentCode || orderId) && (
                    <div className="mx-auto mt-6 max-w-md rounded-2xl bg-surface-container p-4 text-left text-xs font-bold">
                        {paymentCode && (
                            <p className="flex justify-between gap-4">
                                <span className="text-outline">Mã thanh toán</span>
                                <span className="break-all text-right text-on-surface">{paymentCode}</span>
                            </p>
                        )}
                        {orderId && (
                            <p className="mt-2 flex justify-between gap-4">
                                <span className="text-outline">ID đơn hàng</span>
                                <span className="text-on-surface">{orderId}</span>
                            </p>
                        )}
                    </div>
                )}

                {isChecking && (
                    <p className="mt-4 flex items-center justify-center gap-2 text-xs font-bold text-primary">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Đang làm mới lịch sử thanh toán...
                    </p>
                )}
                {paymentError && !isChecking && (
                    <p className="mt-4 text-xs font-bold text-error">
                        Chưa thể tải trạng thái mới nhất. Vui lòng kiểm tra lại trong chi tiết đơn hàng.
                    </p>
                )}

                <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
                    <Link
                        to={orderId
                            ? `/tracking?orderId=${encodeURIComponent(orderId)}`
                            : '/tracking'}
                        className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3 text-xs font-black uppercase tracking-wider text-white"
                    >
                        <ReceiptText className="h-4 w-4" />
                        Xem chi tiết đơn hàng
                    </Link>
                    <Link
                        to="/"
                        className="inline-flex items-center justify-center rounded-xl border border-primary/20 px-6 py-3 text-xs font-black uppercase tracking-wider text-primary"
                    >
                        Về trang chủ
                    </Link>
                </div>
            </section>
        </main>
    );
}
