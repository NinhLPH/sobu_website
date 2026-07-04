import { OrderResponseDto } from '../interface/order.model';
import { ProductModel } from '../interface/product.model';

export interface ReviewEligibilityResult {
    canReview: boolean;
    reason: string;
}

const normalizeId = (value: unknown) =>
    value === undefined || value === null ? '' : String(value).trim();

export const canReviewProductFromOrder = (
    order: OrderResponseDto | null | undefined,
    product: ProductModel | null | undefined
): ReviewEligibilityResult => {
    if (!order) {
        return {
            canReview: false,
            reason: 'Không tìm thấy đơn hàng để xác minh.'
        };
    }

    if (order.status !== 'DELIVERED') {
        return {
            canReview: false,
            reason: 'Chỉ đơn hàng đã giao thành công mới có thể đánh giá sản phẩm.'
        };
    }

    if (!product) {
        return {
            canReview: false,
            reason: 'Không tìm thấy thông tin sản phẩm để xác minh.'
        };
    }

    const productId = normalizeId(product.id);
    const productExternalIds = new Set(
        [product.externalId, product.nhanhProductId]
            .map(normalizeId)
            .filter(Boolean)
    );

    const hasMatchingItem = (order.items || []).some((item) => {
        const itemProductId = normalizeId(item.productId);
        const itemNhanhId = normalizeId(item.nhanhProductId);
        return Boolean(
            (itemProductId && itemProductId === productId) ||
            (itemNhanhId && productExternalIds.has(itemNhanhId))
        );
    });

    if (!hasMatchingItem) {
        return {
            canReview: false,
            reason: 'Đơn hàng này không chứa sản phẩm đang xem.'
        };
    }

    return {
        canReview: true,
        reason: 'Đơn hàng hợp lệ. Bạn có thể gửi đánh giá cho sản phẩm này.'
    };
};
