import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import ZaloChatDock from './ZaloChatDock';

let mockConfigMap: Record<string, string> = {};

jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({
        configMap: mockConfigMap,
        configsLoaded: true,
        isConfigsLoading: false,
        configsError: null,
    }),
}));

const socialLinks = {
    facebook: 'https://facebook.com/sobu',
    instagram: 'https://instagram.com/sobu',
    zalo: 'https://zalo.me/sobu',
    tiktok: 'https://tiktok.com/@sobu',
};

describe('ZaloChatDock', () => {
    beforeEach(() => {
        mockConfigMap = {
            social_chat_widget_enabled: 'false',
            social_chat_config: JSON.stringify({
                provider: 'disabled-provider',
                pageId: '',
            }),
            social_links: JSON.stringify(socialLinks),
        };
        document.getElementById('zalo-sdk-script')?.remove();
        document.body.innerHTML = '';
        jest.clearAllMocks();
    });

    it('renders configured social links from social_links only', () => {
        render(<ZaloChatDock/>);

        expect(screen.getByLabelText('Thanh lien ket social')).toBeTruthy();
        expect(screen.getByRole('link', {name: 'Mo Facebook'})).toBeTruthy();
        expect(screen.getByRole('link', {name: 'Mo Instagram'})).toBeTruthy();
        expect(screen.getByRole('link', {name: 'Mo Zalo'})).toBeTruthy();
        expect(screen.getByRole('link', {name: 'Mo Tiktok'})).toBeTruthy();
    });

    it('opens each configured social link in a new tab', () => {
        render(<ZaloChatDock/>);

        Object.entries({
            Facebook: socialLinks.facebook,
            Instagram: socialLinks.instagram,
            Zalo: socialLinks.zalo,
            Tiktok: socialLinks.tiktok,
        }).forEach(([label, href]) => {
            const link = screen.getByRole('link', {name: `Mo ${label}`}) as HTMLAnchorElement;
            expect(link.getAttribute('href')).toBe(href);
            expect(link.getAttribute('target')).toBe('_blank');
            expect(link.getAttribute('rel')).toBe('noreferrer');
        });
    });

    it('hides blank and missing social links', () => {
        mockConfigMap = {
            social_links: JSON.stringify({
                facebook: 'https://facebook.com/sobu',
                instagram: '   ',
                zalo: '',
            }),
        };

        render(<ZaloChatDock/>);

        expect(screen.getByRole('link', {name: 'Mo Facebook'})).toBeTruthy();
        expect(screen.queryByRole('link', {name: 'Mo Instagram'})).toBeNull();
        expect(screen.queryByRole('link', {name: 'Mo Zalo'})).toBeNull();
        expect(screen.queryByRole('link', {name: 'Mo Tiktok'})).toBeNull();
    });

    it('does not render when social_links has no supported URLs', () => {
        mockConfigMap = {
            social_links: JSON.stringify({
                youtube: 'https://youtube.com/@sobu',
            }),
        };

        render(<ZaloChatDock/>);

        expect(screen.queryByLabelText('Thanh lien ket social')).toBeNull();
    });

    it('does not inject the Zalo SDK script', () => {
        render(<ZaloChatDock/>);

        expect(document.getElementById('zalo-sdk-script')).toBeNull();
    });
});
