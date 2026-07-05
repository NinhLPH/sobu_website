import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Loader2, MessageCircle, Send, X } from 'lucide-react';
import { useAuthStore } from '../../store/useAuthStore';
import { authStorage } from '../../utils/auth-storage';
import { SupportChatService } from '../../service/support-chat.service';
import { ToastService } from '../../service/toast.service';
import { SupportMessage, SupportWebSocketEvent } from '../../interface/support-chat.model';
import { BASE_URL } from '../../api/api-client';

const HISTORY_PAGE = 0;
const HISTORY_SIZE = 20;

type SocketStatus = 'connecting' | 'authenticating' | 'ready' | 'closed' | 'error';

const isUserRole = (roleName?: string) => (roleName || '').replace('ROLE_', '').toUpperCase() === 'USER';

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

export const getSupportWebSocketUrl = (apiBaseUrl = BASE_URL) => {
    try {
        const fallbackOrigin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8081';
        const url = new URL(apiBaseUrl, fallbackOrigin);
        url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
        url.pathname = '/ws/support';
        url.search = '';
        url.hash = '';
        return url.toString();
    } catch {
        return 'ws://localhost:8081/ws/support';
    }
};

export default function SupportChatDock() {
    const { isAuthenticated, user } = useAuthStore();
    const accessToken = authStorage.getAccessToken();
    const canUseChat = isAuthenticated && isUserRole(user?.role?.name) && Boolean(accessToken);

    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState<SupportMessage[]>([]);
    const [draft, setDraft] = useState('');
    const [isInitializing, setIsInitializing] = useState(false);
    const [isHistoryLoading, setIsHistoryLoading] = useState(false);
    const [socketStatus, setSocketStatus] = useState<SocketStatus>('closed');
    const [isAuthenticatedSocket, setIsAuthenticatedSocket] = useState(false);

    const socketRef = useRef<WebSocket | null>(null);
    const bottomRef = useRef<HTMLDivElement | null>(null);

    const canSend = useMemo(() => (
        socketRef.current?.readyState === WebSocket.OPEN &&
        isAuthenticatedSocket &&
        !isInitializing &&
        draft.trim().length > 0
    ), [draft, isAuthenticatedSocket, isInitializing]);

    useEffect(() => {
        if (!isOpen || !canUseChat || !accessToken) {
            setIsInitializing(false);
            setIsHistoryLoading(false);
            setSocketStatus('closed');
            setIsAuthenticatedSocket(false);
            return;
        }

        let isDisposed = false;

        setIsInitializing(true);
        setIsHistoryLoading(false);
        setSocketStatus('connecting');
        setIsAuthenticatedSocket(false);

        SupportChatService.getConversation()
            .then(() => {
                if (isDisposed) return;

                setIsInitializing(false);
                setIsHistoryLoading(true);
                SupportChatService.getMessages({ page: HISTORY_PAGE, size: HISTORY_SIZE })
                    .then((response) => {
                        if (isDisposed) return;
                        setMessages(sortOldestFirst(response.data.content || []));
                    })
                    .catch((error) => {
                        if (isDisposed) return;
                        ToastService.error(error?.response?.data?.message || 'Khong the tai lich su ho tro.');
                    })
                    .finally(() => {
                        if (!isDisposed) {
                            setIsHistoryLoading(false);
                        }
                    });

                const socket = new WebSocket(getSupportWebSocketUrl());
                socketRef.current = socket;

                socket.onopen = () => {
                    if (isDisposed) return;
                    setSocketStatus('authenticating');
                    socket.send(JSON.stringify({ type: 'AUTH', accessToken }));
                };

                socket.onmessage = (messageEvent) => {
                    if (isDisposed) return;

                    let event: SupportWebSocketEvent;
                    try {
                        event = JSON.parse(messageEvent.data);
                    } catch {
                        ToastService.error('Du lieu chat khong hop le.');
                        return;
                    }

                    if (event.type === 'AUTH_SUCCESS') {
                        setIsAuthenticatedSocket(true);
                        setSocketStatus('ready');
                        return;
                    }

                    if (event.type === 'MESSAGE_CREATED') {
                        const createdMessage = getMessageFromEvent(event);
                        if (createdMessage) {
                            setMessages((current) => [...current, createdMessage]);
                        }
                        return;
                    }

                    if (event.type === 'ERROR') {
                        ToastService.error(event.error || 'Khong the xu ly tin nhan ho tro.');
                    }
                };

                socket.onerror = () => {
                    if (isDisposed) return;
                    setSocketStatus('error');
                };

                socket.onclose = () => {
                    if (isDisposed) return;
                    setSocketStatus('closed');
                    setIsAuthenticatedSocket(false);
                };
            })
            .catch((error) => {
                if (isDisposed) return;
                setIsInitializing(false);
                setSocketStatus('error');
                ToastService.error(error?.response?.data?.message || 'Khong the khoi tao chat ho tro.');
            });


        return () => {
            isDisposed = true;
            const socket = socketRef.current;
            if (socket) {
                socket.onopen = null;
                socket.onmessage = null;
                socket.onerror = null;
                socket.onclose = null;
                socket.close();
            }
            socketRef.current = null;
        };
    }, [accessToken, canUseChat, isOpen]);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }, [messages, isOpen]);

    if (!canUseChat) {
        return null;
    }

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
        if (isInitializing) return 'Dang khoi tao...';
        if (isHistoryLoading) return 'Dang tai lich su...';
        if (socketStatus === 'ready') return 'Dang truc tuyen';
        if (socketStatus === 'authenticating') return 'Dang xac thuc...';
        if (socketStatus === 'connecting') return 'Dang ket noi...';
        if (socketStatus === 'error') return 'Ket noi bi loi';
        return 'Mat ket noi';
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
                            <p className="text-sm font-black text-on-surface">Chat voi SOBU</p>
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

                    <div className="flex h-[24rem] flex-col bg-surface-container-lowest">
                        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-3">
                            {isHistoryLoading && messages.length === 0 ? (
                                <div className="flex h-full items-center justify-center text-xs font-bold text-outline">
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin text-primary"/>
                                    Dang tai lich su...
                                </div>
                            ) : messages.length === 0 ? (
                                <div className="flex h-full flex-col items-center justify-center text-center">
                                    <MessageCircle className="mb-3 h-8 w-8 text-primary"/>
                                    <p className="text-sm font-black text-on-surface">Can ho tro?</p>
                                    <p className="mt-1 max-w-[16rem] text-xs font-semibold leading-relaxed text-outline">
                                        Gui tin nhan, doi ngu SOBU se phan hoi ngay khi co the.
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
                                placeholder={isAuthenticatedSocket ? 'Nhap tin nhan...' : 'Dang ket noi...'}
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
                </section>
            )}

            <button
                type="button"
                aria-label={isOpen ? 'An chat ho tro' : 'Mo chat ho tro'}
                aria-expanded={isOpen}
                onClick={() => setIsOpen((current) => !current)}
                className="inline-flex h-14 cursor-pointer items-center gap-2 rounded-full bg-primary px-4 text-sm font-black text-on-primary shadow-[0_14px_35px_rgba(0,97,142,0.28)] transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
            >
                <MessageCircle className="h-5 w-5"/>
                <span>Ho tro</span>
            </button>
        </div>
    );
}
