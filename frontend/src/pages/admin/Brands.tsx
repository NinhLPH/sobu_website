import {FormEvent, useCallback, useEffect, useMemo, useState} from 'react';
import {Edit3, Plus, Power, Trash2} from 'lucide-react';
import {AdminBrand, BrandWriteRequest} from '../../interface/admin-catalog.model';
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

const initial: BrandWriteRequest = {code: '', name: '', parentId: null, status: 1};
export default function AdminBrands() {
    const confirm = useConfirmDialog();
    const [items, setItems] = useState<AdminBrand[]>([]);
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [id, setId] = useState<number | null>(null);
    const [form, setForm] = useState<BrandWriteRequest>(initial);
    const [saving, setSaving] = useState(false);
    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setItems(await AdminCatalogService.getBrands());
        } catch (e) {
            setError(getApiError(e));
        } finally {
            setLoading(false);
        }
    }, []);
    useEffect(() => {
        void load();
    }, [load]);
    const visible = useMemo(() => items.filter(x => `${x.name} ${x.code}`.toLowerCase().includes(query.toLowerCase())), [items, query]);
    const edit = (x?: AdminBrand) => {
        setId(x?.id ?? null);
        setForm(x ? {code: x.code, name: x.name, parentId: x.parentId, status: x.status} : initial);
        setOpen(true);
    };
    const submit = async (e: FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            id ? await AdminCatalogService.updateBrand(id, form) : await AdminCatalogService.createBrand(form);
            ToastService.success(id ? 'Đã cập nhật thương hiệu.' : 'Đã tạo thương hiệu.');
            setOpen(false);
            await load();
        } catch (err) {
            ToastService.error(getApiError(err));
        } finally {
            setSaving(false);
        }
    };
    const remove = async (x: AdminBrand) => {
        if (!await confirm({title: 'Xóa thương hiệu?', message: `Thương hiệu “${x.name}” sẽ bị xóa khỏi hệ thống.`, confirmLabel: 'Xóa thương hiệu', tone: 'danger'})) return;
        try {
            await AdminCatalogService.deleteBrand(x.id);
            ToastService.success('Đã xóa thương hiệu.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e, 'Không thể xóa thương hiệu đang được sử dụng.'));
        }
    };
    const toggle = async (x: AdminBrand) => {
        try {
            await AdminCatalogService.setBrandStatus(x.id, x.status === 1 ? 0 : 1);
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    return <AdminPage title="Thương hiệu" description="Quản lý nhãn hiệu và quan hệ thương hiệu cha–con của catalog."
                      actions={<AdminButton onClick={() => edit()}><Plus className="h-4 w-4"/>Thêm thương
                          hiệu</AdminButton>}><AdminCard>
        <AdminToolbar><AdminSearch value={query} onChange={setQuery}
                                   placeholder="Tìm thương hiệu theo tên hoặc mã"
                                   ariaLabel="Tìm kiếm thương hiệu quản trị"/></AdminToolbar>
        {loading ? <AdminLoading/> : error ?
            <AdminError message={error} onRetry={() => void load()}/> : !visible.length ?
                <AdminEmpty title="Không có thương hiệu"/> : <div className="overflow-x-auto">
                    <table className="w-full min-w-[620px] text-sm">
                        <thead className="bg-surface-container text-left text-xs uppercase text-outline">
                        <tr>
                            <th className="px-4 py-3">Mã</th>
                            <th className="px-4 py-3">Tên thương hiệu</th>
                            <th className="px-4 py-3">Thương hiệu cha</th>
                            <th className="px-4 py-3">Trạng thái</th>
                            <th className="px-4 py-3 text-right">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>{visible.map(x => <tr key={x.id} className="border-t border-outline-variant/25">
                            <td className="px-4 py-3 font-mono text-xs">{x.code}</td>
                            <td className="px-4 py-3 font-bold">{x.name}</td>
                            <td className="px-4 py-3 text-outline">{items.find(p => p.id === x.parentId)?.name || '—'}</td>
                            <td className="px-4 py-3"><AdminStatus active={x.status === 1}/></td>
                            <td className="px-4 py-3">
                                <div className="flex justify-end">
                                    <button onClick={() => edit(x)}
                                            className="rounded-lg p-2 text-outline hover:text-primary"><Edit3
                                        className="h-4 w-4"/></button>
                                    <button onClick={() => void toggle(x)}
                                            className="rounded-lg p-2 text-outline hover:text-primary"><Power
                                        className="h-4 w-4"/></button>
                                    <button onClick={() => void remove(x)}
                                            className="rounded-lg p-2 text-outline hover:text-error"><Trash2
                                        className="h-4 w-4"/></button>
                                </div>
                            </td>
                        </tr>)}</tbody>
                    </table>
                </div>}</AdminCard>
        <AdminModal open={open} onClose={() => setOpen(false)} title={id ? 'Cập nhật thương hiệu' : 'Thêm thương hiệu'}
                    size="md">
            <form onSubmit={submit} className="space-y-4 p-5"><Field label="Mã thương hiệu"><input required
                                                                                                   className={inputClass}
                                                                                                   value={form.code}
                                                                                                   onChange={e => setForm({
                                                                                                       ...form,
                                                                                                       code: e.target.value.toUpperCase()
                                                                                                   })}/></Field><Field
                label="Tên thương hiệu"><input required className={inputClass} value={form.name}
                                               onChange={e => setForm({...form, name: e.target.value})}/></Field><Field
                label="Thương hiệu cha"><select className={inputClass} value={form.parentId ?? ''}
                                                onChange={e => setForm({
                                                    ...form,
                                                    parentId: e.target.value ? Number(e.target.value) : null
                                                })}>
                <option value="">Không có</option>
                {items.filter(x => x.id !== id).map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
            </select></Field><label className="flex gap-2 text-sm font-bold"><input type="checkbox"
                                                                                    checked={form.status === 1}
                                                                                    onChange={e => setForm({
                                                                                        ...form,
                                                                                        status: e.target.checked ? 1 : 0
                                                                                    })}/>Hoạt động</label>
                <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-4"><AdminButton
                    type="button" variant="secondary" onClick={() => setOpen(false)}>Hủy</AdminButton><AdminButton
                    type="submit" disabled={saving}>Lưu thương hiệu</AdminButton></div>
            </form>
        </AdminModal>
    </AdminPage>;
}
