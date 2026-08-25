import {FormEvent, useCallback, useEffect, useMemo, useState} from 'react';
import {Edit3, Plus, Power, Trash2} from 'lucide-react';
import {ProductBadge, ProductBadgeWriteRequest} from '../../interface/admin-catalog.model';
import {AdminCatalogService} from '../../service/admin-catalog.service';
import {ToastService} from '../../service/toast.service';
import {
    AdminButton,
    AdminCard,
    AdminEmpty,
    AdminError,
    AdminLoading,
    AdminModal,
    AdminPage,
    AdminSearch,
    AdminStatus,
    AdminToolbar,
    Field,
    getApiError,
    inputClass
} from '../../components/admin/AdminUi';
import {useConfirmDialog} from '../../components/common/ConfirmDialog';

const initial: ProductBadgeWriteRequest = {name: '', color: '#00618e', textColor: '#ffffff', status: 1};
export default function AdminBadges() {
    const confirm = useConfirmDialog();
    const [items, setItems] = useState<ProductBadge[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [id, setId] = useState<number | null>(null);
    const [form, setForm] = useState(initial);
    const [saving, setSaving] = useState(false);
    const [query, setQuery] = useState('');
    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setItems(await AdminCatalogService.getBadges());
        } catch (e) {
            setError(getApiError(e));
        } finally {
            setLoading(false);
        }
    }, []);
    useEffect(() => {
        void load();
    }, [load]);
    const visible = useMemo(() => items.filter(item => item.name.toLowerCase().includes(query.trim().toLowerCase())), [items, query]);
    const edit = (x?: ProductBadge) => {
        setId(x?.id ?? null);
        setForm(x ? {name: x.name, color: x.color, textColor: x.textColor, status: x.status} : initial);
        setOpen(true);
    };
    const submit = async (e: FormEvent) => {
        e.preventDefault();
        if (form.name.trim().toUpperCase() === 'SALE') {
            ToastService.error('SALE là tag hệ thống, được tự động tạo từ giá sản phẩm.');
            return;
        }
        setSaving(true);
        try {
            id ? await AdminCatalogService.updateBadge(id, form) : await AdminCatalogService.createBadge(form);
            ToastService.success(id ? 'Đã cập nhật tag.' : 'Đã tạo tag sản phẩm.');
            setOpen(false);
            await load();
        } catch (err) {
            ToastService.error(getApiError(err));
        } finally {
            setSaving(false);
        }
    };
    const remove = async (x: ProductBadge) => {
        if (!await confirm({title: 'Xóa tag sản phẩm?', message: `Tag “${x.name}” sẽ bị xóa khỏi hệ thống.`, confirmLabel: 'Xóa tag', tone: 'danger'})) return;
        try {
            await AdminCatalogService.deleteBadge(x.id);
            ToastService.success('Đã xóa tag.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e, 'Không thể xóa tag đang được gắn cho sản phẩm.'));
        }
    };
    const toggle = async (x: ProductBadge) => {
        try {
            await AdminCatalogService.setBadgeStatus(x.id, x.status === 1 ? 0 : 1);
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    return <AdminPage title="Tag sản phẩm"
                      description="Thiết kế tag thủ công HOT, NEW hoặc tùy chỉnh. SALE là tag hệ thống, tự sinh từ giá."
                      actions={<AdminButton onClick={() => edit()}><Plus className="h-4 w-4"/>Thêm
                          tag</AdminButton>}><AdminCard><AdminToolbar><AdminSearch value={query} onChange={setQuery}
                                                                                          placeholder="Tìm tag sản phẩm"/></AdminToolbar>{loading ? <AdminLoading/> : error ?
        <AdminError message={error} onRetry={() => void load()}/> : !visible.length ?
            <AdminEmpty title="Chưa có tag sản phẩm"/> :
            <div className="grid gap-4 p-4 sm:grid-cols-2 xl:grid-cols-3">{visible.map(x => <article key={x.id}
                                                                                                   className="rounded-xl border border-outline-variant/35 p-4">
                <div className="flex items-start justify-between gap-3"><span
                    className="inline-flex rounded-full px-3 py-1.5 text-xs font-black uppercase tracking-wide shadow-sm"
                    style={{backgroundColor: x.color, color: x.textColor}}>{x.name}</span><AdminStatus
                    active={x.status === 1}/></div>
                <div className="mt-5 flex items-center justify-between">
                    <div className="text-xs text-outline"><p>Nền: {x.color}</p><p>Chữ: {x.textColor}</p></div>
                    <div className="flex">
                        <button onClick={() => edit(x)} className="rounded-lg p-2 text-outline hover:text-primary">
                            <Edit3 className="h-4 w-4"/></button>
                        <button onClick={() => void toggle(x)}
                                className="rounded-lg p-2 text-outline hover:text-primary"><Power className="h-4 w-4"/>
                        </button>
                        <button onClick={() => void remove(x)} className="rounded-lg p-2 text-outline hover:text-error">
                            <Trash2 className="h-4 w-4"/></button>
                    </div>
                </div>
            </article>)}</div>}</AdminCard>
        <AdminModal open={open} onClose={() => setOpen(false)} title={id ? 'Cập nhật tag' : 'Tạo tag sản phẩm'}
                    size="md">
            <form onSubmit={submit} className="space-y-4 p-5"><Field label="Tên tag"><input required
                                                                                            className={inputClass}
                                                                                            value={form.name}
                                                                                            placeholder="HOT, NEW hoặc tên tùy chỉnh"
                                                                                            onChange={e => setForm({
                                                                                                ...form,
                                                                                                name: e.target.value
                                                                                            })}/></Field>
                <div className="grid grid-cols-2 gap-4"><Field label="Màu nền">
                    <div className="flex gap-2"><input type="color" className="h-10 w-12 rounded border"
                                                       value={form.color}
                                                       onChange={e => setForm({...form, color: e.target.value})}/><input
                        required pattern="^#[0-9A-Fa-f]{6}$" className={inputClass} value={form.color}
                        onChange={e => setForm({...form, color: e.target.value})}/></div>
                </Field><Field label="Màu chữ">
                    <div className="flex gap-2"><input type="color" className="h-10 w-12 rounded border"
                                                       value={form.textColor} onChange={e => setForm({
                        ...form,
                        textColor: e.target.value
                    })}/><input required pattern="^#[0-9A-Fa-f]{6}$" className={inputClass} value={form.textColor}
                                onChange={e => setForm({...form, textColor: e.target.value})}/></div>
                </Field></div>
                <div className="rounded-xl bg-surface-container p-6 text-center"><span
                    className="inline-flex rounded-full px-4 py-2 text-sm font-black"
                    style={{backgroundColor: form.color, color: form.textColor}}>{form.name || 'TAG PREVIEW'}</span>
                </div>
                <label className="flex gap-2 text-sm font-bold"><input type="checkbox" checked={form.status === 1}
                                                                       onChange={e => setForm({
                                                                           ...form,
                                                                           status: e.target.checked ? 1 : 0
                                                                       })}/>Hoạt động</label>
                <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-4"><AdminButton
                    type="button" variant="secondary" onClick={() => setOpen(false)}>Hủy</AdminButton><AdminButton
                    type="submit" disabled={saving}>Lưu tag</AdminButton></div>
            </form>
        </AdminModal>
    </AdminPage>;
}
