import { create } from 'zustand';
import {
    CreateOrderPaymentDto,
    OrderPaymentResponseDto
} from '../interface/order.model';
import { CustomerService } from '../service/custom.service';
import { createIdempotencyKey } from '../utils/idempotency';
import { getPaymentCheckoutErrorMessage } from '../utils/payment-checkout-error';

let latestPaymentsRequestId = 0;

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

const upsertPayment = (
    payments: OrderPaymentResponseDto[],
    payment: OrderPaymentResponseDto
) => {
    const existingIndex = payments.findIndex(item => item.id === payment.id);
    if (existingIndex < 0) {
        return [...payments, payment];
    }
    return payments.map(item => item.id === payment.id ? payment : item);
};

interface PaymentState {
    payments: OrderPaymentResponseDto[];
    activeOrderId: string | null;
    isLoadingPayments: boolean;
    isCreatingPayment: boolean;
    paymentError: string | null;
    pendingPaymentKey: string | null;
    pendingPaymentFingerprint: string | null;
    fetchPayments: (orderId: string | number) => Promise<OrderPaymentResponseDto[]>;
    createPayment: (
        orderId: string | number,
        data: CreateOrderPaymentDto
    ) => Promise<OrderPaymentResponseDto>;
    clearPaymentError: () => void;
    clearPayments: () => void;
}

export const usePaymentStore = create<PaymentState>((set, get) => ({
    payments: [],
    activeOrderId: null,
    isLoadingPayments: false,
    isCreatingPayment: false,
    paymentError: null,
    pendingPaymentKey: null,
    pendingPaymentFingerprint: null,

    fetchPayments: async (orderId) => {
        const normalizedOrderId = String(orderId);
        const requestId = ++latestPaymentsRequestId;
        set((state) => ({
            payments: state.activeOrderId === normalizedOrderId ? state.payments : [],
            activeOrderId: normalizedOrderId,
            isLoadingPayments: true,
            paymentError: null
        }));
        try {
            const response = await CustomerService.getOrderPayments(orderId);
            if (!response.success) {
                throw new Error(response.message || 'Không thể tải lịch sử thanh toán.');
            }
            if (
                requestId === latestPaymentsRequestId &&
                get().activeOrderId === normalizedOrderId
            ) {
                set({
                    payments: response.data ?? [],
                    isLoadingPayments: false
                });
            }
            return response.data ?? [];
        } catch (error) {
            if (
                requestId === latestPaymentsRequestId &&
                get().activeOrderId === normalizedOrderId
            ) {
                set({
                    isLoadingPayments: false,
                    paymentError: getErrorMessage(error, 'Không thể tải lịch sử thanh toán.')
                });
            }
            throw error;
        }
    },

    createPayment: async (orderId, data) => {
        const fingerprint = JSON.stringify({
            orderId: String(orderId),
            ...data
        });
        const state = get();
        const idempotencyKey = state.pendingPaymentFingerprint === fingerprint
            && state.pendingPaymentKey
            ? state.pendingPaymentKey
            : createIdempotencyKey();

        set((current) => ({
            payments: current.activeOrderId === String(orderId) ? current.payments : [],
            activeOrderId: String(orderId),
            isCreatingPayment: true,
            paymentError: null,
            pendingPaymentKey: idempotencyKey,
            pendingPaymentFingerprint: fingerprint
        }));

        try {
            const response = await CustomerService.createOrderPayment(
                orderId,
                data,
                idempotencyKey
            );
            if (!response.success) {
                throw new Error(response.message || 'Không thể tạo phiên thanh toán.');
            }

            const payment = response.data;
            set(current => ({
                payments: upsertPayment(current.payments, payment),
                isCreatingPayment: false,
                pendingPaymentKey: null,
                pendingPaymentFingerprint: null
            }));

            if (data.paymentMethod === 'ONLINE' && !payment.checkoutUrl) {
                throw new Error(
                    payment.failureReason ||
                    'Cổng thanh toán chưa trả về đường dẫn checkout.'
                );
            }

            return payment;
        } catch (error) {
            set({
                isCreatingPayment: false,
                paymentError: getPaymentCheckoutErrorMessage(
                    getErrorMessage(error, 'Không thể tạo phiên thanh toán.'),
                    'Không thể tạo phiên thanh toán.'
                )
            });
            throw error;
        }
    },

    clearPaymentError: () => set({ paymentError: null }),
    clearPayments: () => {
        latestPaymentsRequestId += 1;
        set({
            payments: [],
            activeOrderId: null,
            isLoadingPayments: false,
            isCreatingPayment: false,
            paymentError: null
        });
    }
}));
