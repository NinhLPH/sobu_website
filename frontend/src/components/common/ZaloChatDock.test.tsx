import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import ZaloChatDock from './ZaloChatDock';

let mockConfigMap: Record<string, string> = {};

jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({configMap: mockConfigMap}),
}));

const enabledConfig = {
    social_chat_widget_enabled: 'true',
    social_chat_config: JSON.stringify({
        provider: 'zalo',
        pageId: '123456789',
        greetingText: 'SOBU can help',
        autoPopup: true,
        width: 360,
        height: 460,
    }),
    social_links: JSON.stringify({zalo: 'https://zalo.me/sobu'}),
};

describe('ZaloChatDock', () => {
    beforeEach(() => {
        mockConfigMap = {...enabledConfig};
        document.getElementById('zalo-sdk-script')?.remove();
        document.body.innerHTML = '';
        delete window.ZaloSocialSDK;
        jest.clearAllMocks();
    });

    it('does not render when disabled', () => {
        mockConfigMap = {
            ...enabledConfig,
            social_chat_widget_enabled: 'false',
        };

        render(<ZaloChatDock/>);

        expect(screen.queryByRole('button', {name: 'Mo chat Zalo'})).toBeNull();
    });

    it('renders a fallback panel when enabled without the Zalo page id', () => {
        mockConfigMap = {
            ...enabledConfig,
            social_chat_config: JSON.stringify({provider: 'zalo'}),
        };

        render(<ZaloChatDock/>);

        expect(screen.getByRole('button', {name: 'Mo chat Zalo'})).toBeTruthy();
        fireEvent.click(screen.getByRole('button', {name: 'Mo chat Zalo'}));

        expect(screen.getByText('Chat Zalo chua san sang')).toBeTruthy();
        expect(screen.getByText(/cau hinh Zalo OA\/pageId/i)).toBeTruthy();
        expect(document.getElementById('zalo-sdk-script')).toBeNull();
    });

    it('shows the configured Zalo fallback link when enabled without the page id', () => {
        mockConfigMap = {
            ...enabledConfig,
            social_chat_config: JSON.stringify({provider: 'zalo'}),
        };

        render(<ZaloChatDock/>);

        fireEvent.click(screen.getByRole('button', {name: 'Mo chat Zalo'}));

        const fallbackLink = screen.getByRole('link', {name: /Mo Zalo/i}) as HTMLAnchorElement;
        expect(fallbackLink.getAttribute('href')).toBe('https://zalo.me/sobu');
        expect(document.getElementById('zalo-sdk-script')).toBeNull();
    });

    it('renders the Zalo bubble when enabled', () => {
        render(<ZaloChatDock/>);

        expect(screen.getByLabelText('Thanh chat Zalo')).toBeTruthy();
        expect(screen.getByRole('button', {name: 'Mo chat Zalo'})).toBeTruthy();
    });

    it('opens the panel and renders the configured Zalo widget attributes', () => {
        const {container} = render(<ZaloChatDock/>);

        fireEvent.click(screen.getByRole('button', {name: 'Mo chat Zalo'}));

        expect(screen.getByLabelText('Zalo chat')).toBeTruthy();
        expect(screen.getByText('Dang tai Zalo...')).toBeTruthy();

        const widget = container.querySelector('.zalo-chat-widget') as HTMLElement;
        expect(widget).toBeTruthy();
        expect(widget.dataset.oaid).toBe('123456789');
        expect(widget.dataset.welcomeMessage).toBe('SOBU can help');
        expect(widget.dataset.autopopup).toBe('1');
        expect(widget.dataset.width).toBe('360');
        expect(widget.dataset.height).toBe('460');
    });

    it('injects the Zalo SDK script only once', () => {
        render(<ZaloChatDock/>);

        fireEvent.click(screen.getByRole('button', {name: 'Mo chat Zalo'}));
        fireEvent.click(screen.getByRole('button', {name: 'An chat Zalo'}));
        fireEvent.click(screen.getByRole('button', {name: 'Mo chat Zalo'}));

        expect(document.querySelectorAll('#zalo-sdk-script')).toHaveLength(1);
        expect((document.getElementById('zalo-sdk-script') as HTMLScriptElement).src)
            .toBe('https://sp.zalo.me/plugins/sdk.js');
    });

    it('shows the configured Zalo fallback link when the SDK fails', async () => {
        render(<ZaloChatDock/>);

        fireEvent.click(screen.getByRole('button', {name: 'Mo chat Zalo'}));

        const script = document.getElementById('zalo-sdk-script');
        expect(script).toBeTruthy();

        act(() => {
            script?.dispatchEvent(new Event('error'));
        });

        await waitFor(() => {
            expect(screen.getByText('Khong tai duoc Zalo chat')).toBeTruthy();
        });

        const fallbackLink = screen.getByRole('link', {name: /Mo Zalo/i}) as HTMLAnchorElement;
        expect(fallbackLink.getAttribute('href')).toBe('https://zalo.me/sobu');
    });
});
