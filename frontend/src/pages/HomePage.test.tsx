import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {render, screen} from '@testing-library/react';
import HomePage from './HomePage';

const mockFetchProducts = jest.fn();
let mockPublicUiState: any;

jest.mock('react-router-dom', () => ({
    Link: ({children, to, ...props}: any) => <a href={to} {...props}>{children}</a>,
}), {virtual: true});

jest.mock('../store/useProductStore', () => ({
    useProductStore: () => ({
        products: [],
        fetchProducts: mockFetchProducts,
    }),
}));

jest.mock('../store/usePublicUiStore', () => {
    return {
        usePublicUiStore: (selector: any) => selector(mockPublicUiState),
        getBannersForPlacement: (banners: any[], position: string, deviceType: string) => banners
            .filter((banner) =>
                banner.position === position &&
                banner.isActive &&
                (banner.deviceType === deviceType || banner.deviceType === 'ALL')
            )
            .sort((left, right) => (left.displayOrder ?? Number.MAX_SAFE_INTEGER) - (right.displayOrder ?? Number.MAX_SAFE_INTEGER)),
    };
});

jest.mock('../hooks/useResponsiveDeviceType', () => ({
    useResponsiveDeviceType: () => 'WEB',
}));

jest.mock('../components/common/ProductSlider', () => ({
    __esModule: true,
    default: ({products}: any) => <div data-testid="product-slider">{products.length} products</div>,
}));

jest.mock('../components/common/BannerMedia', () => ({
    __esModule: true,
    default: ({banner, fallback, children}: any) => banner
        ? <div data-testid={`banner-media-${banner.position}`}>{banner.title}{children}</div>
        : <>{fallback}</>,
}));

jest.mock('../components/common/BannerCarousel', () => ({
    __esModule: true,
    default: ({banners, fallback}: any) => banners?.length
        ? <div data-testid="banner-carousel">{banners.map((banner: any) => <span key={banner.id}>{banner.title}</span>)}</div>
        : <>{fallback}</>,
}));

const banner = (id: number, title: string, position: string, displayOrder = 1) => ({
    id,
    title,
    position,
    displayOrder,
    imageUrl: `${title}.jpg`,
    isActive: true,
    deviceType: 'ALL',
});

describe('HomePage config and banner rendering', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockPublicUiState = {
            banners: [],
            configMap: {},
        };
    });

    it('renders HomePage text and repeated content from public config', () => {
        mockPublicUiState.configMap = {
            home_section_01_title: 'Admin Best Sellers',
            home_custom_service_badges: '["Badge A","Badge B"]',
            home_category_title: 'Admin Categories',
            home_category_cards: '[{"label":"Admin Cat","href":"/admin-cat","bannerPosition":"home_category_card_01"}]',
            home_promo_grid_top_left_title: 'Admin Promo',
            home_partners_title: 'Admin Partners',
            home_partner_brands: '[{"name":"ADMIN BRAND","logoUrl":"brand.jpg"}]',
            home_testimonials_title: 'Admin Testimonials',
        };

        render(<HomePage/>);

        expect(screen.getByText('Admin Best Sellers')).toBeTruthy();
        expect(screen.getByText('Badge A')).toBeTruthy();
        expect(screen.getByText('Admin Categories')).toBeTruthy();
        expect(screen.getByText('Admin Cat')).toBeTruthy();
        expect(screen.getByText('Admin Promo')).toBeTruthy();
        expect(screen.getByText('Admin Partners')).toBeTruthy();
        expect(screen.getByRole('img', {name: 'ADMIN BRAND'})).toBeTruthy();
        expect(screen.getByText('Admin Testimonials')).toBeTruthy();
    });

    it('renders banners from canonical snake_case positions', () => {
        mockPublicUiState.banners = [
            banner(1, 'Home Hero', 'home_hero_carousel'),
            banner(2, 'Best Seller Banner', 'home_section_01_banner'),
            banner(3, 'First Category Banner', 'home_category_card_01'),
            banner(4, 'Tools Banner', 'home_section_02_banner'),
            banner(5, 'Hotwheels Banner', 'home_section_03_banner'),
            banner(6, 'Sale Banner', 'home_section_04_banner'),
        ];

        render(<HomePage/>);

        expect(screen.getByText('Home Hero')).toBeTruthy();
        expect(screen.getByText('Best Seller Banner')).toBeTruthy();
        expect(screen.getByText('First Category Banner')).toBeTruthy();
        expect(screen.getByText('Tools Banner')).toBeTruthy();
        expect(screen.getByText('Hotwheels Banner')).toBeTruthy();
        expect(screen.getByText('Sale Banner')).toBeTruthy();
    });

    it('keeps fallback lists when JSON config is invalid', () => {
        mockPublicUiState.configMap = {
            home_category_cards: '{invalid',
            home_partner_brands: '{"not":"an array"}',
        };

        render(<HomePage/>);

        expect(screen.getByText('Marvel')).toBeTruthy();
        expect(screen.getByRole('img', {name: 'BANDAI'})).toBeTruthy();
    });
});
