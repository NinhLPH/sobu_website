import { describe, expect, it } from '@jest/globals';
import {
    getAllowedRequestTransitions,
    isRequestOpen
} from './request-workflow';

describe('request workflow', () => {
    it('routes pending requests according to request type', () => {
        expect(getAllowedRequestTransitions('PENDING', 'CUSTOM')).toEqual([
            'REVIEWING',
            'REJECTED',
            'CANCELLED'
        ]);
        expect(getAllowedRequestTransitions('PENDING', 'PREORDER')).toEqual([
            'REVIEWING',
            'REJECTED',
            'CANCELLED'
        ]);
        expect(getAllowedRequestTransitions('PENDING', 'FINDING')).toEqual([
            'SOURCING',
            'REJECTED',
            'CANCELLED'
        ]);
    });

    it('supports the backend negotiation loop', () => {
        expect(getAllowedRequestTransitions('REVIEWING', 'CUSTOM')).toContain('SOURCING');
        expect(getAllowedRequestTransitions('SOURCING', 'FINDING')).toContain('REVIEWING');
        expect(getAllowedRequestTransitions('WAITING_CUSTOMER', 'PREORDER')).toEqual(
            expect.arrayContaining(['REVIEWING', 'SOURCING', 'APPROVED'])
        );
    });

    it('locks terminal states except approved cancellation', () => {
        expect(isRequestOpen('APPROVED')).toBe(false);
        expect(isRequestOpen('REJECTED')).toBe(false);
        expect(isRequestOpen('CANCELLED')).toBe(false);
        expect(getAllowedRequestTransitions('APPROVED', 'CUSTOM')).toEqual(['CANCELLED']);
        expect(getAllowedRequestTransitions('REJECTED', 'CUSTOM')).toEqual([]);
        expect(getAllowedRequestTransitions('CANCELLED', 'CUSTOM')).toEqual([]);
    });
});
