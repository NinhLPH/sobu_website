interface BannerPlaceholderProps {
    label: string;
}

export default function BannerPlaceholder({label}: BannerPlaceholderProps) {
    return (
        <div
            className="flex h-[min(560px,calc(100vh-8rem))] min-h-[360px] w-full flex-col overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface-container-lowest p-3 shadow-sm"
            aria-label={label}
        >
            <div className="flex flex-1 animate-pulse flex-col justify-between rounded-xl bg-surface-container p-3">
                <div className="space-y-2">
                    <div className="h-2 w-16 rounded-full bg-surface-container-high"/>
                    <div className="h-2 w-24 rounded-full bg-surface-container-high"/>
                </div>
                <div className="flex flex-col items-center gap-3 text-center">
                    <div className="h-16 w-16 rounded-2xl bg-surface-container-high"/>
                    <span className="text-[10px] font-black uppercase tracking-[0.16em] text-on-surface-variant">
                        {label}
                    </span>
                </div>
                <div className="space-y-2">
                    <div className="h-2 w-full rounded-full bg-surface-container-high"/>
                    <div className="h-2 w-2/3 rounded-full bg-surface-container-high"/>
                </div>
            </div>
        </div>
    );
}
