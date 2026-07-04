import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {render, screen, waitFor} from '@testing-library/react';
import HomePage from './HomePage';

const mockFetchProducts = jest.fn();
const mockGetLatestPublicReviews = jest.fn() as any;
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

jest.mock('../service/review.service', () => ({
    ReviewService: {
        getLatestPublicReviews: (...args: any[]) => mockGetLatestPublicReviews(...args),
    },
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
        mockGetLatestPublicReviews.mockReturnValue(new Promise(() => {}));
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

    it('loads and renders up to six latest customer reviews', async () => {
        mockGetLatestPublicReviews.mockResolvedValue({
            content: Array.from({length: 7}).map((_, index) => ({
                id: index + 1,
                productId: 100 + index,
                rating: 5,
                content: `Review ${index + 1}`,
                customerName: `Customer ${index + 1}`,
                createdAt: '2026-07-01T10:00:00',
            })),
            pageNumber: 0,
            pageSize: 6,
            totalElements: 7,
            totalPages: 2,
            first: true,
            last: false,
            hasNext: true,
            hasPrevious: false,
        });

        render(<HomePage/>);

        await waitFor(() => expect(screen.getByText('Review 1')).toBeTruthy());

        expect(mockGetLatestPublicReviews).toHaveBeenCalledWith({
            page: 0,
            size: 6,
            sortBy: 'createdAt',
            sortDirection: 'DESC',
        });
        expect(screen.getAllByText(/^Review /)).toHaveLength(6);
        expect(screen.queryByText('Review 7')).toBeNull();
    });

    it('renders banners from canonical snake_case positions', () => {
        mockPublicUiState.banners = [
            banner(1, 'Home Hero', 'home_hero_carousel'),
            banner(2, 'Best Seller Banner', 'home_section_01_banner'),
            banner(3, 'First Category Banner', 'home_category_card_01'),
            banner(4, 'Tools Banner', 'home_section_02_banner'),
            banner(5, 'Hotwheels Banner', 'home_section_03_banner'),
            banner(6, 'Sale Banner', 'home_section_04_banner'),
            banner(7, 'Promo Top Left', 'home_promo_grid_top_left'),
            banner(8, 'Promo Bottom Left', 'home_promo_grid_bottom_left'),
            banner(9, 'Promo Top Right', 'home_promo_grid_top_right'),
            banner(10, 'Promo Bottom Right', 'home_promo_grid_bottom_right'),
        ];

        render(<HomePage/>);

        expect(screen.getByText('Home Hero')).toBeTruthy();
        expect(screen.getByText('Best Seller Banner')).toBeTruthy();
        expect(screen.getByText('First Category Banner')).toBeTruthy();
        expect(screen.getByText('Tools Banner')).toBeTruthy();
        expect(screen.getByText('Hotwheels Banner')).toBeTruthy();
        expect(screen.getByText('Sale Banner')).toBeTruthy();
        expect(screen.getByText('Promo Top Left')).toBeTruthy();
        expect(screen.getByText('Promo Bottom Left')).toBeTruthy();
        expect(screen.getByText('Promo Top Right')).toBeTruthy();
        expect(screen.getByText('Promo Bottom Right')).toBeTruthy();
    });

    it('keeps the promo section as a unified 2x2 image grid', () => {
        render(<HomePage/>);

        const promoGrid = screen.getByTestId('home-promo-grid');
        const promoTiles = screen.getAllByTestId(/^promo-tile-/);

        expect(promoGrid.className).toContain('grid-cols-1');
        expect(promoGrid.className).toContain('md:grid-cols-2');
        expect(promoGrid.className).toContain('gap-3');
        expect(promoTiles).toHaveLength(4);
        expect(promoTiles.map((tile) => tile.getAttribute('data-testid'))).toEqual([
            'promo-tile-home_promo_grid_top_left',
            'promo-tile-home_promo_grid_top_right',
            'promo-tile-home_promo_grid_bottom_left',
            'promo-tile-home_promo_grid_bottom_right',
        ]);
        promoTiles.forEach((tile) => {
            expect(tile.className).toContain('rounded-2xl');
            expect(tile.className).toContain('overflow-hidden');
        });
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
