import {useEffect, useMemo, useState} from 'react';
import {ExternalLink, Loader2, MessageCircle, X} from 'lucide-react';
import {usePublicUiStore} from '../../store/usePublicUiStore';
import {parseJsonConfig} from '../../utils/website-config';

const ZALO_SDK_SCRIPT_ID = 'zalo-sdk-script';
const ZALO_SDK_SRC = 'https://sp.zalo.me/plugins/sdk.js';

type SdkStatus = 'idle' | 'loading' | 'loaded' | 'error';

interface ZaloChatConfig {
    provider?: string;
    pageId?: string;
    oaid?: string;
    greetingText?: string;
    welcomeMessage?: string;
    autoPopup?: boolean | string | number;
    width?: number | string;
    height?: number | string;
}

type SocialLinks = Record<string, string>;

declare global {
    interface Window {
        ZaloSocialSDK?: {
            reload?: () => void;
        };
    }
}

const isEnabled = (value?: string) => value?.trim().toLowerCase() === 'true';

const toBooleanFlag = (value: ZaloChatConfig['autoPopup']) =>
    value === true || value === 1 || value === '1' || String(value).toLowerCase() === 'true';

const clampDimension = (value: ZaloChatConfig['width'], fallback: number, min: number, max: number) => {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return fallback;
    return Math.min(Math.max(Math.round(parsed), min), max);
};

function useZaloSdk(shouldLoad: boolean): SdkStatus {
    const [status, setStatus] = useState<SdkStatus>('idle');

    useEffect(() => {
        if (!shouldLoad || typeof document === 'undefined') return;

        const existingScript = document.getElementById(ZALO_SDK_SCRIPT_ID) as HTMLScriptElement | null;
        if (existingScript?.dataset.loaded === 'true') {
            setStatus('loaded');
            return;
        }
        if (existingScript?.dataset.error === 'true') {
            setStatus('error');
            return;
        }

        const script = existingScript || document.createElement('script');

        const handleLoad = () => {
            script.dataset.loaded = 'true';
            delete script.dataset.error;
            setStatus('loaded');
        };
        const handleError = () => {
            script.dataset.error = 'true';
            delete script.dataset.loaded;
            setStatus('error');
        };

        script.addEventListener('load', handleLoad);
        script.addEventListener('error', handleError);

        if (!existingScript) {
            script.id = ZALO_SDK_SCRIPT_ID;
            script.src = ZALO_SDK_SRC;
            script.async = true;
            document.body.appendChild(script);
        }

        setStatus('loading');

        return () => {
            script.removeEventListener('load', handleLoad);
            script.removeEventListener('error', handleError);
        };
    }, [shouldLoad]);

    return status;
}

export default function ChatDock() {
    const configMap = usePublicUiStore((state) => state.configMap);
    const [isOpen, setIsOpen] = useState(false);

    const chatConfig = parseJsonConfig<ZaloChatConfig>(configMap, 'social_chat_config', {});
    const socialLinks = parseJsonConfig<SocialLinks>(configMap, 'social_links', {});

    const provider = (chatConfig.provider || 'zalo').trim().toLowerCase();
    const pageId = (chatConfig.pageId || chatConfig.oaid || '').trim();
    const zaloUrl = socialLinks.zalo?.trim();
    const hasPageId = Boolean(pageId);

    const widget = useMemo(() => ({
        greetingText: chatConfig.greetingText || chatConfig.welcomeMessage || 'SOBU co the ho tro gi cho ban?',
        autoPopup: toBooleanFlag(chatConfig.autoPopup) ? '1' : '0',
        width: clampDimension(chatConfig.width, 360, 280, 420),
        height: clampDimension(chatConfig.height, 460, 320, 560),
    }), [chatConfig.autoPopup, chatConfig.greetingText, chatConfig.height, chatConfig.welcomeMessage, chatConfig.width]);

    const ZaloIcon = ({className}: { className?: string }) => (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48px" height="48px">
            <path fill="#2962ff"
                  d="M15,36V6.827l-1.211-0.811C8.64,8.083,5,13.112,5,19v10c0,7.732,6.268,14,14,14h10	c4.722,0,8.883-2.348,11.417-5.931V36H15z"/>
            <path fill="#eee"
                  d="M29,5H19c-1.845,0-3.601,0.366-5.214,1.014C10.453,9.25,8,14.528,8,19	c0,6.771,0.936,10.735,3.712,14.607c0.216,0.301,0.357,0.653,0.376,1.022c0.043,0.835-0.129,2.365-1.634,3.742	c-0.162,0.148-0.059,0.419,0.16,0.428c0.942,0.041,2.843-0.014,4.797-0.877c0.557-0.246,1.191-0.203,1.729,0.083	C20.453,39.764,24.333,40,28,40c4.676,0,9.339-1.04,12.417-2.916C42.038,34.799,43,32.014,43,29V19C43,11.268,36.732,5,29,5z"/>
            <path fill="#2962ff"
                  d="M36.75,27C34.683,27,33,25.317,33,23.25s1.683-3.75,3.75-3.75s3.75,1.683,3.75,3.75	S38.817,27,36.75,27z M36.75,21c-1.24,0-2.25,1.01-2.25,2.25s1.01,2.25,2.25,2.25S39,24.49,39,23.25S37.99,21,36.75,21z"/>
            <path fill="#2962ff" d="M31.5,27h-1c-0.276,0-0.5-0.224-0.5-0.5V18h1.5V27z"/>
            <path fill="#2962ff"
                  d="M27,19.75v0.519c-0.629-0.476-1.403-0.769-2.25-0.769c-2.067,0-3.75,1.683-3.75,3.75	S22.683,27,24.75,27c0.847,0,1.621-0.293,2.25-0.769V26.5c0,0.276,0.224,0.5,0.5,0.5h1v-7.25H27z M24.75,25.5	c-1.24,0-2.25-1.01-2.25-2.25S23.51,21,24.75,21S27,22.01,27,23.25S25.99,25.5,24.75,25.5z"/>
            <path fill="#2962ff"
                  d="M21.25,18h-8v1.5h5.321L13,26h0.026c-0.163,0.211-0.276,0.463-0.276,0.75V27h7.5	c0.276,0,0.5-0.224,0.5-0.5v-1h-5.321L21,19h-0.026c0.163-0.211,0.276-0.463,0.276-0.75V18z"/>
        </svg>
    );

    const sdkStatus = useZaloSdk(isOpen && provider === 'zalo' && hasPageId);

    useEffect(() => {
        if (isOpen && sdkStatus === 'loaded') {
            window.ZaloSocialSDK?.reload?.();
        }
    }, [isOpen, pageId, sdkStatus]);

    if (!isEnabled(configMap?.social_chat_widget_enabled) || provider !== 'zalo') {
        return null;
    }

    return (
        <div
            aria-label="Thanh chat ho tro"
            className="fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-[60] flex flex-col items-end gap-3 lg:bottom-6"
        >
            {isOpen && (
                <section
                    aria-label="Zalo chat"
                    className="w-[min(calc(100vw-2rem),24rem)] overflow-hidden rounded-2xl border border-outline-variant/40 bg-surface-container-lowest shadow-[0_18px_55px_rgba(14,48,78,0.20)]"
                >
                    <div
                        className="flex items-center justify-between gap-3 border-b border-outline-variant/25 bg-surface-container-low px-4 py-3">
                        <div className="min-w-0">
                            <p className="text-sm font-black text-on-surface">Chat với SOBU</p>
                            <p className="truncate text-[11px] font-semibold text-outline">Hỗ trợ nhanh qua Zalo</p>
                        </div>
                        <button
                            type="button"
                            aria-label="Đóng chat Zalo"
                            onClick={() => setIsOpen(false)}
                            className="flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container-high hover:text-on-surface focus-visible:ring-2 focus-visible:ring-primary/40"
                        >
                            <X className="h-4 w-4"/>
                        </button>
                    </div>

                    <div
                        className="relative bg-surface-container-lowest p-3"
                        style={{height: Math.min(widget.height + 24, 520)}}
                    >
                        {!hasPageId ? (
                            <div
                                className="flex h-full flex-col items-center justify-center rounded-xl border border-outline-variant/30 bg-surface-container-low px-5 text-center">
                                <MessageCircle className="mb-3 h-8 w-8 text-primary"/>
                                <p className="mt-2 max-w-[18rem] text-xs font-semibold leading-relaxed text-outline">
                                    Đang tải...
                                </p>
                                {zaloUrl && (
                                    <a
                                        href={zaloUrl}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2.5 text-xs font-black uppercase text-on-primary transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
                                    >
                                        Mở Zalo
                                        <ExternalLink className="h-3.5 w-3.5"/>
                                    </a>
                                )}
                            </div>
                        ) : sdkStatus === 'error' ? (
                            <div
                                className="flex h-full flex-col items-center justify-center rounded-xl border border-outline-variant/30 bg-surface-container-low px-5 text-center">
                                <MessageCircle className="mb-3 h-8 w-8 text-primary"/>
                                <p className="text-sm font-black text-on-surface">Không tải được Zalo chat</p>
                                {zaloUrl ? (
                                    <a
                                        href={zaloUrl}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2.5 text-xs font-black uppercase text-on-primary transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
                                    >
                                        Mở Zalo
                                        <ExternalLink className="h-3.5 w-3.5"/>
                                    </a>
                                ) : (
                                    <p className="mt-2 text-xs font-semibold text-outline">Vui lòng thử lại sau.</p>
                                )}
                            </div>
                        ) : (
                            <>
                                {sdkStatus !== 'loaded' && (
                                    <div
                                        className="absolute inset-3 z-10 flex items-center justify-center rounded-xl bg-surface-container-lowest/90 text-xs font-bold text-outline backdrop-blur-sm">
                                        <Loader2 className="mr-2 h-4 w-4 animate-spin text-primary"/>
                                        Dang tai Zalo...
                                    </div>
                                )}
                                <div
                                    className="flex h-full items-center justify-center rounded-xl border border-outline-variant/25 bg-surface-container-low">
                                    <div
                                        className="zalo-chat-widget"
                                        data-oaid={pageId}
                                        data-welcome-message={widget.greetingText}
                                        data-autopopup={widget.autoPopup}
                                        data-width={String(widget.width)}
                                        data-height={String(widget.height)}
                                    />
                                </div>
                            </>
                        )}
                    </div>
                </section>
            )}

            <button
                type="button"
                aria-label={isOpen ? 'Ẩn chat Zalo' : 'Mở chat Zalo'}
                aria-expanded={isOpen}
                onClick={() => setIsOpen((current) => !current)}
                className="inline-flex h-14 cursor-pointer items-center gap-2 rounded-full bg-primary px-4 text-sm font-black text-on-primary shadow-[0_14px_35px_rgba(0,97,142,0.28)] transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
            >
                <ZaloIcon className="h-5 w-5" />
            </button>
        </div>
    );
}
