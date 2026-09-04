import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Loader2, LogIn, MessageCircle, Send, X } from 'lucide-react';
import { useAuthStore } from '../../store/useAuthStore';
import { authStorage } from '../../utils/auth-storage';
import { SupportChatService } from '../../service/support-chat.service';
import { ToastService } from '../../service/toast.service';
import { SupportMessage, SupportWebSocketEvent } from '../../interface/support-chat.model';
import { getSupportWebSocketUrl } from '../../utils/support-chat-websocket';

const HISTORY_PAGE = 0;
const HISTORY_SIZE = 20;
const MAX_RECONNECT_ATTEMPTS = 3;
const RECONNECT_DELAY_MS = 1500;

type SocketStatus = 'connecting' | 'authenticating' | 'ready' | 'reconnecting' | 'closed' | 'error';

interface SupportChatDockProps {
    isOpen?: boolean;
    onOpenChange?: (isOpen: boolean) => void;
}

const getRoleName = (role?: unknown) => {
    if (typeof role === 'string') return role;
    if (role && typeof role === 'object' && 'name' in role) {
        const name = (role as { name?: unknown }).name;
        return typeof name === 'string' ? name : undefined;
    }
    return undefined;
};

const cleanRoleName = (roleName?: string) => (roleName || '').replace('ROLE_', '').toUpperCase();
const isUserRole = (roleName?: string) => cleanRoleName(roleName) === 'USER';
const isStaffRole = (roleName?: string) => ['ADMIN', 'STAFF'].includes(cleanRoleName(roleName));

const isAuthSocketError = (error?: string) => {
    const normalized = (error || '').toLowerCase();
    return normalized.includes('token') || normalized.includes('account') || normalized.includes('auth');
};

const getMessageFromEvent = (event: SupportWebSocketEvent) => event.message || event.data?.message;

const formatMessageTime = (createdAt: string) => {
    const date = new Date(createdAt);
    if (Number.isNaN(date.getTime())) return '';

    return date.toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
};

const sortOldestFirst = (messages: SupportMessage[]) => [...messages].reverse();

const getMessageKey = (message: SupportMessage) => (
    message.id != null
        ? `id:${message.id}`
        : `${message.conversationId}-${message.senderId}-${message.createdAt}-${message.content}`
);

const mergeMessagesOldestFirst = (...messageGroups: SupportMessage[][]) => {
    const messagesByKey = new Map<string, SupportMessage>();
    messageGroups.flat().forEach((message) => messagesByKey.set(getMessageKey(message), message));

    return [...messagesByKey.values()].sort((left, right) => {
        const leftTime = new Date(left.createdAt).getTime();
        const rightTime = new Date(right.createdAt).getTime();
        return leftTime - rightTime;
    });
};

export { getSupportWebSocketUrl };

export default function SupportChatDock({
    isOpen: controlledIsOpen,
    onOpenChange
}: SupportChatDockProps = {}) {
    const { isAuthenticated, user } = useAuthStore();
    const roleName = getRoleName(user?.role);
    const isGuest = !isAuthenticated;
    const isStaffAccount = isAuthenticated && isStaffRole(roleName);
    const canUseCustomerChat = isAuthenticated && isUserRole(roleName);

    const [internalIsOpen, setInternalIsOpen] = useState(false);
    const [messages, setMessages] = useState<SupportMessage[]>([]);
    const [draft, setDraft] = useState('');
    const [isInitializing, setIsInitializing] = useState(false);
    const [isHistoryLoading, setIsHistoryLoading] = useState(false);
    const [socketStatus, setSocketStatus] = useState<SocketStatus>('closed');
    const [isAuthenticatedSocket, setIsAuthenticatedSocket] = useState(false);

    const socketRef = useRef<WebSocket | null>(null);
    const bottomRef = useRef<HTMLDivElement | null>(null);
    const reconnectAttemptsRef = useRef(0);
    const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const shouldReconnectRef = useRef(true);
    const isOpen = controlledIsOpen ?? internalIsOpen;
    const setIsOpen = (nextIsOpen: boolean) => {
        if (controlledIsOpen === undefined) {
            setInternalIsOpen(nextIsOpen);
        }
        onOpenChange?.(nextIsOpen);
    };

    const canSend = useMemo(() => (
        socketRef.current?.readyState === WebSocket.OPEN &&
        isAuthenticatedSocket &&
        !isInitializing &&
        draft.trim().length > 0
    ), [draft, isAuthenticatedSocket, isInitializing]);

    useEffect(() => {
        if (!isOpen || !canUseCustomerChat) {
            setIsInitializing(false);
            setIsHistoryLoading(false);
            setSocketStatus('closed');
            setIsAuthenticatedSocket(false);
            return;
        }

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
                    ToastService.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
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
                    if (createdMessage) {
                        setMessages((current) => mergeMessagesOldestFirst(current, [createdMessage]));
                    }
                    return;
                }

                if (event.type === 'ERROR') {
                    if (isAuthSocketError(event.error)) {
                        shouldReconnectRef.current = false;
                        setSocketStatus('error');
                    }
                    ToastService.error(event.error || 'Không thể xử lí tin nhắn hỗ trợ.');
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

        setIsInitializing(true);
        setIsHistoryLoading(false);
        setSocketStatus('connecting');
        setIsAuthenticatedSocket(false);
        reconnectAttemptsRef.current = 0;
        shouldReconnectRef.current = true;

        const initialAccessToken = authStorage.getAccessToken();
        if (!initialAccessToken) {
            setIsInitializing(false);
            setSocketStatus('error');
            ToastService.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            return () => {
                isDisposed = true;
                clearReconnectTimer();
                cleanupSocket(socketRef.current);
                socketRef.current = null;
            };
        }

        SupportChatService.getConversation()
            .then(() => {
                if (isDisposed) return;

                setIsInitializing(false);
                setIsHistoryLoading(true);
                SupportChatService.getMessages({ page: HISTORY_PAGE, size: HISTORY_SIZE })
                    .then((response) => {
                        if (isDisposed) return;
                        setMessages((current) => mergeMessagesOldestFirst(
                            sortOldestFirst(response.data.content || []),
                            current
                        ));
                    })
                    .catch((error) => {
                        if (isDisposed) return;
                        ToastService.error(error?.response?.data?.message || 'Không thể tải lịch sử hỗ trợ.');
                    })
                    .finally(() => {
                        if (!isDisposed) {
                            setIsHistoryLoading(false);
                        }
                    });

                connectSocket(false);
            })
            .catch((error) => {
                if (isDisposed) return;
                setIsInitializing(false);
                setSocketStatus('error');
                ToastService.error(error?.response?.data?.message || 'Lỗi trong việc khởi tạo.');
            });

        return () => {
            isDisposed = true;
            clearReconnectTimer();
            cleanupSocket(socketRef.current);
            socketRef.current = null;
        };
    }, [canUseCustomerChat, isOpen]);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }, [messages, isOpen]);

    const sendMessage = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        const content = draft.trim();
        const socket = socketRef.current;
        if (!content || !socket || socket.readyState !== WebSocket.OPEN || !isAuthenticatedSocket) {
            return;
        }

        socket.send(JSON.stringify({ type: 'SEND_MESSAGE', content }));
        setDraft('');
    };

    const statusLabel = (() => {
        if (isGuest) return 'Đăng nhập để chat';
        if (isStaffAccount) return 'Dùng trang admin support';
        if (!canUseCustomerChat) return 'Đang kiểm tra tài khoản';
        if (isInitializing) return 'Đang khởi tạo...';
        if (isHistoryLoading) return 'Đang tải lịch sử...';
        if (socketStatus === 'ready') return 'Trực tuyến';
        if (socketStatus === 'authenticating') return 'Đang xác thực...';
        if (socketStatus === 'reconnecting') return 'Đang kết nối lại...';
        if (socketStatus === 'connecting') return 'Đang kết nối...';
        if (socketStatus === 'error') return 'Kết nối bị lỗi';
        return 'Mất kết nối';
    })();

    return (
        <div aria-label="Thanh chat ho tro khach hang" className="flex flex-col items-end gap-3">
            {isOpen && (
                <section
                    aria-label="Support chat"
                    className="w-[min(calc(100vw-2rem),24rem)] overflow-hidden rounded-lg border border-outline-variant/40 bg-surface-container-lowest shadow-[0_18px_55px_rgba(14,48,78,0.20)]"
                >
                    <div className="flex items-center justify-between gap-3 border-b border-outline-variant/25 bg-surface-container-low px-4 py-3">
                        <div className="min-w-0">
                            <p className="text-sm font-black text-on-surface">Chat với SOBU</p>
                            <p className="truncate text-[11px] font-semibold text-outline">{statusLabel}</p>
                        </div>
                        <button
                            type="button"
                            aria-label="Dong chat ho tro"
                            onClick={() => setIsOpen(false)}
                            className="flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container-high hover:text-on-surface focus-visible:ring-2 focus-visible:ring-primary/40"
                        >
                            <X className="h-4 w-4"/>
                        </button>
                    </div>

                    {isGuest ? (
                        <div className="flex h-72 flex-col items-center justify-center bg-surface-container-lowest px-6 text-center">
                            <LogIn className="mb-3 h-8 w-8 text-primary"/>
                            <p className="text-sm font-black text-on-surface">Đăng nhập để chat với SOBU</p>
                            <p className="mt-2 max-w-[18rem] text-xs font-semibold leading-relaxed text-outline">
                                Chat Hỗ trợ cần tài khoản để bảo mật lịch sử trao đổi của bạn.
                            </p>
                            <a
                                href="/login"
                                className="mt-5 inline-flex cursor-pointer items-center justify-center rounded-full bg-primary px-5 py-2.5 text-xs font-black uppercase text-on-primary transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
                            >
                                Đăng nhập
                            </a>
                        </div>
                    ) : isStaffAccount ? (
                        <div className="flex h-72 flex-col items-center justify-center bg-surface-container-lowest px-6 text-center">
                            <MessageCircle className="mb-3 h-8 w-8 text-primary"/>
                            <p className="text-sm font-black text-on-surface">Chat Hỗ trợ</p>
                            <p className="mt-2 max-w-[18rem] text-xs font-semibold leading-relaxed text-outline">
                                Quản trị viên truy cập mục Chat Hỗ trợ để phản hồi tin nhắn
                            </p>
                            <a
                                href="/admin/support"
                                className="mt-5 inline-flex cursor-pointer items-center justify-center rounded-full bg-primary px-5 py-2.5 text-xs font-black uppercase text-on-primary transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
                            >
                                Mo admin support
                            </a>
                        </div>
                    ) : !canUseCustomerChat ? (
                        <div className="flex h-72 flex-col items-center justify-center bg-surface-container-lowest px-6 text-center">
                            <MessageCircle className="mb-3 h-8 w-8 text-primary"/>
                            <p className="text-sm font-black text-on-surface">Đang sẵn sàng</p>
                            <p className="mt-2 max-w-[18rem] text-xs font-semibold leading-relaxed text-outline">
                                Hãy tải lại trang nếu thông tin tài khoản chưa được đồng bộ
                            </p>
                        </div>
                    ) : (
                        <div className="flex h-[24rem] flex-col bg-surface-container-lowest">
                            <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-3">
                                {isHistoryLoading && messages.length === 0 ? (
                                    <div className="flex h-full items-center justify-center text-xs font-bold text-outline">
                                        <Loader2 className="mr-2 h-4 w-4 animate-spin text-primary"/>
                                        Đang tải lịch sử...
                                    </div>
                                ) : messages.length === 0 ? (
                                    <div className="flex h-full flex-col items-center justify-center text-center">
                                        <MessageCircle className="mb-3 h-8 w-8 text-primary"/>
                                        <p className="text-sm font-black text-on-surface">Cần hỗ trợ?</p>
                                        <p className="mt-1 max-w-[16rem] text-xs font-semibold leading-relaxed text-outline">
                                            Gửi tin nhắn, đội ngũ SOBU sẽ phản hồi ngay khi có thể.
                                        </p>
                                    </div>
                                ) : (
                                    messages.map((message) => {
                                        const isMine = isUserRole(message.senderRole);
                                        return (
                                            <article
                                                key={`${message.id}-${message.createdAt}`}
                                                className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
                                            >
                                                <div
                                                    className={`max-w-[78%] rounded-lg px-3 py-2 text-sm shadow-sm ${
                                                        isMine
                                                            ? 'bg-primary text-on-primary'
                                                            : 'border border-outline-variant/35 bg-surface-container-low text-on-surface'
                                                    }`}
                                                >
                                                    <p className="whitespace-pre-wrap break-words leading-relaxed">{message.content}</p>
                                                    <p
                                                        className={`mt-1 text-[10px] font-semibold ${
                                                            isMine ? 'text-on-primary/75' : 'text-outline'
                                                        }`}
                                                    >
                                                        {formatMessageTime(message.createdAt)}
                                                    </p>
                                                </div>
                                            </article>
                                        );
                                    })
                                )}
                                <div ref={bottomRef}/>
                            </div>

                            <form
                                onSubmit={sendMessage}
                                className="flex items-center gap-2 border-t border-outline-variant/25 bg-surface-container-low px-3 py-3"
                            >
                                <input
                                    aria-label="Noi dung tin nhan ho tro"
                                    value={draft}
                                    onChange={(event) => setDraft(event.target.value)}
                                    disabled={!isAuthenticatedSocket || isInitializing}
                                    placeholder={isAuthenticatedSocket ? 'Nhập tin nhắn...' : 'Đang kết nối...'}
                                    className="min-w-0 flex-1 rounded-full border border-outline-variant/50 bg-surface-container-lowest px-4 py-2 text-sm font-semibold text-on-surface outline-none transition-colors placeholder:text-outline disabled:cursor-not-allowed disabled:bg-surface-container-high focus:border-primary focus:ring-2 focus:ring-primary/20"
                                />
                                <button
                                    type="submit"
                                    aria-label="Gui tin nhan ho tro"
                                    disabled={!canSend}
                                    className="flex h-10 w-10 shrink-0 cursor-pointer items-center justify-center rounded-full bg-primary text-on-primary transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:bg-outline-variant disabled:text-outline focus-visible:ring-2 focus-visible:ring-primary/40"
                                >
                                    <Send className="h-4 w-4"/>
                                </button>
                            </form>
                        </div>
                    )}
                </section>
            )}

            <button
                type="button"
                aria-label={isOpen ? 'An chat ho tro' : 'Mo chat ho tro'}
                aria-expanded={isOpen}
                onClick={() => setIsOpen(!isOpen)}
                className="inline-flex h-11 cursor-pointer items-center gap-2 rounded-full bg-primary px-3 text-xs font-black text-on-primary shadow-[0_14px_35px_rgba(0,97,142,0.28)] transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40 lg:h-14 lg:px-4 lg:text-sm"
            >
                <MessageCircle className="h-5 w-5"/>
                <span>Hỗ trợ</span>
            </button>
        </div>
    );
}
