import {ReactNode, useEffect, useId, useRef} from 'react';
import {AlertCircle, ChevronLeft, ChevronRight, Loader2, Search, X} from 'lucide-react';

export const inputClass = 'min-h-10 w-full rounded-lg border border-outline-variant/60 bg-surface px-3 text-sm text-on-surface outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15 disabled:cursor-not-allowed disabled:bg-surface-container';
export const labelClass = 'mb-1.5 block text-xs font-bold uppercase tracking-wide text-outline';

export function AdminPage({title, description, actions, children}: {
    title: string;
    description: string;
    actions?: ReactNode;
    children: ReactNode
}) {
    return <section className="space-y-5">
        <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
            <div><h1 className="text-2xl font-black tracking-tight text-on-surface">{title}</h1><p
                className="mt-1 max-w-3xl text-sm text-outline">{description}</p></div>
            {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
        </header>
        {children}
    </section>;
}

export function AdminCard({children, className = ''}: { children: ReactNode; className?: string }) {
    return <div
        className={`overflow-hidden rounded-xl border border-outline-variant/35 bg-surface shadow-sm ${className}`}>{children}</div>;
}

export function AdminButton({
                                children,
                                variant = 'primary',
                                className = '',
                                ...props
                            }: React.ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
}) {
    const styles = {
        primary: 'bg-primary text-on-primary hover:brightness-110',
        secondary: 'border border-outline-variant bg-surface text-on-surface hover:bg-surface-container',
        danger: 'bg-error text-white hover:brightness-110',
        ghost: 'text-on-surface-variant hover:bg-surface-container'
    };
    return <button
        className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${styles[variant]} ${className}`} {...props}>{children}</button>;
}

export function AdminSearch({value, onChange, placeholder, ariaLabel}: {
    value: string;
    onChange: (value: string) => void;
    placeholder: string;
    ariaLabel?: string
}) {
    return <label className="relative block min-w-0 flex-1"><span
        className="sr-only">{ariaLabel || placeholder}</span><Search
        className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/><input
        aria-label={ariaLabel || placeholder} value={value} onChange={e => onChange(e.target.value)}
        placeholder={placeholder} className={`${inputClass} pl-9`}/></label>;
}

export function AdminStatus({active, activeText = 'Hoạt động', inactiveText = 'Tạm dừng'}: {
    active: boolean;
    activeText?: string;
    inactiveText?: string
}) {
    return <span
        className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${active ? 'bg-emerald-100 text-emerald-800' : 'bg-surface-container text-outline'}`}>{active ? activeText : inactiveText}</span>;
}

export function AdminLoading({label = 'Đang tải dữ liệu...'}: { label?: string }) {
    return <div className="flex min-h-48 items-center justify-center gap-2 text-sm text-outline"><Loader2
        className="h-5 w-5 animate-spin text-primary"/>{label}</div>;
}

export function AdminEmpty({title, description}: { title: string; description?: string }) {
    return <div className="flex min-h-48 flex-col items-center justify-center px-6 text-center"><Search
        className="mb-3 h-8 w-8 text-outline-variant"/><p
        className="font-bold text-on-surface">{title}</p>{description &&
        <p className="mt-1 text-sm text-outline">{description}</p>}</div>;
}

export function AdminError({message, onRetry}: { message: string; onRetry?: () => void }) {
    return <div className="flex min-h-48 flex-col items-center justify-center px-6 text-center"><AlertCircle
        className="mb-3 h-8 w-8 text-error"/><p className="font-bold text-on-surface">Không thể tải dữ liệu</p><p
        className="mt-1 text-sm text-outline">{message}</p>{onRetry &&
        <AdminButton variant="secondary" className="mt-4" onClick={onRetry}>Thử lại</AdminButton>}</div>;
}

export function AdminModal({open, title, description, children, onClose, size = 'lg'}: {
    open: boolean;
    title: string;
    description?: string;
    children: ReactNode;
    onClose: () => void;
    size?: 'md' | 'lg' | 'xl'
}) {
    const dialogRef = useRef<HTMLDivElement>(null);
    const onCloseRef = useRef(onClose);
    const titleId = useId();
    onCloseRef.current = onClose;

    useEffect(() => {
        if (!open) return;
        const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        const dialog = dialogRef.current;
        const focusableSelector = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';
        const initialFocus = dialog?.querySelector<HTMLElement>('[data-autofocus], input:not([disabled]), select:not([disabled]), textarea:not([disabled])') ?? dialog;
        window.setTimeout(() => initialFocus?.focus(), 0);

        const handler = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                event.preventDefault();
                onCloseRef.current();
                return;
            }
            if (event.key !== 'Tab' || !dialog) return;
            const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(focusableSelector));
            if (!focusable.length) {
                event.preventDefault();
                dialog.focus();
                return;
            }
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
        document.addEventListener('keydown', handler);
        return () => {
            document.removeEventListener('keydown', handler);
            previousFocus?.focus();
        };
    }, [open]);
    if (!open) return null;
    const width = {md: 'max-w-xl', lg: 'max-w-3xl', xl: 'max-w-5xl'}[size];
    return <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/55 p-3 sm:p-6" role="dialog"
                aria-modal="true" aria-labelledby={titleId}
                onMouseDown={event => event.target === event.currentTarget && onClose()}>
        <div ref={dialogRef} tabIndex={-1} className={`max-h-[92vh] w-full ${width} overflow-y-auto rounded-2xl bg-surface shadow-2xl outline-none`}>
            <header
                className="sticky top-0 z-10 flex items-start justify-between border-b border-outline-variant/35 bg-surface px-5 py-4">
                <div><h2 id={titleId} className="text-lg font-black text-on-surface">{title}</h2>{description &&
                    <p className="mt-1 text-sm text-outline">{description}</p>}</div>
                <button type="button"
                        className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-on-surface"
                        onClick={onClose} aria-label="Đóng"><X className="h-5 w-5"/></button>
            </header>
            {children}
        </div>
    </div>;
}

export function AdminPagination({page, totalPages, onChange}: {
    page: number;
    totalPages: number;
    onChange: (page: number) => void
}) {
    if (totalPages <= 1) return null;
    return <div className="flex items-center justify-between border-t border-outline-variant/30 px-4 py-3 text-sm"><span
        className="text-outline">Trang {page + 1}/{totalPages}</span>
        <div className="flex gap-2"><AdminButton variant="secondary" className="!min-h-9 !px-3" disabled={page <= 0}
                                                 onClick={() => onChange(page - 1)}
                                                 aria-label="Trang trước"><ChevronLeft
            className="h-4 w-4"/></AdminButton><AdminButton variant="secondary" className="!min-h-9 !px-3"
                                                            disabled={page >= totalPages - 1}
                                                            onClick={() => onChange(page + 1)}
                                                            aria-label="Trang sau"><ChevronRight
            className="h-4 w-4"/></AdminButton></div>
    </div>;
}

export function Field({label, children, hint}: { label: string; children: ReactNode; hint?: string }) {
    return <label className="block"><span className={labelClass}>{label}</span>{children}{hint &&
        <span className="mt-1 block text-xs text-outline">{hint}</span>}</label>;
}

export const getApiError = (error: any, fallback = 'Đã có lỗi xảy ra.') => error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
