import { describe, expect, it } from '@jest/globals';
import { OrderResponseDto } from '../interface/order.model';
import { ProductModel } from '../interface/product.model';
import { canReviewProductFromOrder } from './review-eligibility';

const product: ProductModel = {
    id: '1001',
    externalId: '9001001',
    nhanhProductId: '9001001',
    name: 'SOBU model',
    price: 100000,
    brand: 'SOBU',
    imageUrl: '/model.jpg',
    description: 'Test product',
    stock: 5
};

const deliveredOrder: OrderResponseDto = {
    id: 12,
    status: 'DELIVERED',
    items: []
};

describe('canReviewProductFromOrder', () => {
    it('rejects non-delivered orders', () => {
        const result = canReviewProductFromOrder(
            { ...deliveredOrder, status: 'SHIPPED' },
            product
        );

        expect(result.canReview).toBe(false);
        expect(result.reason).toMatch(/đã giao/i);
    });

    it('accepts a delivered order with a matching productId', () => {
        const result = canReviewProductFromOrder(
            {
                ...deliveredOrder,
                items: [{ id: 1, name: 'SOBU model', productId: 1001, price: 100000, quantity: 1 }]
            },
            product
        );

        expect(result.canReview).toBe(true);
    });

    it('accepts a delivered order with a matching nhanhProductId fallback', () => {
        const result = canReviewProductFromOrder(
            {
                ...deliveredOrder,
                items: [{ id: 1, name: 'SOBU model', nhanhProductId: '9001001', price: 100000, quantity: 1 }]
            },
            product
        );

        expect(result.canReview).toBe(true);
    });

    it('rejects delivered orders without the current product', () => {
        const result = canReviewProductFromOrder(
            {
                ...deliveredOrder,
                items: [{ id: 1, name: 'Other product', productId: 2002, nhanhProductId: '9002002', price: 100000, quantity: 1 }]
            },
            product
        );

        expect(result.canReview).toBe(false);
        expect(result.reason).toMatch(/không chứa sản phẩm/i);
    });
});
