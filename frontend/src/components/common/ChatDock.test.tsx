import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

const mockGetMessages: any = jest.fn();
const mockGetAccessToken: any = jest.fn();
const mockToastError: any = jest.fn();

const mockUser = {
    id: 7,
    email: 'user@example.com',
    fullName: 'SOBU User',
    phone: '0900000000',
    status: 'ACTIVE',
    role: {
        id: 1,
        name: 'USER'
    }
};

const mockConfigMap = {
    social_chat_widget_enabled: 'true',
    social_chat_config: JSON.stringify({
        provider: 'zalo',
        pageId: '123456789',
    }),
    social_links: JSON.stringify({zalo: 'https://zalo.me/sobu'}),
};

jest.mock('../../store/useAuthStore', () => ({
    useAuthStore: () => ({
        isAuthenticated: true,
        user: mockUser
    }),
}));

jest.mock('../../utils/auth-storage', () => ({
    authStorage: {
        getAccessToken: mockGetAccessToken
    }
}));

jest.mock('../../service/support-chat.service', () => ({
    SupportChatService: {
        getMessages: mockGetMessages
    }
}));

jest.mock('../../service/toast.service', () => ({
    ToastService: {
        error: mockToastError
    }
}));

jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({configMap: mockConfigMap}),
}));

class MockWebSocket {
    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;

    url: string;
    readyState = MockWebSocket.CONNECTING;
    send = jest.fn();
    close = jest.fn();
    onopen: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    onerror: ((event: Event) => void) | null = null;
    onclose: ((event: CloseEvent) => void) | null = null;

    constructor(url: string) {
        this.url = url;
    }
}

const ChatDock = require('./ChatDock').default;

describe('ChatDock', () => {
    beforeEach(() => {
        (global as any).WebSocket = MockWebSocket;
        jest.clearAllMocks();
        mockGetAccessToken.mockReturnValue('jwt-token');
        mockGetMessages.mockReturnValue(new Promise(() => undefined));
    });

    it('renders support chat above Zalo chat inside the fixed wrapper', () => {
        const { container } = render(<ChatDock/>);

        expect(screen.getByLabelText('Thanh chat ho tro')).toBeTruthy();
        expect(screen.getByLabelText('Thanh chat ho tro khach hang')).toBeTruthy();
        expect(screen.getByLabelText('Thanh chat Zalo')).toBeTruthy();

        const dockOrder = Array.from(container.querySelectorAll('[aria-label="Thanh chat ho tro khach hang"], [aria-label="Thanh chat Zalo"]'))
            .map((element) => element.getAttribute('aria-label'));
        expect(dockOrder).toEqual(['Thanh chat ho tro khach hang', 'Thanh chat Zalo']);
    });
});
