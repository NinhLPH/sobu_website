import {FormEvent, useCallback, useEffect, useMemo, useState} from 'react';
import {Edit3, Plus, Power, Trash2} from 'lucide-react';
import {AdminCategory, CategoryWriteRequest} from '../../interface/admin-catalog.model';
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

const initial: CategoryWriteRequest = {code: '', name: '', parentId: null, order: 0, image: '', content: '', status: 1};
const flattenHierarchy = (items: AdminCategory[]): Array<AdminCategory & { level: number }> => {
    const visit = (parentId: number | null, level: number): Array<AdminCategory & { level: number }> => items
        .filter(item => (item.parentId ?? null) === parentId)
        .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
        .flatMap(item => [{...item, level}, ...visit(item.id, level + 1)]);
    const tree = visit(null, 0);
    const included = new Set(tree.map(item => item.id));
    return [...tree, ...items.filter(item => !included.has(item.id)).map(item => ({...item, level: 0}))];
};

export default function AdminCategories() {
    const confirm = useConfirmDialog();
    const [items, setItems] = useState<AdminCategory[]>([]);
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [id, setId] = useState<number | null>(null);
    const [form, setForm] = useState<CategoryWriteRequest>(initial);
    const [saving, setSaving] = useState(false);
    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setItems(await AdminCatalogService.getCategories());
        } catch (e) {
            setError(getApiError(e));
        } finally {
            setLoading(false);
        }
    }, []);
    useEffect(() => {
        void load();
    }, [load]);
    const all = useMemo(() => flattenHierarchy(items || []), [items]);
    const visible = all.filter(x => `${x.name} ${x.code}`.toLowerCase().includes(query.toLowerCase()));
    const edit = (x?: AdminCategory, parentId?: number) => {
        setId(x?.id ?? null);
        setForm(x ? {
            code: x.code,
            name: x.name,
            parentId: x.parentId,
            order: x.order,
            image: x.image || '',
            content: x.content || '',
            status: x.status
        } : {...initial, parentId: parentId ?? null});
        setOpen(true);
    };
    const submit = async (e: FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            id ? await AdminCatalogService.updateCategory(id, form) : await AdminCatalogService.createCategory(form);
            ToastService.success(id ? 'Đã cập nhật danh mục.' : 'Đã tạo danh mục.');
            setOpen(false);
            await load();
        } catch (err) {
            ToastService.error(getApiError(err));
        } finally {
            setSaving(false);
        }
    };
    const remove = async (x: AdminCategory) => {
        if (!await confirm({title: 'Xóa danh mục?', message: `Danh mục “${x.name}” sẽ bị xóa khỏi hệ thống.`, confirmLabel: 'Xóa danh mục', tone: 'danger'})) return;
        try {
            await AdminCatalogService.deleteCategory(x.id);
            ToastService.success('Đã xóa danh mục.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e, 'Không thể xóa danh mục đang có sản phẩm hoặc danh mục con.'));
        }
    };
    const toggle = async (x: AdminCategory) => {
        try {
            await AdminCatalogService.setCategoryStatus(x.id, x.status === 1 ? 0 : 1);
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    return <AdminPage title="Danh mục"
                      description="Tổ chức cây danh mục dùng chung cho điều hướng và phân loại sản phẩm."
                      actions={<AdminButton onClick={() => edit()}><Plus className="h-4 w-4"/>Thêm danh
                          mục</AdminButton>}><AdminCard>
        <AdminToolbar><AdminSearch value={query} onChange={setQuery}
                                   placeholder="Tìm danh mục theo tên hoặc mã"/></AdminToolbar>
        {loading ? <AdminLoading/> : error ?
            <AdminError message={error} onRetry={() => void load()}/> : !visible.length ?
                <AdminEmpty title="Không có danh mục"/> :
                <div className="divide-y divide-outline-variant/25">{visible.map(x => <div key={x.id}
                                                                                           className="flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low"
                                                                                           style={{paddingLeft: `${16 + x.level * 24}px`}}>
                    <div className="min-w-0 flex-1"><p className="font-bold">{x.name}</p><p
                        className="text-xs text-outline">{x.code}{x.parentId ? ' · Danh mục con' : ' · Danh mục gốc'}</p>
                    </div>
                    <AdminStatus active={x.status === 1}/>
                    <div className="flex">
                        <button onClick={() => edit(undefined, x.id)}
                                className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-primary"
                                title="Thêm danh mục con"><Plus className="h-4 w-4"/></button>
                        <button onClick={() => edit(x)}
                                className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-primary"
                                title="Sửa"><Edit3 className="h-4 w-4"/></button>
                        <button onClick={() => void toggle(x)}
                                className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-primary"
                                title="Đổi trạng thái"><Power className="h-4 w-4"/></button>
                        <button onClick={() => void remove(x)}
                                className="rounded-lg p-2 text-outline hover:bg-error/10 hover:text-error" title="Xóa">
                            <Trash2 className="h-4 w-4"/></button>
                    </div>
                </div>)}</div>}</AdminCard>
        <AdminModal open={open} onClose={() => setOpen(false)} title={id ? 'Cập nhật danh mục' : 'Thêm danh mục'}
                    size="md">
            <form onSubmit={submit} className="space-y-4 p-5">
                <div className="grid gap-4 sm:grid-cols-2"><Field label="Mã danh mục"><input required
                                                                                             className={inputClass}
                                                                                             value={form.code}
                                                                                             onChange={e => setForm({
                                                                                                 ...form,
                                                                                                 code: e.target.value.toUpperCase()
                                                                                             })}/></Field><Field
                    label="Tên danh mục"><input required className={inputClass} value={form.name}
                                                onChange={e => setForm({...form, name: e.target.value})}/></Field><Field
                    label="Danh mục cha"><select className={`${inputClass} admin-select`} value={form.parentId ?? ''}
                                                 onChange={e => setForm({
                                                     ...form,
                                                     parentId: e.target.value ? Number(e.target.value) : null
                                                 })}>
                    <option value="">Danh mục gốc</option>
                    {all.filter(x => x.id !== id).map(x => <option key={x.id}
                                                                   value={x.id}>{'— '.repeat(x.level)}{x.name}</option>)}
                </select></Field><Field label="Thứ tự"><input type="number" min={0} className={inputClass}
                                                              value={form.order ?? 0} onChange={e => setForm({
                    ...form,
                    order: Number(e.target.value)
                })}/></Field></div>
                <Field label="Đường dẫn ảnh"><input className={inputClass} value={form.image || ''}
                                                    onChange={e => setForm({
                                                        ...form,
                                                        image: e.target.value
                                                    })}/></Field><Field label="Nội dung"><textarea
                className={`${inputClass} min-h-20 py-2`} value={form.content || ''}
                onChange={e => setForm({...form, content: e.target.value})}/></Field><label
                className="flex gap-2 text-sm font-bold"><input type="checkbox" checked={form.status === 1}
                                                                onChange={e => setForm({
                                                                    ...form,
                                                                    status: e.target.checked ? 1 : 0
                                                                })}/>Hoạt động</label>
                <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-4"><AdminButton
                    type="button" variant="secondary" onClick={() => setOpen(false)}>Hủy</AdminButton><AdminButton
                    type="submit" disabled={saving}>Lưu danh mục</AdminButton></div>
            </form>
        </AdminModal>
    </AdminPage>;
}
