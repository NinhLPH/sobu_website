import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

const mockGetAdminConversations: any = jest.fn();
const mockGetAdminConversationMessages: any = jest.fn();
const mockGetAccessToken: any = jest.fn();
const mockToastError: any = jest.fn();

jest.mock('../../service/support-chat.service', () => ({
    SupportChatService: {
        getAdminConversations: mockGetAdminConversations,
        getAdminConversationMessages: mockGetAdminConversationMessages
    }
}));

jest.mock('../../utils/auth-storage', () => ({
    authStorage: {
        getAccessToken: mockGetAccessToken
    }
}));

jest.mock('../../service/toast.service', () => ({
    ToastService: {
        error: mockToastError
    }
}));

jest.mock('../../api/api-client', () => ({
    BASE_URL: 'http://localhost:8081'
}));

jest.mock('react-router-dom', () => ({
    Link: (props: any) => {
        const React = require('react');
        const { children, to } = props;
        return React.createElement('a', { href: to }, children);
    },
    Outlet: () => {
        const React = require('react');
        return React.createElement(React.Fragment);
    },
    useLocation: () => ({ pathname: '/admin/support' })
}), { virtual: true });

const AdminSupport = require('./Support').default;
const AdminLayout = require('../../components/admin/AdminLayout').default;
const { SupportChatService } = require('../../service/support-chat.service');

class MockWebSocket {
    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;

    static instances: MockWebSocket[] = [];

    url: string;
    readyState = MockWebSocket.CONNECTING;
    send = jest.fn();
    close = jest.fn(() => {
        this.readyState = MockWebSocket.CLOSED;
    });
    onopen: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    onerror: ((event: Event) => void) | null = null;
    onclose: ((event: CloseEvent) => void) | null = null;

    constructor(url: string) {
        this.url = url;
        MockWebSocket.instances.push(this);
    }

    triggerOpen() {
        this.readyState = MockWebSocket.OPEN;
        this.onopen?.(new Event('open'));
    }

    triggerMessage(payload: unknown) {
        this.onmessage?.({ data: JSON.stringify(payload) } as MessageEvent);
    }
}

const conversationsResponse = {
    success: true,
    message: 'Conversations retrieved',
    data: {
        content: [
            {
                id: 10,
                status: 'OPEN',
                lastMessageAt: '2026-07-03T12:00:00',
                createdAt: '2026-07-03T11:00:00',
                customerEmail: 'customer@example.com',
                customerName: 'SOBU Customer'
            }
        ],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false
    }
};

const messagesResponse = {
    success: true,
    message: 'Messages retrieved',
    data: {
        content: [
            {
                id: 2,
                conversationId: 10,
                senderId: 7,
                senderEmail: 'customer@example.com',
                senderRole: 'USER',
                content: 'Need support',
                createdAt: '2026-07-03T12:01:00'
            }
        ],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false
    }
};

describe('Admin support chat', () => {
    beforeEach(() => {
        MockWebSocket.instances = [];
        (global as any).WebSocket = MockWebSocket;
        (HTMLElement.prototype as any).scrollIntoView = jest.fn();
        jest.clearAllMocks();
        mockGetAccessToken.mockReturnValue('admin-token');
        mockGetAdminConversations.mockResolvedValue(conversationsResponse);
        mockGetAdminConversationMessages.mockResolvedValue(messagesResponse);
    });

    it('adds the support menu entry in the admin layout', () => {
        render(<AdminLayout/>);

        const link = screen.getByRole('link', { name: /Chat ho tro/i });
        expect(link.getAttribute('href')).toBe('/admin/support');
    });

    it('loads conversations, authenticates websocket, and sends staff replies with conversationId', async () => {
        render(<AdminSupport/>);

        await waitFor(() => {
            expect(SupportChatService.getAdminConversations).toHaveBeenCalledWith({ page: 0, size: 20 });
        });
        expect((await screen.findAllByText('SOBU Customer')).length).toBeGreaterThan(0);
        expect(await screen.findByText('Need support')).toBeTruthy();

        await waitFor(() => {
            expect(MockWebSocket.instances).toHaveLength(1);
        });
        const socket = MockWebSocket.instances[0];

        act(() => {
            socket.triggerOpen();
            socket.triggerMessage({ type: 'AUTH_SUCCESS' });
        });

        expect(socket.url).toBe('ws://localhost:8081/ws/support');
        expect(socket.send).toHaveBeenCalledWith(JSON.stringify({
            type: 'AUTH',
            accessToken: 'admin-token'
        }));

        const input = screen.getByLabelText('Noi dung phan hoi ho tro') as HTMLInputElement;
        fireEvent.change(input, { target: { value: ' We can help ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Gui phan hoi ho tro' }));

        expect(socket.send).toHaveBeenLastCalledWith(JSON.stringify({
            type: 'SEND_MESSAGE',
            conversationId: 10,
            content: 'We can help'
        }));
    });

    it('appends live messages for the selected conversation', async () => {
        render(<AdminSupport/>);

        await screen.findByText('Need support');
        const socket = MockWebSocket.instances[0];

        act(() => {
            socket.triggerMessage({
                type: 'MESSAGE_CREATED',
                message: {
                    id: 3,
                    conversationId: 10,
                    senderId: 99,
                    senderEmail: 'staff@example.com',
                    senderRole: 'STAFF',
                    content: 'Live staff reply',
                    createdAt: '2026-07-03T12:02:00'
                }
            });
        });

        expect(screen.getByText('Live staff reply')).toBeTruthy();
        await waitFor(() => {
            expect(SupportChatService.getAdminConversations).toHaveBeenCalledTimes(2);
        });
    });
});
