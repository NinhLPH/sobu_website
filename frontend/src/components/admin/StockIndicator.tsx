import {AlertTriangle, CheckCircle2, XCircle} from 'lucide-react';

export function StockIndicator({stock, threshold, showHealthyValue = true}: {
    stock: number;
    threshold: number;
    showHealthyValue?: boolean;
}) {
    if (stock <= 0) {
        return <span className="inline-flex items-center gap-1 rounded-full bg-error/10 px-2.5 py-1 text-xs font-black text-error">
            <XCircle className="h-3.5 w-3.5" aria-hidden="true"/>Hết hàng
        </span>;
    }
    if (stock <= threshold) {
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-1 text-xs font-black text-amber-800">
            <AlertTriangle className="h-3.5 w-3.5" aria-hidden="true"/>Sắp hết · {stock}
        </span>;
    }
    return showHealthyValue
        ? <span className="inline-flex items-center gap-1 font-bold text-on-surface">
            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-700" aria-hidden="true"/>{stock}
        </span>
        : <span className="font-bold text-on-surface">{stock}</span>;
}
