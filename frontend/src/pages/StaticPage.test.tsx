import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {render, screen, waitFor} from '@testing-library/react';
import StaticPage from './StaticPage';
import {StaticPageService} from '../service/static-page.service';

jest.mock('../service/static-page.service');

const mockedService = jest.mocked(StaticPageService);

describe('StaticPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('fetches by slug and renders sanitized html from the API', async () => {
        mockedService.getPublishedPageBySlug.mockResolvedValue({
            id: 1,
            slug: 'privacy-policy',
            title: 'Privacy Policy',
            htmlContent: '<h2>Privacy</h2><p>Your data.</p>',
            isPublished: true,
        });

        const {container} = render(<StaticPage slug="privacy-policy"/>);

        expect(await screen.findByRole('heading', {name: 'Privacy Policy'})).toBeTruthy();
        expect(container.querySelector('.static-page-content')?.innerHTML).toContain('<h2>Privacy</h2>');
        expect(mockedService.getPublishedPageBySlug).toHaveBeenCalledWith('privacy-policy');
    });

    it('adds enough top spacing for the fixed header', async () => {
        mockedService.getPublishedPageBySlug.mockResolvedValue({
            id: 1,
            slug: 'terms',
            title: 'Terms',
            htmlContent: '<p>Terms content.</p>',
            isPublished: true,
        });

        const {container} = render(<StaticPage slug="terms"/>);

        expect(await screen.findByRole('heading', {name: 'Terms'})).toBeTruthy();
        const main = container.querySelector('main');
        expect(main?.className).toContain('pt-32');
        expect(main?.className).toContain('sm:pt-36');
        expect(main?.className).toContain('lg:pt-40');
    });

    it('constrains long static content inside the page width', async () => {
        mockedService.getPublishedPageBySlug.mockResolvedValue({
            id: 1,
            slug: 'about',
            title: 'About',
            htmlContent: `<p>${'long-static-page-content-'.repeat(30)}</p>`,
            isPublished: true,
        });

        const {container} = render(<StaticPage slug="about"/>);

        expect(await screen.findByRole('heading', {name: 'About'})).toBeTruthy();
        const article = container.querySelector('article');
        const content = container.querySelector('.static-page-content');
        expect(article?.className).toContain('w-full');
        expect(article?.className).toContain('min-w-0');
        expect(content?.className).toContain('max-w-full');
        expect(content?.className).toContain('min-w-0');
    });

    it('shows empty content state for published blank pages', async () => {
        mockedService.getPublishedPageBySlug.mockResolvedValue({
            id: 1,
            slug: 'about',
            title: 'About',
            htmlContent: '',
            isPublished: true,
        });

        render(<StaticPage slug="about"/>);

        expect(await screen.findByRole('heading', {name: 'Nội dung đang được cập nhật'})).toBeTruthy();
    });

    it('shows not found state on load error', async () => {
        mockedService.getPublishedPageBySlug.mockRejectedValue(new Error('Not found'));

        render(<StaticPage slug="missing"/>);

        await waitFor(() => expect(screen.getByRole('heading', {name: 'Không tìm thấy trang'})).toBeTruthy());
        expect(screen.getByText(/Not found/i)).toBeTruthy();
    });
});
