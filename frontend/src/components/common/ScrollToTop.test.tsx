import { render } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import ScrollToTop from './ScrollToTop';

let mockLocation = {
    pathname: '/tracking',
    search: '?mode=internal'
};

jest.mock('react-router-dom', () => ({
    useLocation: () => mockLocation
}), { virtual: true });

describe('ScrollToTop', () => {
    const scrollTo = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockLocation = { pathname: '/tracking', search: '?mode=internal' };
        Object.defineProperty(window, 'scrollTo', {
            configurable: true,
            writable: true,
            value: scrollTo
        });
    });

    it('scrolls on initial load and pathname changes, but not query-only changes', () => {
        const { rerender } = render(<ScrollToTop />);

        expect(scrollTo).toHaveBeenCalledTimes(1);
        expect(scrollTo).toHaveBeenLastCalledWith({ top: 0, left: 0, behavior: 'auto' });

        mockLocation = { pathname: '/tracking', search: '?mode=nhanh' };
        rerender(<ScrollToTop />);
        expect(scrollTo).toHaveBeenCalledTimes(1);

        mockLocation = { pathname: '/requests', search: '' };
        rerender(<ScrollToTop />);
        expect(scrollTo).toHaveBeenCalledTimes(2);
    });
});
