import {describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import BannerCarousel from './BannerCarousel';

jest.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: React.ReactNode; to: string}) => <a href={to}>{children}</a>,
}), {virtual: true});

const banners = [
    {id: 1, title: 'Banner một', imageUrl: 'one.jpg', position: 'home_section_01_banner', deviceType: 'ALL', isActive: true},
    {id: 2, title: 'Banner hai', imageUrl: 'two.jpg', position: 'home_section_01_banner', deviceType: 'ALL', isActive: true},
] as any;

describe('BannerCarousel', () => {
    it('uses fallback for an empty API result', () => {
        render(<BannerCarousel banners={[]} fallback={<span>Ảnh hiện tại</span>}/>);
        expect(screen.getByText('Ảnh hiện tại')).toBeTruthy();
    });

    it('renders API slides and supports manual navigation', () => {
        const {container} = render(<BannerCarousel banners={banners} fallback={null}/>);
        expect(screen.getByText('Banner một')).toBeTruthy();
        fireEvent.click(screen.getByRole('button', {name: 'Banner sau'}));
        const slides = container.querySelectorAll('.absolute.inset-0.transition-opacity');
        expect(slides[1].className).toContain('opacity-100');
    });
});
