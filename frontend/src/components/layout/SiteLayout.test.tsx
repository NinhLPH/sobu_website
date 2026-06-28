import {describe, expect, it, jest} from '@jest/globals';
import {render, screen} from '@testing-library/react';
import SiteLayout from './SiteLayout';

const mockFetchBanners = jest.fn(async () => undefined);
jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({fetchBanners: mockFetchBanners, banners: []}),
    getBannersForPlacement: () => [],
}));

jest.mock('react-router-dom', () => ({
    Outlet: () => <main>Danh sách sản phẩm</main>,
}), {virtual: true});

describe('SiteLayout', () => {
    it('renders the customer outlet between two desktop banner placeholders', () => {
        render(<SiteLayout/>);

        expect(screen.getByText('Danh sách sản phẩm')).toBeTruthy();
        expect(screen.getByLabelText('Quảng cáo bên trái')).toBeTruthy();
        expect(screen.getByLabelText('Quảng cáo bên phải')).toBeTruthy();
        expect(screen.getByLabelText('Quảng cáo trái')).toBeTruthy();
        expect(screen.getByLabelText('Quảng cáo phải')).toBeTruthy();

        const mainColumn = screen.getByText('Danh sách sản phẩm').parentElement;
        expect(mainColumn?.className).toContain('min-w-0');
        expect(screen.getByLabelText('Quảng cáo bên trái').className).toContain('min-[1440px]:block');
    });
});
