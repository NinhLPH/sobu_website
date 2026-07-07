import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Loader2, RefreshCw, Save, Truck } from 'lucide-react';
import { ShippingService } from '../../service/shipping.service';
import { ToastService } from '../../service/toast.service';
import {
    AdminShippingCarriersPayload,
    CarrierConfigDto,
    CarrierConfigRequestDto,
    NormalizedCarrierItem
} from '../../interface/shipping.model';

type CarrierConfigDraft = {
    carrierId: string;
    standardService: string;
    expressService: string;
    expressCarrierId: string;
    expressFallbackId: string;
};

const emptyDraft: CarrierConfigDraft = {
    carrierId: '',
    standardService: '',
    expressService: '',
    expressCarrierId: '',
    expressFallbackId: ''
};

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || error?.message || fallback;

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

const firstPrimitive = (
    record: Record<string, unknown>,
    keys: string[]
) => {
    for (const key of keys) {
        const value = record[key];
        if (typeof value === 'string' && value.trim()) {
            return value.trim();
        }
        if (typeof value === 'number' && Number.isFinite(value)) {
            return String(value);
        }
    }
    return undefined;
};

const toDraft = (config?: CarrierConfigDto | null): CarrierConfigDraft => ({
    carrierId: config?.carrierId ? String(config.carrierId) : '',
    standardService: config?.standardService ?? '',
    expressService: config?.expressService ?? '',
    expressCarrierId: config?.expressCarrierId ? String(config.expressCarrierId) : '',
    expressFallbackId: config?.expressFallbackId ? String(config.expressFallbackId) : ''
});

const hasCarrierShape = (item: NormalizedCarrierItem) =>
    Boolean(item.id || item.name || item.code || item.serviceId || item.serviceName || item.serviceCode);

const normalizeCarrierRecord = (
    record: Record<string, unknown>,
    fallbackId?: string
): NormalizedCarrierItem => ({
    id: firstPrimitive(record, ['carrierId', 'id', 'carrier_id']) ?? fallbackId,
    name: firstPrimitive(record, ['carrierName', 'name', 'carrier_name', 'title']),
    code: firstPrimitive(record, ['code', 'carrierCode', 'carrier_code']),
    serviceId: firstPrimitive(record, ['carrierServiceId', 'serviceId', 'service_id']),
    serviceName: firstPrimitive(record, ['carrierServiceName', 'serviceName', 'service_name']),
    serviceCode: firstPrimitive(record, ['service', 'serviceCode', 'service_code', 'standardService']),
    raw: record
});

const collectCarrierItems = (
    value: unknown,
    fallbackId?: string,
    depth = 0
): NormalizedCarrierItem[] => {
    if (depth > 4) {
        return [];
    }

    if (Array.isArray(value)) {
        return value.flatMap((item) => collectCarrierItems(item, undefined, depth + 1));
    }

    if (!isRecord(value)) {
        return [];
    }

    const ownItem = normalizeCarrierRecord(value, fallbackId);
    const ownItems = hasCarrierShape(ownItem) ? [ownItem] : [];

    const nestedItems = Object.entries(value).flatMap(([key, child]) => {
        const keyedId = /^\d+$/.test(key) ? key : undefined;
        return collectCarrierItems(child, keyedId, depth + 1);
    });

    return [...ownItems, ...nestedItems];
};

const normalizeCarrierItems = (payload: AdminShippingCarriersPayload): NormalizedCarrierItem[] => {
    const seen = new Set<string>();
    return collectCarrierItems(payload).filter((item) => {
        const key = [
            item.id ?? '',
            item.name ?? '',
            item.code ?? '',
            item.serviceId ?? '',
            item.serviceName ?? '',
            item.serviceCode ?? ''
        ].join('|');
        if (seen.has(key)) {
            return false;
        }
        seen.add(key);
        return true;
    });
};

const stringifyPayload = (payload: unknown) => {
    try {
        return JSON.stringify(payload, null, 2);
    } catch {
        return String(payload);
    }
};

const parseOptionalPositiveInteger = (
    value: string,
    label: string
) => {
    const trimmed = value.trim();
    if (!trimmed) {
        return undefined;
    }
    const parsed = Number(trimmed);
    if (!Number.isInteger(parsed) || parsed <= 0) {
        throw new Error(`${label} phai la so nguyen duong.`);
    }
    return parsed;
};

const buildPayload = (draft: CarrierConfigDraft): CarrierConfigRequestDto => ({
    carrierId: parseOptionalPositiveInteger(draft.carrierId, 'Carrier ID'),
    standardService: draft.standardService.trim() || undefined,
    expressService: draft.expressService.trim() || undefined,
    expressCarrierId: parseOptionalPositiveInteger(draft.expressCarrierId, 'Express carrier ID'),
    expressFallbackId: parseOptionalPositiveInteger(draft.expressFallbackId, 'Express fallback ID')
});

export default function AdminShipping() {
    const [carriersPayload, setCarriersPayload] = useState<AdminShippingCarriersPayload>(null);
    const [config, setConfig] = useState<CarrierConfigDto | null>(null);
    const [draft, setDraft] = useState<CarrierConfigDraft>(emptyDraft);
    const [isLoadingCarriers, setIsLoadingCarriers] = useState(false);
    const [isLoadingConfig, setIsLoadingConfig] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [carrierError, setCarrierError] = useState<string | null>(null);
    const [configError, setConfigError] = useState<string | null>(null);
    const [formError, setFormError] = useState<string | null>(null);

    const carrierItems = useMemo(
        () => normalizeCarrierItems(carriersPayload),
        [carriersPayload]
    );
    const isLoading = isLoadingCarriers || isLoadingConfig;

    const loadCarriers = useCallback(async () => {
        setIsLoadingCarriers(true);
        setCarrierError(null);
        try {
            const response = await ShippingService.getAdminCarriers();
            if (!response.success) {
                throw new Error(response.message || 'Could not load shipping carriers.');
            }
            setCarriersPayload(response.data);
        } catch (error) {
            setCarrierError(getErrorMessage(
                error,
                'Khong the tai danh sach don vi van chuyen tu Nhanh.'
            ));
        } finally {
            setIsLoadingCarriers(false);
        }
    }, []);

    const loadConfig = useCallback(async () => {
        setIsLoadingConfig(true);
        setConfigError(null);
        try {
            const response = await ShippingService.getCarrierConfig();
            if (!response.success) {
                throw new Error(response.message || 'Could not load carrier configuration.');
            }
            setConfig(response.data);
            setDraft(toDraft(response.data));
        } catch (error) {
            setConfigError(getErrorMessage(
                error,
                'Khong the tai cau hinh van chuyen.'
            ));
        } finally {
            setIsLoadingConfig(false);
        }
    }, []);

    const load = useCallback(async () => {
        await Promise.all([loadCarriers(), loadConfig()]);
    }, [loadCarriers, loadConfig]);

    useEffect(() => {
        void load();
    }, [load]);

    const updateDraft = (field: keyof CarrierConfigDraft, value: string) => {
        setDraft((current) => ({ ...current, [field]: value }));
        setFormError(null);
    };

    const handleSubmit = async (event: FormEvent) => {
        event.preventDefault();
        setFormError(null);

        let payload: CarrierConfigRequestDto;
        try {
            payload = buildPayload(draft);
        } catch (error: any) {
            setFormError(error.message);
            return;
        }

        setIsSaving(true);
        try {
            const response = await ShippingService.updateCarrierConfig(payload);
            if (!response.success) {
                throw new Error(response.message || 'Could not update carrier configuration.');
            }
            ToastService.success('Da luu cau hinh van chuyen.');
            await loadConfig();
        } catch (error) {
            setFormError(getErrorMessage(
                error,
                'Khong the luu cau hinh van chuyen. Hay kiem tra ket noi Nhanh.'
            ));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-black uppercase text-on-surface">Van chuyen</h1>
                    <p className="mt-1 text-xs font-semibold text-outline">
                        Quan ly don vi van chuyen Nhanh va cau hinh carrier mac dinh dung khi bao gia ship.
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => void load()}
                    disabled={isLoading || isSaving}
                    className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-outline-variant/40 bg-white px-4 py-2.5 text-xs font-black uppercase text-on-surface transition-colors hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                    Tai lai
                </button>
            </div>

            {(carrierError || configError) && (
                <div className="space-y-3">
                    {carrierError && (
                        <div className="flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                            <div>
                                <p>{carrierError}</p>
                                <Link to="/admin/sync" className="mt-1 inline-block underline">
                                    Ket noi Nhanh tai trang Dong bo ERP
                                </Link>
                            </div>
                        </div>
                    )}
                    {configError && (
                        <div className="flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                            <span>{configError}</span>
                        </div>
                    )}
                </div>
            )}

            <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px]">
                <section className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                    <div className="flex items-center justify-between gap-3 border-b border-outline-variant/20 bg-surface-container-lowest px-5 py-4">
                        <div>
                            <h2 className="text-sm font-black uppercase text-on-surface">Don vi van chuyen tu Nhanh</h2>
                            <p className="mt-1 text-xs font-semibold text-outline">
                                API carrier co shape phu thuoc Nhanh, nen UI se hien bang khi parse duoc va giu JSON raw de doi chieu.
                            </p>
                        </div>
                        <Truck className="h-5 w-5 shrink-0 text-primary" />
                    </div>

                    {isLoadingCarriers ? (
                        <div className="flex min-h-[280px] items-center justify-center">
                            <Loader2 className="h-9 w-9 animate-spin text-primary" />
                        </div>
                    ) : carrierItems.length > 0 ? (
                        <div className="overflow-x-auto">
                            <table className="min-w-full text-left text-xs">
                                <thead className="bg-surface-container text-[10px] font-black uppercase text-outline">
                                    <tr>
                                        <th className="px-4 py-3">Carrier ID</th>
                                        <th className="px-4 py-3">Ten / Code</th>
                                        <th className="px-4 py-3">Service</th>
                                        <th className="px-4 py-3">Raw</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/20">
                                    {carrierItems.map((item, index) => (
                                        <tr key={`${item.id ?? 'carrier'}-${item.serviceId ?? item.serviceCode ?? index}`}>
                                            <td className="px-4 py-3 font-black text-primary">{item.id || '-'}</td>
                                            <td className="px-4 py-3 font-bold text-on-surface">
                                                <span className="block">{item.name || '-'}</span>
                                                {item.code && <span className="text-[10px] uppercase text-outline">{item.code}</span>}
                                            </td>
                                            <td className="px-4 py-3 font-semibold text-on-surface-variant">
                                                <span className="block">{item.serviceName || '-'}</span>
                                                <span className="text-[10px] uppercase text-outline">
                                                    {[item.serviceId, item.serviceCode].filter(Boolean).join(' / ') || '-'}
                                                </span>
                                            </td>
                                            <td className="max-w-[260px] px-4 py-3">
                                                <pre className="max-h-20 overflow-auto rounded-lg bg-surface-container-lowest p-2 text-[10px] font-semibold text-outline">
                                                    {stringifyPayload(item.raw)}
                                                </pre>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <div className="p-5">
                            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs font-bold text-amber-800">
                                Chua parse duoc danh sach carrier thanh bang. Hay dung JSON raw ben duoi de copy carrier ID/service code.
                            </div>
                            <pre className="mt-4 max-h-[420px] overflow-auto rounded-xl bg-surface-container-lowest p-4 text-xs font-semibold text-on-surface-variant">
                                {stringifyPayload(carriersPayload)}
                            </pre>
                        </div>
                    )}
                </section>

                <section className="rounded-2xl border border-outline-variant/30 bg-white p-5 shadow-sm">
                    <div className="mb-5">
                        <h2 className="text-sm font-black uppercase text-on-surface">Cau hinh carrier</h2>
                        <p className="mt-1 text-xs font-semibold text-outline">
                            Cac truong nay optional theo API_doc. De trong neu muon backend dung fallback hien co.
                        </p>
                    </div>

                    {config && (
                        <div className="mb-5 flex items-start gap-3 rounded-xl border border-green-200 bg-green-50 p-3 text-xs font-bold text-green-700">
                            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
                            <div>
                                <p>Cau hinh hien tai da duoc tai.</p>
                                <p className="mt-1 font-mono text-[10px]">
                                    carrierId={config.carrierId ?? '-'} | standard={config.standardService || '-'} | express={config.expressService || '-'}
                                </p>
                            </div>
                        </div>
                    )}

                    {formError && (
                        <div className="mb-5 flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                            <span>{formError}</span>
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <label className="block text-[10px] font-black uppercase tracking-wider text-outline">
                            Carrier ID mac dinh
                            <input
                                aria-label="Carrier ID"
                                value={draft.carrierId}
                                onChange={(event) => updateDraft('carrierId', event.target.value)}
                                disabled={isLoadingConfig || isSaving}
                                className="mt-2 w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-bold normal-case text-on-surface outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
                                placeholder="22151"
                            />
                        </label>

                        <label className="block text-[10px] font-black uppercase tracking-wider text-outline">
                            Standard service code
                            <input
                                aria-label="Standard service"
                                value={draft.standardService}
                                onChange={(event) => updateDraft('standardService', event.target.value)}
                                disabled={isLoadingConfig || isSaving}
                                className="mt-2 w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-bold normal-case text-on-surface outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
                                placeholder="VCN"
                            />
                        </label>

                        <label className="block text-[10px] font-black uppercase tracking-wider text-outline">
                            Express service code
                            <input
                                aria-label="Express service"
                                value={draft.expressService}
                                onChange={(event) => updateDraft('expressService', event.target.value)}
                                disabled={isLoadingConfig || isSaving}
                                className="mt-2 w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-bold normal-case text-on-surface outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
                                placeholder="VHT"
                            />
                        </label>

                        <label className="block text-[10px] font-black uppercase tracking-wider text-outline">
                            Express carrier ID
                            <input
                                aria-label="Express carrier ID"
                                value={draft.expressCarrierId}
                                onChange={(event) => updateDraft('expressCarrierId', event.target.value)}
                                disabled={isLoadingConfig || isSaving}
                                className="mt-2 w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-bold normal-case text-on-surface outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
                                placeholder="18"
                            />
                        </label>

                        <label className="block text-[10px] font-black uppercase tracking-wider text-outline">
                            Express fallback carrier ID (khong dung)
                            <input
                                aria-label="Express fallback ID"
                                value={draft.expressFallbackId}
                                onChange={(event) => updateDraft('expressFallbackId', event.target.value)}
                                disabled={isLoadingConfig || isSaving}
                                className="mt-2 w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-bold normal-case text-on-surface outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
                                placeholder="22384"
                            />
                        </label>

                        <button
                            type="submit"
                            disabled={isSaving || isLoadingConfig}
                            className="inline-flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-xs font-black uppercase text-on-primary shadow-sm transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                            Luu cau hinh
                        </button>
                    </form>
                </section>
            </div>
        </div>
    );
}
