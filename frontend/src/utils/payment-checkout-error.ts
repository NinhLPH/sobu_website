const normalizeMessage = (message: string) => message
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/đ/g, 'd');

export const getPaymentCheckoutErrorMessage = (message: unknown, fallback: string) => {
    const rawMessage = typeof message === 'string' ? message.trim() : '';
    if (!rawMessage) {
        return fallback;
    }

    const normalized = normalizeMessage(rawMessage);
    const isDuplicatePayOSOrder = normalized.includes('don thanh toan')
        && normalized.includes('ton tai');
    const isPayOSCheckoutFailure = normalized.includes('payos')
        && (normalized.includes('checkout') || normalized.includes('order code'));

    if (isDuplicatePayOSOrder || isPayOSCheckoutFailure) {
        return 'Không thể tạo phiên thanh toán PayOS. Đơn hàng vẫn được giữ nguyên; vui lòng thử lại tại trang theo dõi đơn.';
    }

    return rawMessage;
};
