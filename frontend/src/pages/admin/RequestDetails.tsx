import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCircle2, Image as ImageIcon, Loader2, Save, Send, ShieldAlert } from 'lucide-react';
import ImageUploader from '../../components/common/ImageUploader';
import RequestWorkflow, { RequestStatusBadge } from '../../components/request/RequestWorkflow';
import { RequestItemDto, UpdateRequestDto } from '../../interface/customer-request.model';
import { useRequestStore } from '../../store/useRequestStore';
import { ToastService } from '../../service/toast.service';
import { formatCurrency } from '../../utils/format';
import { getPublicImageUrl } from '../../utils/file-url';
import {
    canAdminEditQuotation,
    getAdminPrimaryAction,
    REQUEST_STATUS_VIEWS
} from '../../utils/request-workflow';

const typeLabels: Record<string, string> = {
    NORMAL: 'Thông thường', CUSTOM: 'Custom theo yêu cầu', FINDING: 'Tìm đồ hộ', PREORDER: 'Đặt trước'
};

const quoteTotal = (items: RequestItemDto[]) => items.reduce(
    (sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0
);

export default function AdminRequestDetail() {
    const { id } = useParams();
    const {
        currentRequestDetail,
        getRequestDetail,
        updateRequestAction,
        processRequestAction,
        isLoading,
        isSubmitting,
        error,
        clearError,
        clearCurrentDetail
    } = useRequestStore();
    const [items, setItems] = useState<RequestItemDto[]>([]);
    const [totalAmount, setTotalAmount] = useState('');
    const [supportImages, setSupportImages] = useState<string[]>([]);
    const [uploading, setUploading] = useState(false);
    const [deposit, setDeposit] = useState('');
    const [note, setNote] = useState('');
    const [dangerAction, setDangerAction] = useState<'REJECTED' | 'CANCELLED'>('REJECTED');
    const [dangerReason, setDangerReason] = useState('');
    const [localError, setLocalError] = useState<string | null>(null);

    useEffect(() => {
        if (id) void getRequestDetail(id, 'admin');
        return () => clearCurrentDetail();
    }, [id, getRequestDetail, clearCurrentDetail]);

    useEffect(() => {
        if (!currentRequestDetail || String(currentRequestDetail.id) !== id) return;
        setItems(currentRequestDetail.items.map((item) => ({
            nhanhProductId: item.nhanhProductId,
            name: item.name,
            note: item.note,
            metadataJson: item.metadataJson,
            price: item.price,
            quantity: item.quantity
        })));
        setTotalAmount(String(currentRequestDetail.totalAmount || quoteTotal(currentRequestDetail.items)) || '');
        setDeposit(currentRequestDetail.depositAmount > 0 ? String(currentRequestDetail.depositAmount) : '');
        setSupportImages([]);
        setDangerAction(currentRequestDetail.status === 'APPROVED' ? 'CANCELLED' : 'REJECTED');
        setLocalError(null);
    }, [currentRequestDetail, id]);

    const request = currentRequestDetail && String(currentRequestDetail.id) === id ? currentRequestDetail : null;
    const primaryAction = request ? getAdminPrimaryAction(request.type, request.status) : null;
    const quotationEditable = request ? canAdminEditQuotation(request.type, request.status) : false;
    const baseImageUrls = useMemo(() => request?.attachments.map((attachment) => attachment.url) || [], [request]);

    const updateItem = (index: number, field: keyof RequestItemDto, value: string | number) => {
        setItems((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item));
        if (field === 'price' || field === 'quantity') {
            const next = items.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item);
            setTotalAmount(String(quoteTotal(next)));
        }
        setLocalError(null);
    };

    const buildQuotationPayload = (): UpdateRequestDto | null => {
        if (!request || items.length === 0 || items.some((item) => !item.name.trim() || Number(item.quantity) < 1 || Number(item.price) < 0)) {
            setLocalError('Vui lòng nhập đầy đủ tên, số lượng và đơn giá hợp lệ.');
            return null;
        }
        const total = Number(totalAmount);
        if (!Number.isFinite(total) || total < 0) {
            setLocalError('Tổng tiền báo giá không hợp lệ.');
            return null;
        }
        return {
            items: items.map((item) => ({ ...item, name: item.name.trim(), price: Number(item.price), quantity: Number(item.quantity) })),
            totalAmount: total,
            customRequirements: request.customRequirements || '',
            uploadedImageUrls: Array.from(new Set([...baseImageUrls, ...supportImages]))
        };
    };

    const saveQuotation = async (sendAfterSave = false) => {
        if (!request) return;
        clearError();
        setLocalError(null);
        const payload = buildQuotationPayload();
        if (!payload) return;
        try {
            await updateRequestAction(request.id, payload, 'admin');
            if (sendAfterSave) {
                await processRequestAction(request.id, { targetStatus: 'WAITING_CUSTOMER' });
                ToastService.success('Đã lưu và gửi báo giá cho khách hàng.');
            } else {
                ToastService.success('Đã lưu nháp báo giá.');
            }
        } catch (caught: any) {
            setLocalError(caught?.message || 'Không thể lưu báo giá.');
        }
    };

    const runPrimaryAction = async (event?: FormEvent) => {
        event?.preventDefault();
        if (!request || !primaryAction) return;
        if (primaryAction.kind === 'SEND_QUOTE') {
            await saveQuotation(true);
            return;
        }
        let depositAmount: number | undefined;
        if (primaryAction.requiresDeposit) {
            if (!deposit.trim()) {
                setLocalError('Vui lòng nhập số tiền cọc trước khi duyệt.');
                return;
            }
            depositAmount = Number(deposit);
            if (!Number.isFinite(depositAmount) || depositAmount < 0 || depositAmount > request.totalAmount) {
                setLocalError('Tiền cọc phải từ 0 đến tổng tiền báo giá.');
                return;
            }
        }
        try {
            await processRequestAction(request.id, {
                targetStatus: primaryAction.targetStatus,
                note: note.trim() || undefined,
                depositAmount
            });
            ToastService.success(primaryAction.kind === 'APPROVE' ? 'Đã duyệt yêu cầu và tạo đơn hàng.' : 'Đã cập nhật bước xử lý.');
        } catch (caught: any) {
            setLocalError(caught?.message || 'Không thể xử lý yêu cầu.');
        }
    };

    const submitDangerAction = async (event: FormEvent) => {
        event.preventDefault();
        if (!request || !dangerReason.trim()) {
            setLocalError('Vui lòng nhập lý do từ chối hoặc hủy yêu cầu.');
            return;
        }
        try {
            await processRequestAction(request.id, { targetStatus: dangerAction, note: dangerReason.trim() });
            setDangerReason('');
            ToastService.success(dangerAction === 'REJECTED' ? 'Đã từ chối yêu cầu.' : 'Đã hủy yêu cầu.');
        } catch (caught: any) {
            setLocalError(caught?.message || 'Không thể cập nhật yêu cầu.');
        }
    };

    if (isLoading || !request) {
        return (
            <div className="flex min-h-[50vh] flex-col items-center justify-center">
                {error ? <><AlertTriangle className="mb-3 h-10 w-10 text-error" /><p className="text-sm font-bold text-error">{error}</p></> : <><Loader2 className="mb-3 h-8 w-8 animate-spin text-primary" /><p className="text-xs font-bold text-outline">Đang tải chi tiết yêu cầu...</p></>}
            </div>
        );
    }

    const statusView = REQUEST_STATUS_VIEWS[request.status];

    return (
        <main className="space-y-6 pb-12 pt-6">
            <Link to="/admin/requests" className="inline-flex items-center gap-2 text-xs font-black uppercase text-outline hover:text-primary"><ArrowLeft className="h-4 w-4" /> Danh sách yêu cầu</Link>

            <header className="rounded-3xl border border-outline-variant/30 bg-white p-5 shadow-sm sm:p-7">
                <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
                    <div>
                        <div className="flex flex-wrap items-center gap-3">
                            <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface">#{request.requestCode || request.id}</h1>
                            <RequestStatusBadge status={request.status} />
                        </div>
                        <p className="mt-2 text-sm font-bold text-on-surface">{typeLabels[request.type]} · {request.customerPhone}</p>
                        <p className="mt-2 text-xs font-semibold text-outline">{statusView.description}</p>
                    </div>
                    <div className="rounded-2xl bg-surface-container px-5 py-3 text-right">
                        <p className="text-[9px] font-black uppercase text-outline">Tổng báo giá</p>
                        <p className="mt-1 text-xl font-black text-primary">{request.totalAmount > 0 ? formatCurrency(request.totalAmount) : 'Chưa có'}</p>
                        <p className="mt-1 text-[10px] font-bold text-outline">Tiền cọc: {request.depositAmount > 0 ? formatCurrency(request.depositAmount) : 'Chưa thiết lập'}</p>
                    </div>
                </div>
                <div className="mt-6"><RequestWorkflow type={request.type} status={request.status} /></div>
            </header>

            {(localError || error) && <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">{localError || error}</div>}

            <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
                <div className="space-y-6">
                    <section className="rounded-3xl border border-outline-variant/30 bg-white p-5 shadow-sm sm:p-6">
                        <h2 className="text-sm font-black uppercase tracking-wide text-on-surface">Nội dung khách hàng gửi</h2>
                        <div className="mt-5 rounded-2xl bg-surface-container p-4 text-sm font-semibold leading-relaxed text-on-surface">
                            {request.customRequirements || 'Không có mô tả bổ sung.'}
                        </div>
                        <div className="mt-5 grid gap-3 sm:grid-cols-2">
                            {request.attachments.map((attachment) => (
                                <a key={attachment.id} href={getPublicImageUrl(attachment.url)} target="_blank" rel="noreferrer" className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface-container">
                                    <img src={getPublicImageUrl(attachment.url)} alt="Ảnh khách hàng gửi" className="h-40 w-full object-cover" />
                                </a>
                            ))}
                            {request.attachments.length === 0 && <p className="col-span-full flex items-center gap-2 text-xs font-bold text-outline"><ImageIcon className="h-4 w-4" /> Không có hình ảnh đính kèm.</p>}
                        </div>
                    </section>

                    <section className="rounded-3xl border border-outline-variant/30 bg-white p-5 shadow-sm sm:p-6">
                        <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
                            <div>
                                <h2 className="text-sm font-black uppercase tracking-wide text-on-surface">Sản phẩm & báo giá</h2>
                                <p className="mt-1 text-[11px] font-semibold text-outline">Thông tin khách gửi được giữ nguyên; Admin chỉ cập nhật báo giá ở đúng bước.</p>
                            </div>
                            {quotationEditable && <span className="rounded-full bg-primary/10 px-3 py-1 text-[9px] font-black uppercase text-primary">Đang chỉnh báo giá</span>}
                        </div>
                        <div className="mt-5 space-y-3">
                            {items.map((item, index) => (
                                <div key={index} className="grid gap-3 rounded-2xl border border-outline-variant/30 p-4 lg:grid-cols-[minmax(0,1fr)_110px_170px]">
                                    <div>
                                        <p className="text-xs font-black text-on-surface">{item.name}</p>
                                        <input aria-label={`Ghi chú sản phẩm ${index + 1}`} value={item.note || ''} onChange={(event) => updateItem(index, 'note', event.target.value)} disabled={!quotationEditable} className="mt-2 w-full rounded-lg border-0 bg-surface-container px-3 py-2 text-xs disabled:opacity-70" placeholder="Thông tin nguồn hàng / ghi chú" />
                                    </div>
                                    <label className="text-[10px] font-black uppercase text-outline">Số lượng
                                        <input aria-label={`Số lượng ${index + 1}`} type="number" min="1" value={item.quantity} onChange={(event) => updateItem(index, 'quantity', Number(event.target.value))} disabled={!quotationEditable} className="mt-1 w-full rounded-lg border-0 bg-surface-container px-3 py-2 text-xs text-on-surface" />
                                    </label>
                                    <label className="text-[10px] font-black uppercase text-outline">Đơn giá (VND)
                                        <input aria-label="Đơn giá (VND)" type="number" min="0" value={item.price ?? ''} onChange={(event) => updateItem(index, 'price', Number(event.target.value))} disabled={!quotationEditable} className="mt-1 w-full rounded-lg border-0 bg-surface-container px-3 py-2 text-xs font-black text-on-surface" />
                                    </label>
                                </div>
                            ))}
                        </div>

                        {quotationEditable && (
                            <div className="mt-5 space-y-4 border-t border-surface-container pt-5">
                                <label className="block text-[10px] font-black uppercase text-outline">Tổng tiền (VND)
                                    <input aria-label="Tổng tiền (VND)" type="number" min="0" value={totalAmount} onChange={(event) => setTotalAmount(event.target.value)} className="mt-1 w-full rounded-xl border border-outline-variant/30 px-4 py-3 text-sm font-black text-primary" />
                                </label>
                                <div>
                                    <p className="mb-2 text-[10px] font-black uppercase text-outline">Ảnh Admin bổ sung</p>
                                    <ImageUploader uploadedUrls={supportImages} onChange={setSupportImages} disabled={isSubmitting} subDirectory="requests" onUploadingChange={setUploading} />
                                    <p className="mt-2 text-[10px] font-semibold text-outline">Ảnh mới được nối thêm; ảnh gốc của khách không bị xóa.</p>
                                </div>
                                <div className="grid gap-3 sm:grid-cols-2">
                                    <button type="button" onClick={() => void saveQuotation(false)} disabled={isSubmitting || uploading} className="inline-flex items-center justify-center gap-2 rounded-xl bg-surface-container px-5 py-3 text-xs font-black uppercase text-on-surface disabled:opacity-50"><Save className="h-4 w-4" /> Lưu nháp báo giá</button>
                                    {primaryAction?.kind === 'SEND_QUOTE' && <button type="button" onClick={() => void saveQuotation(true)} disabled={isSubmitting || uploading} className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-xs font-black uppercase text-white disabled:opacity-50"><Send className="h-4 w-4" /> {primaryAction.label}</button>}
                                </div>
                            </div>
                        )}
                    </section>
                </div>

                <aside className="space-y-5">
                    {primaryAction && primaryAction.kind !== 'SEND_QUOTE' && (
                        <form onSubmit={(event) => void runPrimaryAction(event)} className="rounded-3xl border border-primary/20 bg-primary/5 p-5 shadow-sm">
                            <h2 className="flex items-center gap-2 text-sm font-black uppercase text-on-surface"><CheckCircle2 className="h-5 w-5 text-primary" /> Hành động tiếp theo</h2>
                            <p className="mt-2 text-xs font-semibold leading-relaxed text-outline">{statusView.description}</p>
                            {primaryAction.requiresDeposit && (
                                <label className="mt-5 block text-[10px] font-black uppercase text-outline">Số tiền cọc (VND)
                                    <input aria-label="Số tiền cọc (VND)" type="number" min="0" max={request.totalAmount} value={deposit} onChange={(event) => setDeposit(event.target.value)} className="mt-1 w-full rounded-xl border border-primary/20 bg-white px-4 py-3 text-sm font-black text-on-surface" />
                                </label>
                            )}
                            <label className="mt-4 block text-[10px] font-black uppercase text-outline">Ghi chú xử lý
                                <textarea value={note} onChange={(event) => setNote(event.target.value)} rows={3} className="mt-1 w-full resize-none rounded-xl border border-primary/20 bg-white px-4 py-3 text-xs" />
                            </label>
                            <button type="submit" disabled={isSubmitting} className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-xs font-black uppercase text-white disabled:opacity-50">
                                {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />} {primaryAction.label}
                            </button>
                        </form>
                    )}

                    <section className="rounded-3xl border border-outline-variant/30 bg-white p-5 shadow-sm">
                        <h2 className="text-xs font-black uppercase text-on-surface">Thông tin hệ thống</h2>
                        <dl className="mt-4 space-y-3 text-xs">
                            <div className="flex justify-between gap-3"><dt className="text-outline">Ngày tạo</dt><dd className="text-right font-bold">{new Date(request.createdAt).toLocaleString('vi-VN')}</dd></div>
                            <div className="flex justify-between gap-3"><dt className="text-outline">Cập nhật</dt><dd className="text-right font-bold">{new Date(request.updatedAt).toLocaleString('vi-VN')}</dd></div>
                            <div className="flex justify-between gap-3"><dt className="text-outline">Mã đơn hàng</dt><dd className="text-right font-bold text-primary">{request.nhanhOrderCode || request.nhanhOrderId || 'Chưa tạo'}</dd></div>
                        </dl>
                    </section>

                    {(request.status !== 'REJECTED' && request.status !== 'CANCELLED') && (
                        <form onSubmit={(event) => void submitDangerAction(event)} className="rounded-3xl border border-red-200 bg-red-50/50 p-5">
                            <h2 className="flex items-center gap-2 text-xs font-black uppercase text-red-800"><ShieldAlert className="h-4 w-4" /> Xử lý ngoại lệ</h2>
                            <select value={dangerAction} onChange={(event) => setDangerAction(event.target.value as 'REJECTED' | 'CANCELLED')} className="mt-4 w-full rounded-xl border border-red-200 bg-white px-3 py-2.5 text-xs font-bold">
                                {request.status !== 'APPROVED' && <option value="REJECTED">Từ chối yêu cầu</option>}
                                <option value="CANCELLED">Hủy yêu cầu</option>
                            </select>
                            <textarea aria-label="Lý do xử lý ngoại lệ" value={dangerReason} onChange={(event) => setDangerReason(event.target.value)} rows={3} className="mt-3 w-full resize-none rounded-xl border border-red-200 bg-white px-3 py-2.5 text-xs" placeholder="Nhập lý do bắt buộc..." />
                            <button type="submit" disabled={isSubmitting} className="mt-3 w-full rounded-xl border border-red-300 bg-white px-4 py-2.5 text-[10px] font-black uppercase text-red-700 disabled:opacity-50">Xác nhận xử lý</button>
                        </form>
                    )}
                </aside>
            </div>
        </main>
    );
}
