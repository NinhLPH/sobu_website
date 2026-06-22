import {FormEvent, useCallback, useEffect, useState} from 'react';
import {ChevronLeft, ChevronRight, Edit, Image as ImageIcon, Loader2, Plus, Search, Trash2, X} from 'lucide-react';
import {AdminUiService, buildBannerFormData} from '../../service/admin-ui.service';
import {BannerDTO, BannerMutationPayload} from '../../interface/public-ui-config.model';
import {PageResponse} from '../../interface/api-response';
import {getPublicImageUrl} from '../../utils/file-url';
import {ToastService} from '../../service/toast.service';
import {usePublicUiStore} from '../../store/usePublicUiStore';

const emptyPage: PageResponse<BannerDTO> = {
    content: [], pageNumber: 1, pageSize: 10, totalElements: 0, totalPages: 0,
    first: true, last: true, hasNext: false, hasPrevious: false,
};

const emptyBanner = (): BannerMutationPayload => ({
    title: '', imageUrl: '', linkUrl: '', displayOrder: 0,
    position: 'HOME_TOP', isActive: true, startDate: '', endDate: '', deviceType: 'ALL',
});

const toFormValue = (banner: BannerDTO): BannerMutationPayload => ({
    title: banner.title,
    imageUrl: banner.imageUrl,
    linkUrl: banner.linkUrl || '',
    displayOrder: banner.displayOrder ?? 0,
    position: banner.position,
    isActive: banner.isActive,
    startDate: banner.startDate?.slice(0, 16) || '',
    endDate: banner.endDate?.slice(0, 16) || '',
    deviceType: banner.deviceType,
});

const isSupportedUrl = (value: string) => !value || value.startsWith('/') || /^https?:\/\//i.test(value);
const errorMessage = (error: any) => error?.response?.data?.message || error?.message || 'Thao tác banner thất bại.';

export default function AdminBanners() {
    const [pageData, setPageData] = useState(emptyPage);
    const [page, setPage] = useState(1);
    const [searchInput, setSearchInput] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [editing, setEditing] = useState<BannerDTO | null>(null);
    const [form, setForm] = useState<BannerMutationPayload>(emptyBanner);
    const [image, setImage] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState('');
    const [modalOpen, setModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const invalidateBanners = usePublicUiStore((state) => state.invalidateBanners);
    const fetchPublicBanners = usePublicUiStore((state) => state.fetchBanners);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            setPageData(await AdminUiService.searchBanners(searchTerm, {
                page, pageSize: 10, sortBy: 'displayOrder', sortDirection: 'ASC',
            }));
        } catch (loadError) {
            setError(errorMessage(loadError));
        } finally {
            setLoading(false);
        }
    }, [page, searchTerm]);

    useEffect(() => { void load(); }, [load]);
    useEffect(() => {
        if (!image) {
            setPreviewUrl('');
            return;
        }
        const objectUrl = URL.createObjectURL(image);
        setPreviewUrl(objectUrl);
        return () => URL.revokeObjectURL(objectUrl);
    }, [image]);

    const openCreate = () => {
        setEditing(null);
        setForm(emptyBanner());
        setImage(null);
        setError(null);
        setModalOpen(true);
    };

    const openEdit = (banner: BannerDTO) => {
        setEditing(banner);
        setForm(toFormValue(banner));
        setImage(null);
        setError(null);
        setModalOpen(true);
    };

    const validate = () => {
        if (!form.title.trim()) return 'Tiêu đề banner là bắt buộc.';
        if (!editing && !image && !form.imageUrl.trim()) return 'Hãy chọn ảnh hoặc nhập URL ảnh.';
        if (!isSupportedUrl(form.imageUrl.trim())) return 'URL ảnh phải bắt đầu bằng /, http:// hoặc https://.';
        if (!isSupportedUrl(form.linkUrl?.trim() || '')) return 'Link đích không hợp lệ.';
        if (form.displayOrder < 0) return 'Thứ tự hiển thị không được âm.';
        if (form.startDate && form.endDate && form.endDate < form.startDate) return 'Ngày kết thúc phải sau ngày bắt đầu.';
        return null;
    };

    const refreshPublic = async () => {
        invalidateBanners();
        await fetchPublicBanners(true);
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        const validationError = validate();
        if (validationError) return setError(validationError);
        setSubmitting(true);
        setError(null);
        const payload: BannerMutationPayload = {
            ...form,
            title: form.title.trim(),
            imageUrl: form.imageUrl.trim(),
            linkUrl: form.linkUrl?.trim() || undefined,
            startDate: form.startDate || undefined,
            endDate: form.endDate || undefined,
        };
        try {
            const formData = buildBannerFormData(payload, image);
            if (editing) await AdminUiService.updateBanner(editing.id, formData);
            else await AdminUiService.createBanner(formData);
            await Promise.all([load(), refreshPublic()]);
            ToastService.success(editing ? 'Đã cập nhật banner.' : 'Đã tạo banner.');
            setModalOpen(false);
        } catch (submitError) {
            setError(errorMessage(submitError));
        } finally {
            setSubmitting(false);
        }
    };

    const remove = async (banner: BannerDTO) => {
        const action = banner.isActive ? 'vô hiệu hóa' : 'xóa vĩnh viễn';
        if (!window.confirm(`Bạn có chắc muốn ${action} banner “${banner.title}”?`)) return;
        setLoading(true);
        setError(null);
        try {
            await AdminUiService.deleteBanner(banner.id);
            await Promise.all([load(), refreshPublic()]);
            ToastService.success(`Đã ${action} banner.`);
        } catch (removeError) {
            setError(errorMessage(removeError));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div><h1 className="text-2xl font-black uppercase text-on-surface">Quản lý banner</h1><p className="mt-1 text-xs font-semibold text-outline">Hero, quảng cáo giữa trang và hai sidebar.</p></div>
                <button type="button" onClick={openCreate} className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-black uppercase text-white"><Plus className="h-4 w-4"/> Thêm banner</button>
            </div>

            <form onSubmit={(event) => { event.preventDefault(); setPage(1); setSearchTerm(searchInput.trim()); }} className="flex gap-3 rounded-2xl border border-outline-variant/30 bg-white p-4">
                <div className="relative flex-1"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Tìm theo tiêu đề banner..." className="w-full rounded-xl bg-surface-container py-2.5 pl-10 pr-4 text-xs font-semibold outline-none"/></div>
                <button className="rounded-xl bg-surface-container px-4 text-xs font-black">Tìm kiếm</button>
            </form>

            {error && !modalOpen && <div className="rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">{error}</div>}

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full min-w-[900px] text-left text-xs">
                        <thead className="bg-surface-variant text-on-surface-variant"><tr><th className="px-5 py-4">Ảnh</th><th className="px-5 py-4">Tiêu đề</th><th className="px-5 py-4">Vị trí</th><th className="px-5 py-4">Thiết bị</th><th className="px-5 py-4">Thứ tự</th><th className="px-5 py-4">Hiệu lực</th><th className="px-5 py-4 text-center">Thao tác</th></tr></thead>
                        <tbody>
                            {loading ? <tr><td colSpan={7} className="py-16 text-center"><Loader2 className="mx-auto h-8 w-8 animate-spin text-primary"/></td></tr> : pageData.content.map((banner) => (
                                <tr key={banner.id} className="border-t border-outline-variant/20">
                                    <td className="px-5 py-3"><img src={getPublicImageUrl(banner.imageUrl)} alt={banner.title} className="h-14 w-24 rounded-lg bg-surface-container object-cover"/></td>
                                    <td className="max-w-xs px-5 py-3"><p className="truncate font-black text-on-surface">{banner.title}</p><p className="mt-1 truncate text-[10px] text-outline">{banner.linkUrl || 'Không có link'}</p></td>
                                    <td className="px-5 py-3 font-bold">{banner.position}</td><td className="px-5 py-3">{banner.deviceType}</td><td className="px-5 py-3">{banner.displayOrder ?? 0}</td>
                                    <td className="px-5 py-3"><span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${banner.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{banner.isActive ? 'Hoạt động' : 'Không hoạt động'}</span><p className="mt-2 text-[10px] text-outline">{banner.startDate ? new Date(banner.startDate).toLocaleDateString('vi-VN') : 'Ngay lập tức'} → {banner.endDate ? new Date(banner.endDate).toLocaleDateString('vi-VN') : 'Không giới hạn'}</p></td>
                                    <td className="px-5 py-3"><div className="flex justify-center gap-2"><button type="button" onClick={() => openEdit(banner)} aria-label={`Sửa banner ${banner.title}`} className="rounded-lg bg-primary/10 p-2 text-primary"><Edit className="h-4 w-4"/></button><button type="button" onClick={() => void remove(banner)} aria-label={`${banner.isActive ? 'Vô hiệu hóa' : 'Xóa'} banner ${banner.title}`} className="rounded-lg bg-error/10 p-2 text-error"><Trash2 className="h-4 w-4"/></button></div></td>
                                </tr>
                            ))}
                            {!loading && !pageData.content.length && <tr><td colSpan={7} className="py-16 text-center font-bold text-outline"><ImageIcon className="mx-auto mb-2 h-9 w-9 opacity-30"/>Không có banner.</td></tr>}
                        </tbody>
                    </table>
                </div>
                {pageData.totalPages > 1 && <div className="flex items-center justify-between border-t border-outline-variant/20 px-5 py-4"><span className="text-xs font-bold text-outline">Trang {pageData.pageNumber}/{pageData.totalPages}</span><div className="flex gap-2"><button type="button" disabled={!pageData.hasPrevious || loading} onClick={() => setPage((value) => Math.max(1, value - 1))} className="rounded-lg bg-surface-container p-2 disabled:opacity-40"><ChevronLeft className="h-4 w-4"/></button><button type="button" disabled={!pageData.hasNext || loading} onClick={() => setPage((value) => value + 1)} className="rounded-lg bg-surface-container p-2 disabled:opacity-40"><ChevronRight className="h-4 w-4"/></button></div></div>}
            </div>

            {modalOpen && <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white shadow-xl">
                <div className="flex items-center justify-between border-b border-outline-variant/30 p-6"><h2 className="text-xl font-black">{editing ? 'Cập nhật banner' : 'Thêm banner'}</h2><button type="button" onClick={() => setModalOpen(false)} aria-label="Đóng"><X className="h-6 w-6"/></button></div>
                <form onSubmit={submit} className="space-y-4 p-6">
                    <div><label className="mb-1 block text-xs font-black uppercase text-outline">Tiêu đề</label><input aria-label="Tiêu đề" required value={form.title} onChange={(event) => setForm({...form, title: event.target.value})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"/></div>
                    <div className="grid gap-4 sm:grid-cols-2"><div><label className="mb-1 block text-xs font-black uppercase text-outline">Vị trí</label><select value={form.position} onChange={(event) => setForm({...form, position: event.target.value as BannerMutationPayload['position']})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"><option value="HOME_TOP">HOME_TOP</option><option value="HOME_MIDDLE">HOME_MIDDLE</option><option value="PRODUCT_SIDEBAR">PRODUCT_SIDEBAR</option></select></div><div><label className="mb-1 block text-xs font-black uppercase text-outline">Thiết bị</label><select value={form.deviceType} onChange={(event) => setForm({...form, deviceType: event.target.value as BannerMutationPayload['deviceType']})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"><option value="ALL">ALL</option><option value="WEB">WEB</option><option value="MOBILE">MOBILE</option></select></div></div>
                    <div className="grid gap-4 sm:grid-cols-2"><div><label className="mb-1 block text-xs font-black uppercase text-outline">Ảnh tải lên</label><input type="file" accept="image/*" onChange={(event) => setImage(event.target.files?.[0] || null)} className="w-full rounded-xl border border-outline-variant px-3 py-2 text-xs"/></div><div><label className="mb-1 block text-xs font-black uppercase text-outline">Hoặc URL ảnh</label><input value={form.imageUrl} onChange={(event) => setForm({...form, imageUrl: event.target.value})} placeholder="/api/public/files/banners/..." className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"/></div></div>
                    {(previewUrl || form.imageUrl) && <div className="rounded-xl bg-surface-container p-2"><img src={previewUrl || getPublicImageUrl(form.imageUrl)} alt="Xem trước banner" className="h-36 w-full rounded-lg object-cover"/></div>}
                    <div><label className="mb-1 block text-xs font-black uppercase text-outline">Link đích</label><input value={form.linkUrl || ''} onChange={(event) => setForm({...form, linkUrl: event.target.value})} placeholder="/products hoặc https://..." className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"/></div>
                    <div className="grid gap-4 sm:grid-cols-3"><div><label className="mb-1 block text-xs font-black uppercase text-outline">Thứ tự</label><input type="number" min={0} value={form.displayOrder} onChange={(event) => setForm({...form, displayOrder: Number(event.target.value)})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"/></div><div><label className="mb-1 block text-xs font-black uppercase text-outline">Bắt đầu</label><input type="datetime-local" value={form.startDate || ''} onChange={(event) => setForm({...form, startDate: event.target.value})} className="w-full rounded-xl border border-outline-variant px-3 py-2.5 text-sm"/></div><div><label className="mb-1 block text-xs font-black uppercase text-outline">Kết thúc</label><input type="datetime-local" value={form.endDate || ''} onChange={(event) => setForm({...form, endDate: event.target.value})} className="w-full rounded-xl border border-outline-variant px-3 py-2.5 text-sm"/></div></div>
                    <label className="flex items-center gap-3 text-sm font-bold"><input type="checkbox" checked={form.isActive} onChange={(event) => setForm({...form, isActive: event.target.checked})} className="h-4 w-4 accent-primary"/> Hoạt động</label>
                    {error && <div className="rounded-xl bg-error/10 p-3 text-xs font-bold text-error">{error}</div>}
                    <div className="flex justify-end gap-3 border-t border-outline-variant/30 pt-4"><button type="button" onClick={() => setModalOpen(false)} className="rounded-xl px-5 py-2.5 text-xs font-black">Hủy</button><button disabled={submitting} className="flex items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-black text-white disabled:opacity-50">{submitting && <Loader2 className="h-4 w-4 animate-spin"/>} Lưu banner</button></div>
                </form>
            </div></div>}
        </div>
    );
}
