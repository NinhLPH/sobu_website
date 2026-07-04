import SupportChatDock from './SupportChatDock';
import ZaloChatDock from './ZaloChatDock';

export default function ChatDock() {
    return (
        <div
            aria-label="Thanh chat ho tro"
            className="fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-[60] flex flex-col items-end gap-3 lg:bottom-6"
        >
            <SupportChatDock/>
            <ZaloChatDock/>
        </div>
    );
}
