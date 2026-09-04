import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { cartFallback } from './cart-fallback';
import { CartItem } from '../interface/product.model';

const key = 'sobu.cartFallback.v1:990';
const items: CartItem[] = [{ product: { id: '10', name: 'Product', price: 100,
    stock: 5, brand: '', imageUrl: '', description: '' }, quantity: 2 }];

describe('cart fallback provenance', () => {
    beforeEach(() => {
        window.sessionStorage.clear();
        window.localStorage.clear();
        window.sessionStorage.setItem('user', JSON.stringify({ id: 990 }));
    });

    it('writes local-edit by default, including intentional emptiness', () => {
        cartFallback.save(items);
        expect(cartFallback.getSnapshot()).toEqual({ items, source: 'local-edit' });
        cartFallback.save([]);
        expect(cartFallback.getSnapshot()).toEqual({ items: [], source: 'local-edit' });
    });

    it('distinguishes legacy nonempty, legacy empty and read-cache data', () => {
        window.sessionStorage.setItem(key, JSON.stringify({ items }));
        expect(cartFallback.getSnapshot()?.source).toBe('local-edit');
        window.sessionStorage.setItem(key, JSON.stringify({ items: [] }));
        expect(cartFallback.getSnapshot()?.source).toBe('legacy-empty');
        cartFallback.save([], 'read-cache');
        expect(cartFallback.getSnapshot()?.source).toBe('read-cache');
    });

    it('retains the array getter contract and returns independent item copies', () => {
        cartFallback.save(items, 'read-cache');
        const result = cartFallback.get()!;
        result[0].quantity = 9;
        expect(cartFallback.get()).toEqual(items);
    });

    it('does not expose a different account snapshot', () => {
        cartFallback.save(items);
        window.sessionStorage.setItem('user', JSON.stringify({ id: 991 }));
        expect(cartFallback.get()).toBeNull();
    });

    it('tolerates unavailable browser storage', () => {
        const read = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Denied'); });
        try {
            expect(cartFallback.get()).toBeNull();
            expect(cartFallback.save(items)).toBe(false);
            expect(() => cartFallback.clear()).not.toThrow();
        } finally { read.mockRestore(); }
    });
});
