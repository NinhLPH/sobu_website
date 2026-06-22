import {FormEvent, useCallback, useEffect, useState} from 'react';
import {ChevronLeft, ChevronRight, Edit, FileJson, Loader2, Plus, Search, Trash2, X} from 'lucide-react';
import {AdminUiService} from '../../service/admin-ui.service';
import {WebsiteConfigurationDTO, WebsiteConfigurationMutationPayload} from '../../interface/public-ui-config.model';
import {PageResponse} from '../../interface/api-response';
import {ToastService} from '../../service/toast.service';
import {usePublicUiStore} from '../../store/usePublicUiStore';

const emptyPage: PageResponse<WebsiteConfigurationDTO> = {
    content: [], pageNumber: 1, pageSize: 10, totalElements: 0, totalPages: 0,
    first: true, last: true, hasNext: false, hasPrevious: false,
};

const emptyConfig = (): WebsiteConfigurationMutationPayload => ({
    key: '', value: '', type: 'text', groupName: '', description: '', isPublic: true,
});

const errorMessage = (error: any) => error?.response?.data?.message || error?.message || 'Thao tác cấu hình thất bại.';

export default function AdminConfigs() {
    const [pageData, setPageData] = useState(emptyPage);
    const [page, setPage] = useState(1);
    const [searchInput, setSearchInput] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [editing, setEditing] = useState<WebsiteConfigurationDTO | null>(null);
    const [form, setForm] = useState<WebsiteConfigurationMutationPayload>(emptyConfig);
    const [modalOpen, setModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const invalidateConfigs = usePublicUiStore((state) => state.invalidateConfigs);
    const fetchPublicConfigs = usePublicUiStore((state) => state.fetchConfigs);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            setPageData(await AdminUiService.searchConfigs(searchTerm, {
                page, pageSize: 10, sortBy: 'createdAt', sortDirection: 'DESC',
            }));
        } catch (loadError) {
            setError(errorMessage(loadError));
        } finally {
            setLoading(false);
        }
    }, [page, searchTerm]);

    useEffect(() => { void load(); }, [load]);

    const openCreate = () => {
        setEditing(null);
        setForm(emptyConfig());
        setError(null);
        setModalOpen(true);
    };

    const openEdit = (config: WebsiteConfigurationDTO) => {
        setEditing(config);
        setForm({
            key: config.key, value: config.value, type: config.type,
            groupName: config.groupName || '', description: config.description || '', isPublic: config.isPublic,
        });
        setError(null);
        setModalOpen(true);
    };

    const validate = () => {
        if (!form.key.trim()) return 'Key cấu hình là bắt buộc.';
        if (!form.value.trim() && form.type !== 'text') return 'Giá trị cấu hình là bắt buộc.';
        if (form.type === 'number' && !Number.isFinite(Number(form.value))) return 'Giá trị number không hợp lệ.';
        if (form.type === 'json') {
            try { JSON.parse(form.value); } catch { return 'Giá trị JSON không hợp lệ.'; }
        }
        if (form.type === 'image' && form.value && !form.value.startsWith('/') && !/^https?:\/\//i.test(form.value)) return 'URL ảnh không hợp lệ.';
        return null;
    };

    const refreshPublic = async () => {
        invalidateConfigs();
        await fetchPublicConfigs(true);
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        const validationError = validate();
        if (validationError) return setError(validationError);
        setSubmitting(true);
        setError(null);
        const payload = {...form, key: form.key.trim(), value: form.value.trim(), groupName: form.groupName?.trim() || undefined, description: form.description?.trim() || undefined};
        try {
            if (editing) await AdminUiService.updateConfig(editing.id, payload);
            else await AdminUiService.createConfig(payload);
            await Promise.all([load(), refreshPublic()]);
            ToastService.success(editing ? 'Đã cập nhật cấu hình.' : 'Đã tạo cấu hình.');
            setModalOpen(false);
        } catch (submitError) {
            setError(errorMessage(submitError));
        } finally {
            setSubmitting(false);
        }
    };

    const remove = async (config: WebsiteConfigurationDTO) => {
        if (!window.confirm(`Bạn có chắc muốn vô hiệu hóa hoặc xóa cấu hình “${config.key}”?`)) return;
        setLoading(true);
        setError(null);
        try {
            await AdminUiService.deleteConfig(config.id);
            await Promise.all([load(), refreshPublic()]);
            ToastService.success('Đã xử lý cấu hình.');
        } catch (removeError) {
            setError(errorMessage(removeError));
        } finally {
            setLoading(false);
        }
    };

    const valueInput = form.type === 'boolean_type' ? (
        <select aria-label="Giá trị" value={form.value || 'true'} onChange={(event) => setForm({...form, value: event.target.value})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"><option value="true">true</option><option value="false">false</option></select>
    ) : form.type === 'json' ? (
        <textarea aria-label="Giá trị" value={form.value} onChange={(event) => setForm({...form, value: event.target.value})} rows={6} className="w-full rounded-xl border border-outline-variant px-4 py-3 font-mono text-xs" placeholder='{"key":"value"}'/>
    ) : (
        <input aria-label="Giá trị" type={form.type === 'number' ? 'number' : form.type === 'color' ? 'color' : form.type === 'image' ? 'url' : 'text'} value={form.value} onChange={(event) => setForm({...form, value: event.target.value})} className={`w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm ${form.type === 'color' ? 'h-12' : ''}`}/>
    );

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-4"><div><h1 className="text-2xl font-black uppercase text-on-surface">Cấu hình website</h1><p className="mt-1 text-xs font-semibold text-outline">Quản lý key/value public và nội bộ.</p></div><button type="button" onClick={openCreate} className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-black uppercase text-white"><Plus className="h-4 w-4"/> Thêm cấu hình</button></div>

            <form onSubmit={(event) => { event.preventDefault(); setPage(1); setSearchTerm(searchInput.trim()); }} className="flex gap-3 rounded-2xl border border-outline-variant/30 bg-white p-4"><div className="relative flex-1"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Tìm key hoặc mô tả..." className="w-full rounded-xl bg-surface-container py-2.5 pl-10 pr-4 text-xs font-semibold outline-none"/></div><button className="rounded-xl bg-surface-container px-4 text-xs font-black">Tìm kiếm</button></form>
            {error && !modalOpen && <div className="rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">{error}</div>}

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm"><div className="overflow-x-auto"><table className="w-full min-w-[800px] text-left text-xs"><thead className="bg-surface-variant text-on-surface-variant"><tr><th className="px-5 py-4">Key</th><th className="px-5 py-4">Giá trị</th><th className="px-5 py-4">Type</th><th className="px-5 py-4">Nhóm</th><th className="px-5 py-4">Public</th><th className="px-5 py-4 text-center">Thao tác</th></tr></thead><tbody>
                {loading ? <tr><td colSpan={6} className="py-16 text-center"><Loader2 className="mx-auto h-8 w-8 animate-spin text-primary"/></td></tr> : pageData.content.map((config) => <tr key={config.id} className="border-t border-outline-variant/20"><td className="px-5 py-4 font-mono font-black text-primary">{config.key}</td><td className="max-w-sm px-5 py-4"><p className="line-clamp-2 break-all font-semibold text-on-surface">{config.value}</p><p className="mt-1 line-clamp-1 text-[10px] text-outline">{config.description || 'Không có mô tả'}</p></td><td className="px-5 py-4">{config.type}</td><td className="px-5 py-4">{config.groupName || '—'}</td><td className="px-5 py-4"><span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${config.isPublic ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{config.isPublic ? 'Public' : 'Private'}</span></td><td className="px-5 py-4"><div className="flex justify-center gap-2"><button type="button" onClick={() => openEdit(config)} aria-label={`Sửa config ${config.key}`} className="rounded-lg bg-primary/10 p-2 text-primary"><Edit className="h-4 w-4"/></button><button type="button" onClick={() => void remove(config)} aria-label={`Xóa config ${config.key}`} className="rounded-lg bg-error/10 p-2 text-error"><Trash2 className="h-4 w-4"/></button></div></td></tr>)}
                {!loading && !pageData.content.length && <tr><td colSpan={6} className="py-16 text-center font-bold text-outline"><FileJson className="mx-auto mb-2 h-9 w-9 opacity-30"/>Không có cấu hình.</td></tr>}
            </tbody></table></div>{pageData.totalPages > 1 && <div className="flex items-center justify-between border-t border-outline-variant/20 px-5 py-4"><span className="text-xs font-bold text-outline">Trang {pageData.pageNumber}/{pageData.totalPages}</span><div className="flex gap-2"><button type="button" disabled={!pageData.hasPrevious || loading} onClick={() => setPage((value) => Math.max(1, value - 1))} className="rounded-lg bg-surface-container p-2 disabled:opacity-40"><ChevronLeft className="h-4 w-4"/></button><button type="button" disabled={!pageData.hasNext || loading} onClick={() => setPage((value) => value + 1)} className="rounded-lg bg-surface-container p-2 disabled:opacity-40"><ChevronRight className="h-4 w-4"/></button></div></div>}</div>

            {modalOpen && <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-3xl bg-white shadow-xl"><div className="flex items-center justify-between border-b border-outline-variant/30 p-6"><h2 className="text-xl font-black">{editing ? 'Cập nhật cấu hình' : 'Thêm cấu hình'}</h2><button type="button" onClick={() => setModalOpen(false)} aria-label="Đóng"><X className="h-6 w-6"/></button></div><form onSubmit={submit} className="space-y-4 p-6">
                <div><label className="mb-1 block text-xs font-black uppercase text-outline">Key</label><input aria-label="Key" required value={form.key} onChange={(event) => setForm({...form, key: event.target.value})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 font-mono text-sm" placeholder="homepage.heroTitle"/></div>
                <div className="grid gap-4 sm:grid-cols-2"><div><label className="mb-1 block text-xs font-black uppercase text-outline">Type</label><select aria-label="Type" value={form.type} onChange={(event) => setForm({...form, type: event.target.value as WebsiteConfigurationMutationPayload['type'], value: event.target.value === 'boolean_type' ? 'true' : ''})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"><option value="text">text</option><option value="color">color</option><option value="image">image</option><option value="boolean_type">boolean_type</option><option value="json">json</option><option value="number">number</option></select></div><div><label className="mb-1 block text-xs font-black uppercase text-outline">Nhóm</label><input value={form.groupName || ''} onChange={(event) => setForm({...form, groupName: event.target.value})} className="w-full rounded-xl border border-outline-variant px-4 py-2.5 text-sm"/></div></div>
                <div><label className="mb-1 block text-xs font-black uppercase text-outline">Giá trị</label>{valueInput}</div>
                <div><label className="mb-1 block text-xs font-black uppercase text-outline">Mô tả</label><textarea value={form.description || ''} onChange={(event) => setForm({...form, description: event.target.value})} rows={3} className="w-full rounded-xl border border-outline-variant px-4 py-3 text-sm"/></div>
                <label className="flex items-center gap-3 text-sm font-bold"><input type="checkbox" checked={form.isPublic} onChange={(event) => setForm({...form, isPublic: event.target.checked})} className="h-4 w-4 accent-primary"/> Cho phép public API truy cập</label>
                {error && <div className="rounded-xl bg-error/10 p-3 text-xs font-bold text-error">{error}</div>}
                <div className="flex justify-end gap-3 border-t border-outline-variant/30 pt-4"><button type="button" onClick={() => setModalOpen(false)} className="rounded-xl px-5 py-2.5 text-xs font-black">Hủy</button><button disabled={submitting} className="flex items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-black text-white disabled:opacity-50">{submitting && <Loader2 className="h-4 w-4 animate-spin"/>} Lưu cấu hình</button></div>
            </form></div></div>}
        </div>
    );
}
