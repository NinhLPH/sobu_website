import { describe, expect, it } from '@jest/globals';
import { mapDetailToProductModel, mapListItemToProductModel } from './product.model';

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
});
