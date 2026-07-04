import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

const mockGetConversation: any = jest.fn();
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

let mockAuthState: any = {
    isAuthenticated: true,
    user: mockUser
};

jest.mock('../../store/useAuthStore', () => ({
    useAuthStore: () => mockAuthState,
}));

jest.mock('../../service/support-chat.service', () => ({
    SupportChatService: {
        getConversation: mockGetConversation,
        getMessages: mockGetMessages
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

const SupportChatDock = require('./SupportChatDock').default;
const { getSupportWebSocketUrl } = require('./SupportChatDock');
const { SupportChatService } = require('../../service/support-chat.service');
const { ToastService } = require('../../service/toast.service');

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

const conversationResponse = {
    success: true,
    message: 'Conversation retrieved',
    data: {
        id: 1,
        status: 'OPEN',
        lastMessageAt: '2026-07-03T12:00:00',
        createdAt: '2026-07-03T12:00:00',
        customerEmail: 'user@example.com',
        customerName: 'SOBU User'
    }
};

const emptyHistoryResponse = {
    success: true,
    message: 'Messages retrieved',
    data: {
        content: [],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false
    }
};

const historyResponse = {
    success: true,
    message: 'Messages retrieved',
    data: {
        content: [
            {
                id: 2,
                conversationId: 1,
                senderId: 7,
                senderEmail: 'user@example.com',
                senderRole: 'USER',
                content: 'Newest history',
                createdAt: '2026-07-03T12:02:00'
            },
            {
                id: 1,
                conversationId: 1,
                senderId: 99,
                senderEmail: 'staff@example.com',
                senderRole: 'STAFF',
                content: 'Oldest history',
                createdAt: '2026-07-03T12:01:00'
            }
        ],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false
    }
};

const pendingHistoryResponse = new Promise(() => undefined);

const deferred = <T,>() => {
    let resolve!: (value: T) => void;
    let reject!: (reason?: unknown) => void;
    const promise = new Promise<T>((promiseResolve, promiseReject) => {
        resolve = promiseResolve;
        reject = promiseReject;
    });
    return { promise, resolve, reject };
};

const openChatAndWaitForSocket = async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Mo chat ho tro' }));

    await waitFor(() => {
        expect(SupportChatService.getConversation).toHaveBeenCalled();
    });
    await waitFor(() => {
        expect(MockWebSocket.instances).toHaveLength(1);
    });

    return MockWebSocket.instances[0];
};

describe('SupportChatDock', () => {
    beforeEach(() => {
        mockAuthState = {
            isAuthenticated: true,
            user: mockUser
        };
        MockWebSocket.instances = [];
        (global as any).WebSocket = MockWebSocket;
        (HTMLElement.prototype as any).scrollIntoView = jest.fn();
        jest.clearAllMocks();
        mockGetAccessToken.mockReturnValue('jwt-token');
        mockGetConversation.mockResolvedValue(conversationResponse);
        mockGetMessages.mockReturnValue(pendingHistoryResponse);
    });

    it('does not render for unauthenticated users', () => {
        mockAuthState = {
            isAuthenticated: false,
            user: null
        };

        render(<SupportChatDock/>);

        expect(screen.queryByRole('button', { name: 'Mo chat ho tro' })).toBeNull();
        expect(SupportChatService.getConversation).not.toHaveBeenCalled();
        expect(SupportChatService.getMessages).not.toHaveBeenCalled();
        expect(MockWebSocket.instances).toHaveLength(0);
    });

    it('does not render for non-user roles', () => {
        mockAuthState = {
            isAuthenticated: true,
            user: {
                ...mockUser,
                role: {
                    id: 2,
                    name: 'ADMIN'
                }
            }
        };

        render(<SupportChatDock/>);

        expect(screen.queryByRole('button', { name: 'Mo chat ho tro' })).toBeNull();
        expect(SupportChatService.getConversation).not.toHaveBeenCalled();
        expect(SupportChatService.getMessages).not.toHaveBeenCalled();
        expect(MockWebSocket.instances).toHaveLength(0);
    });

    it('does not initialize conversation, history, or websocket until opened', () => {
        render(<SupportChatDock/>);

        expect(screen.getByRole('button', { name: 'Mo chat ho tro' })).toBeTruthy();
        expect(SupportChatService.getConversation).not.toHaveBeenCalled();
        expect(SupportChatService.getMessages).not.toHaveBeenCalled();
        expect(MockWebSocket.instances).toHaveLength(0);
    });

    it('creates the conversation before loading history and opening the websocket', async () => {
        const conversation = deferred<typeof conversationResponse>();
        mockGetConversation.mockReturnValue(conversation.promise);

        render(<SupportChatDock/>);
        fireEvent.click(screen.getByRole('button', { name: 'Mo chat ho tro' }));

        expect(SupportChatService.getConversation).toHaveBeenCalled();
        expect(SupportChatService.getMessages).not.toHaveBeenCalled();
        expect(MockWebSocket.instances).toHaveLength(0);

        await act(async () => {
            conversation.resolve(conversationResponse);
        });

        await waitFor(() => {
            expect(SupportChatService.getMessages).toHaveBeenCalledWith({ page: 0, size: 20 });
        });
        expect(MockWebSocket.instances).toHaveLength(1);
        expect(MockWebSocket.instances[0].url).toBe('ws://localhost:8081/ws/support');
        expect(mockGetConversation.mock.invocationCallOrder[0]).toBeLessThan(mockGetMessages.mock.invocationCallOrder[0]);
    });

    it('sends the auth frame on websocket open without putting the token in the url', async () => {
        render(<SupportChatDock/>);
        const socket = await openChatAndWaitForSocket();

        act(() => {
            socket.triggerOpen();
        });

        expect(socket.url).toBe('ws://localhost:8081/ws/support');
        expect(socket.url).not.toContain('jwt-token');
        expect(socket.send).toHaveBeenCalledWith(JSON.stringify({
            type: 'AUTH',
            accessToken: 'jwt-token'
        }));
    });

    it('derives websocket urls from API base urls', () => {
        expect(getSupportWebSocketUrl('http://localhost:8081')).toBe('ws://localhost:8081/ws/support');
        expect(getSupportWebSocketUrl('https://suffocate-ground-keenness.ngrok-free.dev')).toBe('wss://suffocate-ground-keenness.ngrok-free.dev/ws/support');
    });

    it('keeps the input disabled until AUTH_SUCCESS is received', async () => {
        mockGetMessages.mockResolvedValue(historyResponse);

        render(<SupportChatDock/>);
        fireEvent.click(screen.getByRole('button', { name: 'Mo chat ho tro' }));

        const input = await screen.findByLabelText('Noi dung tin nhan ho tro') as HTMLInputElement;
        expect(input.disabled).toBe(true);

        await waitFor(() => {
            expect(MockWebSocket.instances).toHaveLength(1);
        });
        const socket = MockWebSocket.instances[0];
        act(() => {
            socket.triggerOpen();
            socket.triggerMessage({ type: 'AUTH_SUCCESS' });
        });

        await waitFor(() => {
            expect(input.disabled).toBe(false);
        });
    });

    it('sends messages after websocket auth even when history is still loading', async () => {
        render(<SupportChatDock/>);
        fireEvent.click(screen.getByRole('button', { name: 'Mo chat ho tro' }));

        const input = await screen.findByLabelText('Noi dung tin nhan ho tro') as HTMLInputElement;
        const sendButton = screen.getByRole('button', { name: 'Gui tin nhan ho tro' }) as HTMLButtonElement;
        await waitFor(() => {
            expect(MockWebSocket.instances).toHaveLength(1);
        });
        const socket = MockWebSocket.instances[0];

        act(() => {
            socket.triggerOpen();
            socket.triggerMessage({ type: 'AUTH_SUCCESS' });
        });
        fireEvent.change(input, { target: { value: ' Can you help? ' } });
        await waitFor(() => {
            expect(sendButton.disabled).toBe(false);
        });
        fireEvent.click(sendButton);

        expect(socket.send).toHaveBeenLastCalledWith(JSON.stringify({
            type: 'SEND_MESSAGE',
            content: 'Can you help?'
        }));
        expect(input.value).toBe('');
    });

    it('allows sending when history returns an empty 200 page', async () => {
        mockGetMessages.mockResolvedValue(emptyHistoryResponse);

        render(<SupportChatDock/>);
        const socket = await openChatAndWaitForSocket();
        const input = screen.getByLabelText('Noi dung tin nhan ho tro') as HTMLInputElement;
        const sendButton = screen.getByRole('button', { name: 'Gui tin nhan ho tro' }) as HTMLButtonElement;

        act(() => {
            socket.triggerOpen();
            socket.triggerMessage({ type: 'AUTH_SUCCESS' });
        });
        fireEvent.change(input, { target: { value: 'Hello support' } });
        await waitFor(() => {
            expect(sendButton.disabled).toBe(false);
        });
        fireEvent.click(sendButton);

        expect(socket.send).toHaveBeenLastCalledWith(JSON.stringify({
            type: 'SEND_MESSAGE',
            content: 'Hello support'
        }));
    });

    it('keeps chat usable when history loading fails after conversation initialization', async () => {
        mockGetMessages.mockRejectedValue({
            response: {
                data: {
                    message: 'History failed'
                }
            }
        });

        render(<SupportChatDock/>);
        const socket = await openChatAndWaitForSocket();
        const input = screen.getByLabelText('Noi dung tin nhan ho tro') as HTMLInputElement;
        const sendButton = screen.getByRole('button', { name: 'Gui tin nhan ho tro' }) as HTMLButtonElement;

        act(() => {
            socket.triggerOpen();
            socket.triggerMessage({ type: 'AUTH_SUCCESS' });
        });
        fireEvent.change(input, { target: { value: 'Still need help' } });
        await waitFor(() => {
            expect(sendButton.disabled).toBe(false);
        });
        fireEvent.click(sendButton);

        expect(socket.send).toHaveBeenLastCalledWith(JSON.stringify({
            type: 'SEND_MESSAGE',
            content: 'Still need help'
        }));
        await waitFor(() => {
            expect(ToastService.error).toHaveBeenCalledWith('History failed');
        });
    });

    it('renders history oldest-first and appends MESSAGE_CREATED events', async () => {
        mockGetMessages.mockResolvedValue(historyResponse);

        render(<SupportChatDock/>);
        fireEvent.click(screen.getByRole('button', { name: 'Mo chat ho tro' }));

        await waitFor(() => {
            expect(screen.getByText('Oldest history')).toBeTruthy();
        });
        const oldest = screen.getByText('Oldest history');
        const newest = screen.getByText('Newest history');
        expect(oldest.compareDocumentPosition(newest) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

        const socket = MockWebSocket.instances[0];
        act(() => {
            socket.triggerMessage({
                type: 'MESSAGE_CREATED',
                message: {
                    id: 3,
                    conversationId: 1,
                    senderId: 99,
                    senderEmail: 'staff@example.com',
                    senderRole: 'STAFF',
                    content: 'Live reply',
                    createdAt: '2026-07-03T12:03:00'
                }
            });
        });

        expect(screen.getByText('Live reply')).toBeTruthy();
    });

    it('supports data.message fallback for MESSAGE_CREATED events', async () => {
        render(<SupportChatDock/>);
        const socket = await openChatAndWaitForSocket();

        act(() => {
            socket.triggerMessage({
                type: 'MESSAGE_CREATED',
                data: {
                    message: {
                        id: 4,
                        conversationId: 1,
                        senderId: 7,
                        senderEmail: 'user@example.com',
                        senderRole: 'USER',
                        content: 'Fallback message',
                        createdAt: '2026-07-03T12:04:00'
                    }
                }
            });
        });

        expect(screen.getByText('Fallback message')).toBeTruthy();
    });

    it('shows a toast for websocket ERROR events', async () => {
        render(<SupportChatDock/>);
        const socket = await openChatAndWaitForSocket();

        act(() => {
            socket.triggerMessage({ type: 'ERROR', error: 'Invalid message format' });
        });

        expect(ToastService.error).toHaveBeenCalledWith('Invalid message format');
    });

    it('closes the websocket when chat is closed', async () => {
        render(<SupportChatDock/>);
        const socket = await openChatAndWaitForSocket();

        fireEvent.click(screen.getByRole('button', { name: 'Dong chat ho tro' }));

        expect(socket.close).toHaveBeenCalled();
    });

    it('auto-scrolls to the newest message', async () => {
        mockGetMessages.mockResolvedValue(historyResponse);

        render(<SupportChatDock/>);
        fireEvent.click(screen.getByRole('button', { name: 'Mo chat ho tro' }));

        await waitFor(() => {
            expect(HTMLElement.prototype.scrollIntoView).toHaveBeenCalled();
        });
    });
});
