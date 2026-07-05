import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen, within} from '@testing-library/react';
import Header from './Header';
import {useCartStore} from '../../store/useCartStore';
import {useProductStore} from '../../store/useProductStore';
import {useAuthStore} from '../../store/useAuthStore';

jest.mock('../../store/useCartStore');
jest.mock('../../store/useProductStore');
jest.mock('../../store/useAuthStore');

const mockNavigate = jest.fn();
let mockPathname = '/';

jest.mock('react-router-dom', () => ({
    Link: ({children, to, ...props}: {children: React.ReactNode; to: string}) => <a href={to} {...props}>{children}</a>,
    useLocation: () => ({pathname: mockPathname}),
    useNavigate: () => mockNavigate,
}), {virtual: true});

const mockedCartStore = jest.mocked(useCartStore);
const mockedProductStore = jest.mocked(useProductStore);
const mockedAuthStore = jest.mocked(useAuthStore);

describe('Header mobile navigation', () => {
    beforeEach(() => {
        mockPathname = '/';
        mockNavigate.mockClear();
        window.localStorage.clear();
        document.documentElement.classList.remove('dark', 'light');
        document.documentElement.removeAttribute('data-theme');
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
        mockedCartStore.mockReturnValue({
            items: [],
            removeFromCart: jest.fn(),
            getTotals: () => ({subtotal: 0, itemCount: 0}),
        } as any);
        mockedProductStore.mockReturnValue({
            categories: [],
            brands: [],
            allProducts: [
                {id: 1, code: 'SD-SERUM', name: 'Sodu Serum', categoryName: 'Skincare', brandName: 'Sodu'},
                {id: 2, code: 'GD-001', name: 'Gundam RX-78', categoryName: 'Mecha', brandName: 'Bandai'},
            ],
            allProductsLoaded: true,
            categoriesLoaded: true,
            brandsLoaded: true,
            fetchAllProducts: jest.fn(),
            fetchCategories: jest.fn(),
            fetchBrands: jest.fn(),
        } as any);
        mockedAuthStore.mockReturnValue({
            isAuthenticated: false,
            user: null,
            logoutAction: jest.fn(),
        } as any);
    });

    it('renders the mobile header structure without the old hamburger panel', () => {
        const {container} = render(<Header/>);

        expect(screen.getByRole('link', {name: 'SOBU'}).getAttribute('href')).toBe('/');
        expect(screen.getByTitle('Giỏ hàng')).toBeTruthy();
        expect(screen.getByTitle('Đăng nhập / Đăng ký')).toBeTruthy();
        expect(screen.getAllByPlaceholderText('Tìm kiếm mô hình...')).toHaveLength(2);
        expect(screen.queryByRole('button', {name: 'Mở menu'})).toBeNull();
        expect(container.querySelector('#mobile-navigation')).toBeNull();
    });

    it('renders the fixed mobile bottom toolbar with five primary links', () => {
        mockPathname = '/products';
        const {container} = render(<Header/>);

        const toolbar = screen.getByRole('navigation', {name: 'Thanh điều hướng mobile'});
        expect(container.querySelector('header')?.contains(toolbar)).toBe(false);
        expect(within(toolbar).getByRole('link', {name: 'Trang chủ'}).getAttribute('href')).toBe('/');
        expect(within(toolbar).getByRole('link', {name: 'Sản phẩm'}).getAttribute('href')).toBe('/products');
        expect(within(toolbar).getByRole('link', {name: 'Dịch vụ'}).getAttribute('href')).toBe('/services');
        expect(within(toolbar).getByRole('link', {name: 'Thành viên'}).getAttribute('href')).toBe('/membership');
        expect(within(toolbar).getByRole('link', {name: 'Tin tức'}).getAttribute('href')).toBe('/blog');
        expect(within(toolbar).getByRole('link', {name: 'Sản phẩm'}).getAttribute('aria-current')).toBe('page');
    });

    it('keeps desktop navigation ordered as category, search, service, membership, and news', () => {
        render(<Header/>);

        const nav = screen.getByRole('navigation', {name: 'Điều hướng chính'});
        const category = within(nav).getByRole('link', {name: /Danh mục/});
        const search = within(nav).getByPlaceholderText('Tìm kiếm mô hình...');
        const services = within(nav).getByRole('link', {name: 'Dịch vụ'});
        const membership = within(nav).getByRole('link', {name: 'Thẻ thành viên'});
        const news = within(nav).getByRole('link', {name: 'Tin tức'});

        expect(category.compareDocumentPosition(search) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(search.compareDocumentPosition(services) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(services.compareDocumentPosition(membership) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(membership.compareDocumentPosition(news) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('submits mobile search to the products search route', () => {
        render(<Header/>);

        const inputs = screen.getAllByPlaceholderText('Tìm kiếm mô hình...') as HTMLInputElement[];
        const mobileInput = inputs[inputs.length - 1];
        fireEvent.change(mobileInput, {target: {value: '  sodu serum  '}});
        fireEvent.submit(mobileInput.closest('form') as HTMLFormElement);

        expect(mockNavigate).toHaveBeenCalledWith('/products?search=sodu%20serum');
    });

    it('selects a header suggestion and navigates to product search', () => {
        render(<Header/>);

        const desktopInput = screen.getAllByPlaceholderText('Tìm kiếm mô hình...')[0];
        fireEvent.change(desktopInput, {target: {value: 'ser'}});
        fireEvent.mouseDown(screen.getByRole('option', {name: /Sodu Serum/i}));

        expect(mockNavigate).toHaveBeenCalledWith('/products?search=Sodu%20Serum');
    });

    it('opens a guest user dropdown with dark mode and login actions', () => {
        render(<Header/>);

        fireEvent.click(screen.getByTitle('Đăng nhập / Đăng ký'));

        expect(screen.getByText('Tài khoản')).toBeTruthy();
        expect(screen.getAllByRole('button', {name: 'Đăng nhập / Đăng ký'}).length).toBeGreaterThanOrEqual(2);
        expect(screen.getAllByRole('button', {name: 'Chuyển sang giao diện tối'}).length).toBeGreaterThanOrEqual(1);
    });

    it('keeps authenticated account links and logout in the user dropdown', () => {
        const logoutAction = jest.fn(async () => undefined);
        mockedAuthStore.mockReturnValue({
            isAuthenticated: true,
            user: {
                fullName: 'Sobu Admin',
                email: 'admin@sobu.vn',
                role: {name: 'ADMIN'},
            },
            logoutAction,
        } as any);
        render(<Header/>);

        fireEvent.click(screen.getByTitle('Tài khoản của bạn'));

        expect(screen.getByText('Sobu Admin')).toBeTruthy();
        expect(screen.getByRole('link', {name: 'Trang quản trị (Admin)'}).getAttribute('href')).toBe('/admin');
        expect(screen.getByRole('link', {name: 'Yêu cầu của tôi'}).getAttribute('href')).toBe('/requests');
        expect(screen.getByRole('link', {name: 'Tra cứu đơn hàng'}).getAttribute('href')).toBe('/tracking');
        expect(screen.getByRole('button', {name: 'Đăng xuất'})).toBeTruthy();
    });

    it('persists the selected light and dark theme from the user dropdown', () => {
        window.localStorage.setItem('sobu-theme', 'dark');
        render(<Header/>);

        expect(document.documentElement.classList.contains('dark')).toBe(true);
        expect(document.documentElement.dataset.theme).toBe('dark');

        fireEvent.click(screen.getByTitle('Đăng nhập / Đăng ký'));
        const themeButtons = screen.getAllByRole('button', {name: 'Chuyển sang giao diện sáng'});
        fireEvent.click(themeButtons[themeButtons.length - 1]);

        expect(window.localStorage.getItem('sobu-theme')).toBe('light');
        expect(document.documentElement.classList.contains('light')).toBe(true);
        expect(document.documentElement.classList.contains('dark')).toBe(false);
        expect(document.documentElement.dataset.theme).toBe('light');
    });
});
