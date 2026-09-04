import {useEffect, useState} from 'react';
import {MessageCircle, X} from 'lucide-react';
import SupportChatDock from './SupportChatDock';
import ZaloChatDock from './ZaloChatDock';

type ActiveChatDock = 'support' | null;

const DESKTOP_MEDIA_QUERY = '(min-width: 1024px)';

const getIsDesktopLayout = () => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
        return true;
    }

    return window.matchMedia(DESKTOP_MEDIA_QUERY).matches;
};

export default function ChatDock() {
    const [activeDock, setActiveDock] = useState<ActiveChatDock>(null);
    const [isMobileExpanded, setIsMobileExpanded] = useState(false);
    const [isDesktopLayout, setIsDesktopLayout] = useState(getIsDesktopLayout);

    useEffect(() => {
        if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
            return;
        }

        const mediaQuery = window.matchMedia(DESKTOP_MEDIA_QUERY);
        const updateLayout = () => setIsDesktopLayout(mediaQuery.matches);

        updateLayout();
        mediaQuery.addEventListener('change', updateLayout);
        return () => mediaQuery.removeEventListener('change', updateLayout);
    }, []);

    useEffect(() => {
        if (!isDesktopLayout && activeDock === 'support') {
            setIsMobileExpanded(true);
        }
    }, [activeDock, isDesktopLayout]);

    const handleSupportOpenChange = (isOpen: boolean) => {
        setActiveDock(isOpen ? 'support' : null);
        if (isOpen && !isDesktopLayout) {
            setIsMobileExpanded(true);
        }
    };

    const toggleMobileExpand = () => {
        const next = !isMobileExpanded;
        setIsMobileExpanded(next);
        if (!next && activeDock === 'support') {
            setActiveDock(null);
        }
    };

    return (
        <div
            aria-label="Thanh chat ho tro"
            className="fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-[60] flex flex-col items-end gap-3 lg:bottom-6"
        >
            {isDesktopLayout ? (
                <div className="flex flex-col items-end gap-3">
                    <ZaloChatDock/>
                    <SupportChatDock
                        isOpen={activeDock === 'support'}
                        onOpenChange={handleSupportOpenChange}
                    />
                </div>
            ) : (
                <div className="flex flex-col items-end gap-3">
                    {isMobileExpanded && (
                        <div className="flex animate-in fade-in slide-in-from-bottom-2 duration-200 flex-col items-end gap-3">
                            <SupportChatDock
                                isOpen={activeDock === 'support'}
                                onOpenChange={handleSupportOpenChange}
                            />
                            <ZaloChatDock/>
                        </div>
                    )}

                    {activeDock !== 'support' && (
                        <button
                            type="button"
                            aria-label={isMobileExpanded ? 'Thu gon menu ho tro' : 'Mo menu ho tro'}
                            aria-expanded={isMobileExpanded}
                            onClick={toggleMobileExpand}
                            className={`flex h-14 w-14 cursor-pointer items-center justify-center rounded-full text-white shadow-[0_14px_35px_rgba(0,97,142,0.35)] transition-all duration-300 focus-visible:ring-2 focus-visible:ring-primary/40 ${
                                isMobileExpanded
                                    ? 'bg-on-surface/80 hover:bg-on-surface rotate-0'
                                    : 'bg-primary hover:bg-primary-container'
                            }`}
                        >
                            {isMobileExpanded ? (
                                <X className="h-5 w-5"/>
                            ) : (
                                <MessageCircle className="h-5 w-5"/>
                            )}
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}
