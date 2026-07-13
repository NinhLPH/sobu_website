import { render, screen } from '@testing-library/react';
import { describe, expect, it, jest } from '@jest/globals';
import LegacyTrackingRedirect from './LegacyTrackingRedirect';

let mockSearch = '';

jest.mock('react-router-dom', () => ({
    Navigate: ({ to }: { to: string }) => <div data-testid="redirect" data-to={to} />,
    useLocation: () => ({ search: mockSearch })
}), { virtual: true });

describe('LegacyTrackingRedirect', () => {
    it('preserves internal order and payment setup links', () => {
        mockSearch = '?orderId=12&paymentSetup=cod';
        render(<LegacyTrackingRedirect />);

        expect(screen.getByTestId('redirect').getAttribute('data-to')).toBe('/orders/12?paymentSetup=cod');
    });

    it('preserves Nhanh lookup links', () => {
        mockSearch = '?nhanhOrderId=NH-12';
        render(<LegacyTrackingRedirect />);

        expect(screen.getByTestId('redirect').getAttribute('data-to')).toBe('/orders/lookup?nhanhOrderId=NH-12');
    });
});
