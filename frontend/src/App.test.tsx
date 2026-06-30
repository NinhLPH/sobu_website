import {describe, expect, it, jest} from '@jest/globals';
import {render} from '@testing-library/react';
import App from './App';

const mockFetchConfigs = jest.fn(async () => undefined);
jest.mock('./store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({fetchConfigs: mockFetchConfigs}),
}));

jest.mock('react-router-dom', () => ({
    BrowserRouter: ({children}: any) => <>{children}</>,
    Routes: ({children}: any) => <>{children}</>,
    Route: ({path, element, children}: any) => (
        <div data-path={path || ''} data-element={element?.type?.name || ''}>{children}</div>
    ),
    Navigate: function Navigate() { return null; },
    Outlet: () => null,
}), {virtual: true});

jest.mock('./components/common/Header', () => () => null);
jest.mock('./components/common/Footer', () => () => null);
jest.mock('./components/common/Toast', () => () => null);
jest.mock('./components/common/ScrollToTop', () => () => null);
jest.mock('./components/common/ProtectedRoute', () => ({__esModule: true, default: function ProtectedRoute() { return null; }}));
jest.mock('./components/layout/SiteLayout', () => ({__esModule: true, default: function SiteLayout() { return null; }}));
jest.mock('./components/admin/AdminLayout', () => () => null);

jest.mock('./pages/HomePage', () => () => null);
jest.mock('./pages/LoginPage', () => () => null);
jest.mock('./pages/ProductList', () => () => null);
jest.mock('./pages/ProductDetails', () => () => null);
jest.mock('./pages/BlogList', () => () => null);
jest.mock('./pages/BlogDetail', () => () => null);
jest.mock('./pages/ServicesLandingPage', () => () => null);
jest.mock('./pages/Membership', () => () => null);
jest.mock('./pages/OrderTracking', () => () => null);
jest.mock('./pages/PaymentResult', () => () => null);
jest.mock('./pages/StaticPage', () => () => null);
jest.mock('./pages/Cart', () => () => null);
jest.mock('./pages/MyRequests', () => () => null);
jest.mock('./pages/CreateRequest', () => () => null);
jest.mock('./pages/RequestDetail', () => () => null);
jest.mock('./pages/admin/Products', () => () => null);
jest.mock('./pages/admin/Categories', () => () => null);
jest.mock('./pages/admin/Brands', () => () => null);
jest.mock('./pages/admin/Orders', () => () => null);
jest.mock('./pages/admin/Requests', () => () => null);
jest.mock('./pages/admin/OrderDetails', () => () => null);
jest.mock('./pages/admin/Sync', () => () => null);
jest.mock('./pages/admin/NhanhCallback', () => () => null);
jest.mock('./pages/admin/Banners', () => () => null);
jest.mock('./pages/admin/Configs', () => () => null);
jest.mock('./pages/admin/StaticPages', () => () => null);

describe('App route layout', () => {
    it('places customer routes inside SiteLayout and keeps auth/admin routes outside', () => {
        const {container} = render(<App/>);
        const siteLayout = container.querySelector('[data-element="SiteLayout"]');

        expect(siteLayout?.querySelector('[data-path="/products"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/cart"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/payment-result"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/about"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/privacy"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/terms"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/policies/privacy"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/policies/terms"]')).toBeTruthy();
        expect(siteLayout?.querySelector('[data-path="/login"]')).toBeNull();
        expect(siteLayout?.querySelector('[data-path="/activate"]')).toBeNull();
        expect(siteLayout?.querySelector('[data-path="/verify-email"]')).toBeNull();
        expect(siteLayout?.querySelector('[data-path="/admin"]')).toBeNull();
        expect(container.querySelector('[data-path="/login"]')).toBeTruthy();
        expect(container.querySelector('[data-path="/activate"]')?.getAttribute('data-element')).toBe('Navigate');
        expect(container.querySelector('[data-path="/verify-email"]')?.getAttribute('data-element')).toBe('Navigate');
        expect(container.querySelector('[data-path="/admin"]')).toBeTruthy();
        expect(container.querySelector('[data-path="banners"]')).toBeTruthy();
        expect(container.querySelector('[data-path="configs"]')).toBeTruthy();
        expect(container.querySelector('[data-path="static-pages"]')).toBeTruthy();
    });
});
