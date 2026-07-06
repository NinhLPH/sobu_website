import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Loader2, MessageCircle, RefreshCw, Send } from 'lucide-react';
import { SupportConversation, SupportMessage, SupportWebSocketEvent } from '../../interface/support-chat.model';
import { SupportChatService } from '../../service/support-chat.service';
import { ToastService } from '../../service/toast.service';
import { authStorage } from '../../utils/auth-storage';
import { getSupportWebSocketUrl } from '../../utils/support-chat-websocket';

const PAGE = 0;
const PAGE_SIZE = 20;
const MAX_RECONNECT_ATTEMPTS = 3;
const RECONNECT_DELAY_MS = 1500;

type SocketStatus = 'connecting' | 'authenticating' | 'ready' | 'reconnecting' | 'error';

const getMessageFromEvent = (event: SupportWebSocketEvent) => event.message || event.data?.message;

const isStaffMessage = (message: SupportMessage) => {
    const role = (message.senderRole || '').replace('ROLE_', '').toUpperCase();
    return role === 'ADMIN' || role === 'STAFF';
};

const formatDateTime = (value?: string) => {
    if (!value) return 'N/A';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return 'N/A';
    return parsed.toLocaleString('vi-VN');
};

const sortOldestFirst = (messages: SupportMessage[]) => [...messages].reverse();

export default function AdminSupport() {
    const [conversations, setConversations] = useState<SupportConversation[]>([]);
    const [selectedConversationId, setSelectedConversationId] = useState<number | null>(null);
    const [messages, setMessages] = useState<SupportMessage[]>([]);
    const [draft, setDraft] = useState('');
    const [isConversationsLoading, setIsConversationsLoading] = useState(false);
    const [isMessagesLoading, setIsMessagesLoading] = useState(false);
    const [socketStatus, setSocketStatus] = useState<SocketStatus>('connecting');
    const [isAuthenticatedSocket, setIsAuthenticatedSocket] = useState(false);

    const socketRef = useRef<WebSocket | null>(null);
    const reconnectAttemptsRef = useRef(0);
    const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const shouldReconnectRef = useRef(true);
    const selectedConversationIdRef = useRef<number | null>(null);
    const bottomRef = useRef<HTMLDivElement | null>(null);

    const selectedConversation = useMemo(
        () => conversations.find((conversation) => conversation.id === selectedConversationId) || null,
        [conversations, selectedConversationId]
    );

    const canSend = Boolean(
        selectedConversationId &&
        socketRef.current?.readyState === WebSocket.OPEN &&
        isAuthenticatedSocket &&
        draft.trim()
    );

    const loadConversations = (showLoader = true) => {
        if (showLoader) {
            setIsConversationsLoading(true);
        }

        return SupportChatService.getAdminConversations({ page: PAGE, size: PAGE_SIZE })
            .then((response) => {
                const nextConversations = response.data.content || [];
                setConversations(nextConversations);
                setSelectedConversationId((current) => {
                    if (current && nextConversations.some((conversation) => conversation.id === current)) {
                        return current;
                    }
                    return nextConversations[0]?.id || null;
                });
            })
            .catch((error) => {
                ToastService.error(error?.response?.data?.message || 'Không thể tải danh sách chat.');
            })
            .finally(() => {
                if (showLoader) {
                    setIsConversationsLoading(false);
                }
            });
    };

    useEffect(() => {
        selectedConversationIdRef.current = selectedConversationId;
    }, [selectedConversationId]);

    useEffect(() => {
        void loadConversations();
    }, []);

    useEffect(() => {
        if (!selectedConversationId) {
            setMessages([]);
            return;
        }

        let isDisposed = false;
        setIsMessagesLoading(true);
        SupportChatService.getAdminConversationMessages(selectedConversationId, { page: PAGE, size: PAGE_SIZE })
            .then((response) => {
                if (isDisposed) return;
                setMessages(sortOldestFirst(response.data.content || []));
            })
            .catch((error) => {
                if (isDisposed) return;
                ToastService.error(error?.response?.data?.message || 'Không thể tải nội dung hội thoại.');
            })
            .finally(() => {
                if (!isDisposed) {
                    setIsMessagesLoading(false);
                }
            });

        return () => {
            isDisposed = true;
        };
    }, [selectedConversationId]);

    useEffect(() => {
        let isDisposed = false;

        const clearReconnectTimer = () => {
            if (reconnectTimerRef.current) {
                clearTimeout(reconnectTimerRef.current);
                reconnectTimerRef.current = null;
            }
        };

        const cleanupSocket = (socket: WebSocket | null) => {
            if (!socket) return;
            socket.onopen = null;
            socket.onmessage = null;
            socket.onerror = null;
            socket.onclose = null;
            socket.close();
        };

        const connectSocket = (isRetry = false) => {
            if (isDisposed) return;

            const socket = new WebSocket(getSupportWebSocketUrl());
            socketRef.current = socket;
            setSocketStatus(isRetry ? 'reconnecting' : 'connecting');
            setIsAuthenticatedSocket(false);

            socket.onopen = () => {
                if (isDisposed) return;
                const latestAccessToken = authStorage.getAccessToken();
                if (!latestAccessToken) {
                    shouldReconnectRef.current = false;
                    setSocketStatus('error');
                    ToastService.error('Phiên đăng nhập đã hết hạn.  Vui lòng đăng nhập lại.');
                    socket.close();
                    return;
                }

                setSocketStatus('authenticating');
                socket.send(JSON.stringify({ type: 'AUTH', accessToken: latestAccessToken }));
            };

            socket.onmessage = (messageEvent) => {
                if (isDisposed) return;

                let event: SupportWebSocketEvent;
                try {
                    event = JSON.parse(messageEvent.data);
                } catch {
                    ToastService.error('Dữ liệu chat không hợp lệ.');
                    return;
                }

                if (event.type === 'AUTH_SUCCESS') {
                    reconnectAttemptsRef.current = 0;
                    setIsAuthenticatedSocket(true);
                    setSocketStatus('ready');
                    return;
                }

                if (event.type === 'MESSAGE_CREATED') {
                    const createdMessage = getMessageFromEvent(event);
                    if (!createdMessage) return;

                    if (createdMessage.conversationId === selectedConversationIdRef.current) {
                        setMessages((current) => [...current, createdMessage]);
                    }

                    setConversations((current) => {
                        const updated = current.map((conversation) => (
                            conversation.id === createdMessage.conversationId
                                ? { ...conversation, lastMessageAt: createdMessage.createdAt }
                                : conversation
                        ));
                        return [...updated].sort((a, b) => (
                            new Date(b.lastMessageAt || b.createdAt).getTime() -
                            new Date(a.lastMessageAt || a.createdAt).getTime()
                        ));
                    });
                    void loadConversations(false);
                    return;
                }

                if (event.type === 'ERROR') {
                    const normalized = (event.error || '').toLowerCase();
                    if (normalized.includes('token') || normalized.includes('account') || normalized.includes('auth')) {
                        shouldReconnectRef.current = false;
                        setSocketStatus('error');
                    }
                    ToastService.error(event.error || 'Không thể xử lý tin nhắn hỗ trợ.');
                }
            };

            socket.onerror = () => {
                if (isDisposed) return;
                setSocketStatus('error');
            };

            socket.onclose = () => {
                if (isDisposed) return;
                if (socketRef.current === socket) {
                    socketRef.current = null;
                }
                setIsAuthenticatedSocket(false);

                if (!shouldReconnectRef.current || reconnectAttemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
                    setSocketStatus('error');
                    return;
                }

                reconnectAttemptsRef.current += 1;
                setSocketStatus('reconnecting');
                clearReconnectTimer();
                reconnectTimerRef.current = setTimeout(() => connectSocket(true), RECONNECT_DELAY_MS);
            };
        };

        reconnectAttemptsRef.current = 0;
        shouldReconnectRef.current = true;
        connectSocket(false);

        return () => {
            isDisposed = true;
            clearReconnectTimer();
            cleanupSocket(socketRef.current);
            socketRef.current = null;
        };
    }, []);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }, [messages, selectedConversationId]);

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const content = draft.trim();
        const socket = socketRef.current;
        if (!selectedConversationId || !content || !socket || socket.readyState !== WebSocket.OPEN || !isAuthenticatedSocket) {
            return;
        }

        socket.send(JSON.stringify({
            type: 'SEND_MESSAGE',
            conversationId: selectedConversationId,
            content
        }));
        setDraft('');
    };

    const statusLabel = (() => {
        if (socketStatus === 'ready') return 'Trực tuyến';
        if (socketStatus === 'authenticating') return 'Đang xác thực';
        if (socketStatus === 'reconnecting') return 'Đang kết nối lại';
        if (socketStatus === 'connecting') return 'Đang kết nối';
        return 'Kết nối bị lỗi';
    })();

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                        Customer support
                    </p>
                    <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface">
                        Chat Hỗ trợ
                    </h1>
                </div>
                <button
                    type="button"
                    onClick={() => void loadConversations()}
                    disabled={isConversationsLoading}
                    className="inline-flex cursor-pointer items-center justify-center gap-2 rounded-xl bg-surface-container px-4 py-2.5 text-xs font-black text-on-surface transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-50"
                >
                    <RefreshCw className={`h-4 w-4 ${isConversationsLoading ? 'animate-spin' : ''}`} />
                    Làm mới
                </button>
            </div>

            <div className="grid min-h-[36rem] overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm lg:grid-cols-[20rem_minmax(0,1fr)]">
                <aside className="border-b border-outline-variant/30 bg-surface-container-lowest lg:border-b-0 lg:border-r">
                    <div className="flex items-center justify-between border-b border-outline-variant/20 px-4 py-3">
                        <p className="text-xs font-black uppercase tracking-widest text-outline">
                            Hội thoại
                        </p>
                        <span className="rounded-full bg-surface-container px-2.5 py-1 text-[10px] font-black text-outline">
                            {statusLabel}
                        </span>
                    </div>
                    <div className="max-h-[34rem] overflow-y-auto p-3">
                        {isConversationsLoading ? (
                            <div className="flex items-center justify-center py-16 text-xs font-bold text-outline">
                                <Loader2 className="mr-2 h-4 w-4 animate-spin text-primary" />
                                Đang tải...
                            </div>
                        ) : conversations.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-16 text-center text-xs font-bold text-outline">
                                <MessageCircle className="mb-3 h-8 w-8 text-outline/40" />
                                Chưa có hội thoại
                            </div>
                        ) : conversations.map((conversation) => {
                            const isSelected = conversation.id === selectedConversationId;
                            return (
                                <button
                                    key={conversation.id}
                                    type="button"
                                    onClick={() => setSelectedConversationId(conversation.id)}
                                    className={`mb-2 w-full cursor-pointer rounded-xl border px-3 py-3 text-left transition-colors focus-visible:ring-2 focus-visible:ring-primary/40 ${
                                        isSelected
                                            ? 'border-primary bg-primary text-on-primary'
                                            : 'border-outline-variant/20 bg-white text-on-surface hover:bg-surface-container-low'
                                    }`}
                                >
                                    <p className="truncate text-sm font-black">
                                        {conversation.customerName || conversation.customerEmail || `Khách #${conversation.id}`}
                                    </p>
                                    <p className={`mt-1 truncate text-[11px] font-semibold ${isSelected ? 'text-on-primary/75' : 'text-outline'}`}>
                                        {conversation.customerEmail || 'Chưa có email'}
                                    </p>
                                    <p className={`mt-2 text-[10px] font-bold ${isSelected ? 'text-on-primary/70' : 'text-outline'}`}>
                                        Cập nhật: {formatDateTime(conversation.lastMessageAt || conversation.createdAt)}
                                    </p>
                                </button>
                            );
                        })}
                    </div>
                </aside>

                <section className="flex min-h-[36rem] min-w-0 flex-col">
                    <div className="border-b border-outline-variant/20 bg-surface-container-low px-5 py-4">
                        <p className="text-sm font-black text-on-surface">
                            {selectedConversation
                                ? selectedConversation.customerName || selectedConversation.customerEmail || `Hội thoại #${selectedConversation.id}`
                                : 'Chọn hội thoại'}
                        </p>
                        <p className="mt-1 text-[11px] font-semibold text-outline">
                            {selectedConversation?.customerEmail || 'Chọn một hội thoại để xem và phản hồi tin nhắn.'}
                        </p>
                    </div>

                    <div className="min-h-0 flex-1 space-y-3 overflow-y-auto bg-surface-container-lowest px-5 py-4">
                        {!selectedConversationId ? (
                            <div className="flex h-full flex-col items-center justify-center text-center">
                                <MessageCircle className="mb-3 h-10 w-10 text-outline/40" />
                                <p className="text-sm font-black text-on-surface">Chưa chọn hội thoại</p>
                                <p className="mt-1 text-xs font-semibold text-outline">Chọn khách hàng để bắt đầu hỗ trợ.</p>
                            </div>
                        ) : isMessagesLoading ? (
                            <div className="flex h-full items-center justify-center text-xs font-bold text-outline">
                                <Loader2 className="mr-2 h-4 w-4 animate-spin text-primary" />
                                Đang tải tin nhắn...
                            </div>
                        ) : messages.length === 0 ? (
                            <div className="flex h-full flex-col items-center justify-center text-center">
                                <MessageCircle className="mb-3 h-10 w-10 text-primary" />
                                <p className="text-sm font-black text-on-surface">Chưa có tin nhắn </p>
                                <p className="mt-1 text-xs font-semibold text-outline">Gửi phản hồi đầu tiên cho khách hàng này.</p>
                            </div>
                        ) : messages.map((message) => {
                            const isMine = isStaffMessage(message);
                            return (
                                <article
                                    key={`${message.id}-${message.createdAt}`}
                                    className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
                                >
                                    <div
                                        className={`max-w-[76%] rounded-lg px-3 py-2 text-sm shadow-sm ${
                                            isMine
                                                ? 'bg-primary text-on-primary'
                                                : 'border border-outline-variant/35 bg-white text-on-surface'
                                        }`}
                                    >
                                        <p className="whitespace-pre-wrap break-words leading-relaxed">{message.content}</p>
                                        <p className={`mt-1 text-[10px] font-semibold ${isMine ? 'text-on-primary/75' : 'text-outline'}`}>
                                            {message.senderEmail} - {formatDateTime(message.createdAt)}
                                        </p>
                                    </div>
                                </article>
                            );
                        })}
                        <div ref={bottomRef} />
                    </div>

                    <form
                        onSubmit={handleSubmit}
                        className="flex items-center gap-3 border-t border-outline-variant/20 bg-surface-container-low px-4 py-4"
                    >
                        <input
                            aria-label="Noi dung phan hoi ho tro"
                            value={draft}
                            onChange={(event) => setDraft(event.target.value)}
                            disabled={!selectedConversationId || !isAuthenticatedSocket}
                            placeholder={selectedConversationId ? 'Nhập phản hồi...' : 'Chọn hội thoại'}
                            className="min-w-0 flex-1 rounded-full border border-outline-variant/50 bg-white px-4 py-2.5 text-sm font-semibold text-on-surface outline-none transition-colors placeholder:text-outline disabled:cursor-not-allowed disabled:bg-surface-container-high focus:border-primary focus:ring-2 focus:ring-primary/20"
                        />
                        <button
                            type="submit"
                            aria-label="Gui phan hoi ho tro"
                            disabled={!canSend}
                            className="flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full bg-primary text-on-primary transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:bg-outline-variant disabled:text-outline focus-visible:ring-2 focus-visible:ring-primary/40"
                        >
                            <Send className="h-4 w-4" />
                        </button>
                    </form>
                </section>
            </div>
        </div>
    );
}
