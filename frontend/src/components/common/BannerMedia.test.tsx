import {describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import BannerMedia from './BannerMedia';

jest.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: React.ReactNode; to: string}) => <a href={to}>{children}</a>,
}), {virtual: true});

const banner = {
    id: 1, title: 'Hero API', imageUrl: '/api/public/files/banners/hero.jpg',
    linkUrl: '/products', displayOrder: 1, position: 'HOME_TOP', isActive: true, deviceType: 'ALL',
} as const;

describe('BannerMedia', () => {
    it('renders the API image inside an internal link', () => {
        render(<BannerMedia banner={banner}/>);
        expect(screen.getByRole('link').getAttribute('href')).toBe('/products');
        expect(screen.getByRole('img', {name: 'Hero API'}).getAttribute('src')).toContain('/api/public/files/banners/hero.jpg');
    });

    it('shows the fallback when the image cannot be loaded', () => {
        render(<BannerMedia banner={banner} fallback={<span>Banner fallback</span>}/>);
        fireEvent.error(screen.getByRole('img', {name: 'Hero API'}));
        expect(screen.getByText('Banner fallback')).toBeTruthy();
    });
});
