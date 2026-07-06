import { useState } from 'react';
import SupportChatDock from './SupportChatDock';
import ZaloChatDock from './ZaloChatDock';

type ActiveChatDock = 'support' | 'zalo' | null;

export default function ChatDock() {
    const [activeDock, setActiveDock] = useState<ActiveChatDock>(null);

    return (
        <div
            aria-label="Thanh chat ho tro"
            className="fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-[60] flex flex-col items-end gap-3 lg:bottom-6"
        >
            <SupportChatDock
                isOpen={activeDock === 'support'}
                onOpenChange={(isOpen) => setActiveDock(isOpen ? 'support' : null)}
            />
            <ZaloChatDock
                isOpen={activeDock === 'zalo'}
                onOpenChange={(isOpen) => setActiveDock(isOpen ? 'zalo' : null)}
            />
        </div>
    );
}
