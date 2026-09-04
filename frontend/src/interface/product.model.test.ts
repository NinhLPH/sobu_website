import { describe, expect, it } from '@jest/globals';
import { getDiscountPercent, isSaleProduct, mapDetailToProductModel, mapListItemToProductModel } from './product.model';

describe('product model mappers', () => {
    it('defaults missing review summary to zero', () => {
        const product = mapListItemToProductModel({
            id: 1001,
            name: 'SOBU model',
            code: 'SOBU-001',
            price: 100000
        });

        expect(product.rating).toBe(0);
        expect(product.reviewsCount).toBe(0);
    });

    it('maps backend review summary values from product detail', () => {
        const product = mapDetailToProductModel({
            id: 1001,
            name: 'SOBU model',
            code: 'SOBU-001',
            description: 'Test product',
            content: '',
            price: 100000,
            oldPrice: 120000,
            avatarImage: '/model.jpg',
            brandName: 'SOBU',
            categoryName: 'Model',
            stockAvailable: 5,
            stockRemain: 5,
            units: [],
            attributes: [],
            images: [],
            averageRating: 4.5,
            reviewsCount: 12,
            updatedAt: '2026-07-04T00:00:00'
        });

        expect(product.rating).toBe(4.5);
        expect(product.reviewsCount).toBe(12);
    });

    it('preserves a zero available stock instead of falling back to remaining stock', () => {
        const product = mapDetailToProductModel({
            id: 1004, name: 'Reserved model', code: 'RES-001', description: '', content: '', price: 100000,
            avatarImage: '', brandName: '', categoryName: '', stockAvailable: 0, stockRemain: 8,
            units: [], attributes: [], images: [], updatedAt: '2026-07-04T00:00:00'
        });

        expect(product.stock).toBe(0);
    });

    it('maps one manual tag and derives sale only from a valid price pair', () => {
        const product = mapListItemToProductModel({
            id: 1002,
            name: 'Sale model',
            price: 80000,
            oldPrice: 100000,
            badgeName: 'HOT',
            badgeColor: '#dc2626',
            badgeTextColor: '#ffffff'
        });

        expect(product.manualTag).toEqual({
            label: 'HOT',
            backgroundColor: '#dc2626',
            textColor: '#ffffff'
        });
        expect(isSaleProduct(product)).toBe(true);
        expect(getDiscountPercent(product)).toBe(20);
    });

    it('does not expose SALE as a manual tag or accept an invalid discount', () => {
        const product = mapListItemToProductModel({
            id: 1003,
            name: 'Regular model',
            price: 100000,
            oldPrice: 90000,
            badgeName: 'SALE'
        });

        expect(product.manualTag).toBeUndefined();
        expect(product.originalPrice).toBeUndefined();
        expect(isSaleProduct(product)).toBe(false);
    });
});
