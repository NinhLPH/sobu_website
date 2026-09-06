import {useEffect, useId, useMemo, useRef, useState} from 'react';
import {AlertTriangle, ChevronLeft, Loader2, TicketPercent, Truck, X} from 'lucide-react';
import {ActiveVoucher} from '../../interface/voucher.model';
import {formatCurrency} from '../../utils/format';

interface VoucherPickerModalProps {
    open: boolean;
    vouchers: ActiveVoucher[];
    subtotal: number;
    discountVoucherCode: string;
    shippingVoucherCode: string;
    autoAppliedVoucherCodes: string[];
    isLoading: boolean;
    error: string | null;
    disabled?: boolean;
    onSelectionChange: (voucher: ActiveVoucher, selected: boolean) => void;
    onClose: () => void;
}

const isShippingVoucher = (voucher: ActiveVoucher) =>
    voucher.slot === 'SHIPPING' || voucher.type === 'FREE_SHIP';

const voucherTitle = (voucher: ActiveVoucher) => {
    if (voucher.type === 'FREE_SHIP') return 'Miễn phí vận chuyển';
    if (voucher.type === 'DISCOUNT_PERCENT') return `Giảm ${voucher.value ?? 0}%`;
    return `Giảm ${formatCurrency(voucher.value ?? 0)}`;
};

const voucherCondition = (voucher: ActiveVoucher) => {
    const conditions: string[] = [];
    if (voucher.minOrderValue) conditions.push(`Đơn tối thiểu ${formatCurrency(voucher.minOrderValue)}`);
    if (voucher.maxDiscountAmount) conditions.push(`Tối đa ${formatCurrency(voucher.maxDiscountAmount)}`);
    return conditions.length > 0 ? conditions.join(' · ') : 'Không yêu cầu giá trị đơn tối thiểu';
};

const formatVoucherDate = (value?: string | null) => {
    if (!value) return 'Không giới hạn';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'Không giới hạn' : date.toLocaleDateString('vi-VN');
};

const voucherScopeText = (voucher: ActiveVoucher) => {
    if (voucher.scope === 'PRODUCT') return 'Sản phẩm được áp dụng';
    if (voucher.scope === 'CATEGORY') return 'Danh mục được áp dụng';
    return isShippingVoucher(voucher) ? 'Phí vận chuyển của đơn hàng' : 'Toàn bộ đơn hàng';
};

function VoucherCard({
    voucher,
    subtotal,
    selected,
    disabled,
    onSelectionChange,
    onShowConditions
}: {
    voucher: ActiveVoucher;
    subtotal: number;
    selected: boolean;
    disabled?: boolean;
    onSelectionChange: (voucher: ActiveVoucher, selected: boolean) => void;
    onShowConditions: (voucher: ActiveVoucher, trigger: HTMLButtonElement) => void;
}) {
    const shipping = isShippingVoucher(voucher);
    const minimum = Number(voucher.minOrderValue ?? 0);
    const missingAmount = Number.isFinite(minimum) ? Math.max(0, minimum - subtotal) : 0;
    const eligible = missingAmount === 0;
    const inputDisabled = Boolean(disabled) || !eligible;

    return (
        <article className={`overflow-hidden rounded-2xl border bg-surface-container-lowest transition-colors ${
            eligible
                ? selected
                    ? 'border-primary shadow-[0_8px_24px_-18px_rgb(var(--color-primary))]'
                    : 'border-outline-variant/35 hover:border-primary/45'
                : 'border-outline-variant/25 opacity-60'
        }`}>
            <div className="grid min-h-[142px] grid-cols-[68px_minmax(0,1fr)_44px] sm:grid-cols-[84px_minmax(0,1fr)_52px]">
                <div className={`flex flex-col items-center justify-center gap-2 border-r border-dashed border-outline-variant/40 px-2 text-center ${
                    shipping ? 'bg-surface text-primary' : 'bg-primary text-on-primary'
                }`}>
                    {shipping
                        ? <Truck className="h-6 w-6" aria-hidden="true"/>
                        : <TicketPercent className="h-6 w-6" aria-hidden="true"/>}
                    <span className="text-[9px] font-black uppercase tracking-wide sm:text-[10px]">
                        {shipping ? 'Freeship' : 'Giảm giá'}
                    </span>
                </div>

                <div className="min-w-0 px-3 py-4 sm:px-4">
                    <p className="text-sm font-black text-on-surface">{voucherTitle(voucher)}</p>
                    <p className="mt-1 truncate text-[11px] font-black uppercase tracking-wide text-primary">{voucher.code}</p>
                    <p className="mt-2 text-[11px] font-semibold leading-relaxed text-on-surface-variant">
                        {voucherCondition(voucher)}
                    </p>
                    <p className="mt-1 text-[10px] font-semibold text-outline">
                        Hạn sử dụng: {formatVoucherDate(voucher.endDate)}
                    </p>
                    <button
                        type="button"
                        onClick={(event) => onShowConditions(voucher, event.currentTarget)}
                        className="mt-2 cursor-pointer text-[11px] font-black text-primary underline-offset-2 transition-colors hover:underline focus-visible:rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
                    >
                        Điều kiện
                    </button>
                    {!eligible && (
                        <p className="mt-2 flex items-start gap-1.5 text-[10px] font-bold leading-relaxed text-error">
                            <AlertTriangle className="mt-0.5 h-3 w-3 shrink-0" aria-hidden="true"/>
                            Mua thêm {formatCurrency(missingAmount)} để sử dụng mã này
                        </p>
                    )}
                </div>

                <div className="flex items-start justify-center px-2 py-4">
                    <input
                        type="checkbox"
                        checked={selected}
                        disabled={inputDisabled}
                        onChange={(event) => onSelectionChange(voucher, event.target.checked)}
                        aria-label={`${selected ? 'Bỏ chọn' : 'Chọn'} voucher ${voucher.code}`}
                        className="h-5 w-5 cursor-pointer rounded border-outline-variant text-primary accent-primary focus-visible:ring-2 focus-visible:ring-primary/40 disabled:cursor-not-allowed"
                    />
                </div>
            </div>
        </article>
    );
}

export default function VoucherPickerModal({
    open,
    vouchers,
    subtotal,
    discountVoucherCode,
    shippingVoucherCode,
    autoAppliedVoucherCodes,
    isLoading,
    error,
    disabled,
    onSelectionChange,
    onClose
}: VoucherPickerModalProps) {
    const [conditionVoucher, setConditionVoucher] = useState<ActiveVoucher | null>(null);
    const dialogRef = useRef<HTMLDivElement>(null);
    const closeButtonRef = useRef<HTMLButtonElement>(null);
    const conditionTriggerRef = useRef<HTMLButtonElement | null>(null);
    const previousFocusRef = useRef<HTMLElement | null>(null);
    const conditionVoucherRef = useRef<ActiveVoucher | null>(null);
    const titleId = useId();
    const descriptionId = useId();

    const groups = useMemo(() => ({
        discount: vouchers.filter(voucher => !isShippingVoucher(voucher)),
        shipping: vouchers.filter(isShippingVoucher)
    }), [vouchers]);
    const normalizedAutoAppliedCodes = useMemo(
        () => new Set(autoAppliedVoucherCodes.map(code => code.trim().toUpperCase())),
        [autoAppliedVoucherCodes]
    );

    useEffect(() => {
        conditionVoucherRef.current = conditionVoucher;
    }, [conditionVoucher]);

    useEffect(() => {
        if (!open) return;
        previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        window.setTimeout(() => closeButtonRef.current?.focus(), 0);

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                event.preventDefault();
                if (conditionVoucherRef.current) {
                    setConditionVoucher(null);
                    window.setTimeout(() => conditionTriggerRef.current?.focus(), 0);
                } else {
                    onClose();
                }
                return;
            }
            if (event.key !== 'Tab' || !dialogRef.current) return;
            const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(
                'button:not([disabled]), input:not([disabled]), [href], [tabindex]:not([tabindex="-1"])'
            ));
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
            setConditionVoucher(null);
            previousFocusRef.current?.focus();
        };
    }, [onClose, open]);

    if (!open) return null;

    const isAutoApplied = (voucher: ActiveVoucher) =>
        normalizedAutoAppliedCodes.has(voucher.code.trim().toUpperCase());

    const selectedCodeFor = (voucher: ActiveVoucher) => {
        const selectedCode = isShippingVoucher(voucher) ? shippingVoucherCode : discountVoucherCode;
        return selectedCode.trim().toUpperCase() === voucher.code.trim().toUpperCase()
            || isAutoApplied(voucher);
    };

    const showConditions = (voucher: ActiveVoucher, trigger: HTMLButtonElement) => {
        conditionTriggerRef.current = trigger;
        setConditionVoucher(voucher);
    };

    const hideConditions = () => {
        setConditionVoucher(null);
        window.setTimeout(() => conditionTriggerRef.current?.focus(), 0);
    };

    return (
        <div
            className="fixed inset-0 z-[90] flex items-center justify-center bg-black/60 p-3 backdrop-blur-[2px] sm:p-6"
            onMouseDown={(event) => event.target === event.currentTarget && onClose()}
        >
            <div
                ref={dialogRef}
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                aria-describedby={descriptionId}
                tabIndex={-1}
                className="flex max-h-[88dvh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-outline-variant/35 bg-surface-container-lowest text-on-surface shadow-2xl outline-none"
            >
                <header className="flex items-start gap-3 border-b border-outline-variant/30 px-4 py-4 sm:px-6">
                    {conditionVoucher && (
                        <button
                            type="button"
                            onClick={hideConditions}
                            aria-label="Quay lại danh sách voucher"
                            className="mt-0.5 cursor-pointer rounded-xl p-2 text-outline transition-colors hover:bg-surface-container hover:text-on-surface focus-visible:ring-2 focus-visible:ring-primary/40"
                        >
                            <ChevronLeft className="h-5 w-5"/>
                        </button>
                    )}
                    <div className="min-w-0 flex-1">
                        <h2 id={titleId} className="text-lg font-black sm:text-xl">
                            {conditionVoucher ? 'Điều kiện voucher' : 'Chọn voucher'}
                        </h2>
                        <p id={descriptionId} className="mt-1 text-xs font-semibold leading-relaxed text-on-surface-variant">
                            {conditionVoucher
                                ? `Thông tin áp dụng của mã ${conditionVoucher.code}`
                                : 'Bạn có thể chọn đồng thời một mã giảm giá và một mã miễn phí vận chuyển.'}
                        </p>
                    </div>
                    <button
                        ref={closeButtonRef}
                        type="button"
                        onClick={onClose}
                        aria-label="Đóng danh sách voucher"
                        className="-mr-1 cursor-pointer rounded-xl p-2 text-outline transition-colors hover:bg-surface-container hover:text-on-surface focus-visible:ring-2 focus-visible:ring-primary/40"
                    >
                        <X className="h-5 w-5"/>
                    </button>
                </header>

                {conditionVoucher ? (
                    <div className="overflow-y-auto px-4 py-5 sm:px-6">
                        <div className="rounded-2xl border border-primary/20 bg-primary/5 p-4 sm:p-5">
                            <p className="text-[11px] font-black uppercase tracking-wider text-primary">{conditionVoucher.code}</p>
                            <h3 className="mt-1 text-lg font-black text-on-surface">{voucherTitle(conditionVoucher)}</h3>
                            <p className="mt-1 text-sm font-semibold text-on-surface-variant">{conditionVoucher.name}</p>
                        </div>
                        <dl className="mt-4 divide-y divide-outline-variant/25 rounded-2xl border border-outline-variant/30 bg-surface px-4 text-sm">
                            <div className="grid gap-1 py-3 sm:grid-cols-[160px_1fr]">
                                <dt className="font-bold text-outline">Giá trị đơn tối thiểu</dt>
                                <dd className="font-semibold text-on-surface">{conditionVoucher.minOrderValue ? formatCurrency(conditionVoucher.minOrderValue) : 'Không yêu cầu'}</dd>
                            </div>
                            <div className="grid gap-1 py-3 sm:grid-cols-[160px_1fr]">
                                <dt className="font-bold text-outline">Mức giảm tối đa</dt>
                                <dd className="font-semibold text-on-surface">{conditionVoucher.maxDiscountAmount ? formatCurrency(conditionVoucher.maxDiscountAmount) : 'Không giới hạn'}</dd>
                            </div>
                            <div className="grid gap-1 py-3 sm:grid-cols-[160px_1fr]">
                                <dt className="font-bold text-outline">Phạm vi áp dụng</dt>
                                <dd className="font-semibold text-on-surface">{voucherScopeText(conditionVoucher)}</dd>
                            </div>
                            <div className="grid gap-1 py-3 sm:grid-cols-[160px_1fr]">
                                <dt className="font-bold text-outline">Hạn sử dụng</dt>
                                <dd className="font-semibold text-on-surface">{formatVoucherDate(conditionVoucher.endDate)}</dd>
                            </div>
                        </dl>
                    </div>
                ) : (
                    <div className="overflow-y-auto px-4 py-5 sm:px-6">
                        {isLoading ? (
                            <div className="flex min-h-48 flex-col items-center justify-center gap-3 text-sm font-bold text-outline" aria-live="polite">
                                <Loader2 className="h-7 w-7 animate-spin text-primary"/>
                                Đang tải voucher...
                            </div>
                        ) : error ? (
                            <div role="alert" className="flex min-h-40 flex-col items-center justify-center gap-3 rounded-2xl border border-error/25 bg-error/5 p-5 text-center text-sm font-bold text-error">
                                <AlertTriangle className="h-7 w-7"/>
                                {error}
                            </div>
                        ) : vouchers.length === 0 ? (
                            <div className="flex min-h-40 items-center justify-center rounded-2xl border border-dashed border-outline-variant/45 bg-surface p-5 text-center text-sm font-semibold text-outline">
                                Hiện chưa có voucher khả dụng.
                            </div>
                        ) : (
                            <div className="space-y-6">
                                {groups.discount.length > 0 && (
                                    <section aria-labelledby="discount-voucher-group">
                                        <h3 id="discount-voucher-group" className="mb-3 text-xs font-black uppercase tracking-wider text-on-surface">
                                            Voucher giảm giá
                                        </h3>
                                        <div className="space-y-3">
                                            {groups.discount.map(voucher => (
                                                <VoucherCard
                                                    key={voucher.id}
                                                    voucher={voucher}
                                                    subtotal={subtotal}
                                                    selected={selectedCodeFor(voucher)}
                                                    disabled={disabled}
                                                    onSelectionChange={onSelectionChange}
                                                    onShowConditions={showConditions}
                                                />
                                            ))}
                                        </div>
                                    </section>
                                )}
                                {groups.shipping.length > 0 && (
                                    <section aria-labelledby="shipping-voucher-group">
                                        <h3 id="shipping-voucher-group" className="mb-3 text-xs font-black uppercase tracking-wider text-on-surface">
                                            Voucher miễn phí vận chuyển
                                        </h3>
                                        <div className="space-y-3">
                                            {groups.shipping.map(voucher => (
                                                <VoucherCard
                                                    key={voucher.id}
                                                    voucher={voucher}
                                                    subtotal={subtotal}
                                                    selected={selectedCodeFor(voucher)}
                                                    disabled={disabled}
                                                    onSelectionChange={onSelectionChange}
                                                    onShowConditions={showConditions}
                                                />
                                            ))}
                                        </div>
                                    </section>
                                )}
                            </div>
                        )}
                    </div>
                )}

                <footer className="border-t border-outline-variant/30 px-4 py-4 sm:px-6">
                    <button
                        type="button"
                        onClick={conditionVoucher ? hideConditions : onClose}
                        className="min-h-11 w-full cursor-pointer rounded-xl bg-primary px-5 text-sm font-black text-on-primary transition-colors hover:bg-primary/90 focus-visible:ring-2 focus-visible:ring-primary/40 sm:ml-auto sm:block sm:w-auto"
                    >
                        {conditionVoucher ? 'Quay lại danh sách' : 'Hoàn tất'}
                    </button>
                </footer>
            </div>
        </div>
    );
}
