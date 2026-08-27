import {createContext, ReactNode, useCallback, useContext, useEffect, useId, useRef, useState} from 'react';
import {AlertTriangle, HelpCircle, ShieldAlert, X} from 'lucide-react';

export type ConfirmDialogTone = 'default' | 'warning' | 'danger';

export interface ConfirmDialogOptions {
    title: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    tone?: ConfirmDialogTone;
}

type PendingConfirmation = ConfirmDialogOptions & { resolve: (confirmed: boolean) => void };
type ConfirmDialogContextValue = (options: ConfirmDialogOptions) => Promise<boolean>;

const ConfirmDialogContext = createContext<ConfirmDialogContextValue | null>(null);

export function useConfirmDialog() {
    const value = useContext(ConfirmDialogContext);
    if (!value) throw new Error('useConfirmDialog must be used within ConfirmDialogProvider');
    return value;
}

export function ConfirmDialogProvider({children}: { children: ReactNode }) {
    const [pending, setPending] = useState<PendingConfirmation | null>(null);
    const dialogRef = useRef<HTMLDivElement>(null);
    const cancelRef = useRef<HTMLButtonElement>(null);
    const titleId = useId();
    const descriptionId = useId();

    const confirm = useCallback((options: ConfirmDialogOptions) => new Promise<boolean>((resolve) => {
        setPending(current => {
            current?.resolve(false);
            return {...options, resolve};
        });
    }), []);

    const finish = useCallback((confirmed: boolean) => {
        setPending(current => {
            current?.resolve(confirmed);
            return null;
        });
    }, []);

    useEffect(() => {
        if (!pending) return;
        const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        window.setTimeout(() => cancelRef.current?.focus(), 0);

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                event.preventDefault();
                finish(false);
                return;
            }
            if (event.key !== 'Tab' || !dialogRef.current) return;
            const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>('button:not([disabled]), [href], [tabindex]:not([tabindex="-1"])'));
            if (!focusable.length) return;
            const first = focusable[0];
            const last = focusable[focusable.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        };
        document.addEventListener('keydown', onKeyDown);
        return () => {
            document.removeEventListener('keydown', onKeyDown);
            document.body.style.overflow = originalOverflow;
            previousFocus?.focus();
        };
    }, [finish, pending]);

    const tone = pending?.tone || 'default';
    const toneClasses = {
        default: 'bg-primary/10 text-primary',
        warning: 'bg-amber-500/15 text-amber-600 dark:text-amber-300',
        danger: 'bg-error/10 text-error'
    }[tone];
    const confirmClasses = tone === 'danger'
        ? 'bg-error text-on-error hover:brightness-110'
        : tone === 'warning'
            ? 'bg-amber-500 text-slate-950 hover:bg-amber-400'
            : 'bg-primary text-on-primary hover:brightness-110';
    const Icon = tone === 'danger' ? ShieldAlert : tone === 'warning' ? AlertTriangle : HelpCircle;

    return <ConfirmDialogContext.Provider value={confirm}>
        {children}
        {pending && <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 p-4 backdrop-blur-[2px]"
                         onMouseDown={event => event.target === event.currentTarget && finish(false)}>
            <div ref={dialogRef} role="alertdialog" aria-modal="true" aria-labelledby={titleId}
                 aria-describedby={descriptionId} tabIndex={-1}
                 className="w-full max-w-md rounded-2xl border border-outline-variant/35 bg-surface-container-lowest p-5 text-on-surface shadow-2xl outline-none sm:p-6">
                <div className="flex items-start gap-4">
                    <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${toneClasses}`}>
                        <Icon className="h-5 w-5" aria-hidden="true"/>
                    </div>
                    <div className="min-w-0 flex-1">
                        <div className="flex items-start justify-between gap-3">
                            <h2 id={titleId} className="text-lg font-black text-on-surface">{pending.title}</h2>
                            <button type="button" onClick={() => finish(false)} aria-label="Đóng hộp thoại xác nhận"
                                    className="-mr-2 -mt-2 rounded-lg p-2 text-outline transition-colors hover:bg-surface-container hover:text-on-surface focus-visible:ring-2 focus-visible:ring-primary/40">
                                <X className="h-4 w-4"/>
                            </button>
                        </div>
                        <p id={descriptionId} className="mt-2 text-sm font-medium leading-6 text-on-surface-variant">{pending.message}</p>
                    </div>
                </div>
                <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                    <button ref={cancelRef} type="button" onClick={() => finish(false)}
                            className="min-h-10 rounded-lg border border-outline-variant/50 bg-surface px-4 text-sm font-bold text-on-surface transition-colors hover:bg-surface-container focus-visible:ring-2 focus-visible:ring-primary/40">
                        {pending.cancelLabel || 'Hủy'}
                    </button>
                    <button type="button" onClick={() => finish(true)}
                            className={`min-h-10 rounded-lg px-4 text-sm font-black transition-colors focus-visible:ring-2 focus-visible:ring-primary/40 ${confirmClasses}`}>
                        {pending.confirmLabel || 'Xác nhận'}
                    </button>
                </div>
            </div>
        </div>}
    </ConfirmDialogContext.Provider>;
}
