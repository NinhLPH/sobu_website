import {FormEvent, useCallback, useEffect, useState} from 'react';
import {Edit3, Plus, Power, Trash2} from 'lucide-react';
import {AdminCategory, AdminProductListItem, Voucher, VoucherWriteRequest} from '../../interface/admin-catalog.model';
import {AdminCatalogService, AdminVoucherService} from '../../service/admin-catalog.service';
import {ToastService} from '../../service/toast.service';
import {formatCurrency} from '../../utils/format';
import {
    AdminButton,
    AdminCard,
    AdminEmpty,
    AdminError,
    AdminLoading,
    AdminModal,
    AdminPage,
    AdminPagination,
    AdminSearch,
    AdminStatus,
    Field,
    getApiError,
    inputClass
} from '../../components/admin/AdminUi';

const initial: VoucherWriteRequest = {
    code: '',
    name: '',
    type: 'DISCOUNT_PERCENT',
    slot: 'ORDER',
    scope: 'ALL',
    geoScope: 'ALL',
    value: 0,
    maxDiscountAmount: null,
    minOrderValue: 0,
    usageLimit: null,
    autoApply: false,
    applicableProductIds: [],
    applicableCategoryIds: [],
    startDate: '',
    endDate: '',
    active: true
};
const asLocal = (value?: string | null) => value ? value.slice(0, 16) : '';
const ids = (options: HTMLOptionsCollection) => Array.from(options).filter(x => x.selected).map(x => Number(x.value));

export default function AdminVouchers() {
    const [items, setItems] = useState<Voucher[]>([]);
    const [products, setProducts] = useState<AdminProductListItem[]>([]);
    const [productQuery, setProductQuery] = useState('');
    const [categories, setCategories] = useState<AdminCategory[]>([]);
    const [query, setQuery] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [id, setId] = useState<number | null>(null);
    const [form, setForm] = useState<VoucherWriteRequest>(initial);
    const [saving, setSaving] = useState(false);
    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const [data, p, c] = await Promise.all([AdminVoucherService.getVouchers({
                page,
                size: 20,
                keyword: query || undefined,
                sort: 'createdAt,desc'
            }), AdminCatalogService.getProducts({page: 0, pageSize: 100}), AdminCatalogService.getCategories()]);
            setItems(data.content || []);
            setTotalPages(data.totalPages || 0);
            setProducts(p.content || []);
            setCategories(c || []);
        } catch (e) {
            setError(getApiError(e));
        } finally {
            setLoading(false);
        }
    }, [page, query]);
    useEffect(() => {
        const timer = window.setTimeout(() => void load(), 250);
        return () => clearTimeout(timer);
    }, [load]);
    useEffect(() => {
        if (!open || form.scope !== 'PRODUCT') return;
        const timer = window.setTimeout(() => {
            void AdminCatalogService.getProducts({
                page: 0,
                pageSize: 100,
                search: productQuery || undefined
            }).then(data => setProducts(data.content || [])).catch(error => ToastService.error(getApiError(error)));
        }, 250);
        return () => window.clearTimeout(timer);
    }, [open, form.scope, productQuery]);
    const edit = (x?: Voucher) => {
        setId(x?.id ?? null);
        setForm(x ? {
            code: x.code,
            name: x.name,
            type: x.type,
            slot: x.slot,
            scope: x.scope,
            geoScope: x.geoScope,
            value: x.value,
            maxDiscountAmount: x.maxDiscountAmount,
            minOrderValue: x.minOrderValue,
            usageLimit: x.usageLimit,
            autoApply: x.autoApply,
            applicableProductIds: x.applicableProductIds || [],
            applicableCategoryIds: x.applicableCategoryIds || [],
            startDate: asLocal(x.startDate),
            endDate: asLocal(x.endDate),
            active: x.active
        } : {...initial});
        setOpen(true);
    };
    const submit = async (e: FormEvent) => {
        e.preventDefault();
        if (form.type === 'DISCOUNT_PERCENT' && form.value > 100) {
            ToastService.error('Mức giảm phần trăm không được vượt quá 100%.');
            return;
        }
        if (form.startDate && form.endDate && new Date(form.endDate) <= new Date(form.startDate)) {
            ToastService.error('Ngày kết thúc phải sau ngày bắt đầu.');
            return;
        }
        if (form.scope === 'PRODUCT' && !form.applicableProductIds?.length) {
            ToastService.error('Vui lòng chọn ít nhất một sản phẩm áp dụng.');
            return;
        }
        if (form.scope === 'CATEGORY' && !form.applicableCategoryIds?.length) {
            ToastService.error('Vui lòng chọn ít nhất một danh mục áp dụng.');
            return;
        }
        setSaving(true);
        try {
            const payload = {...form, startDate: form.startDate || null, endDate: form.endDate || null};
            id ? await AdminVoucherService.updateVoucher(id, payload) : await AdminVoucherService.createVoucher(payload);
            ToastService.success(id ? 'Đã cập nhật voucher.' : 'Đã tạo voucher.');
            setOpen(false);
            await load();
        } catch (err) {
            ToastService.error(getApiError(err));
        } finally {
            setSaving(false);
        }
    };
    const toggle = async (x: Voucher) => {
        try {
            await AdminVoucherService.toggleVoucher(x.id);
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    const remove = async (x: Voucher) => {
        if (!window.confirm(`Xóa voucher “${x.code}”?`)) return;
        try {
            await AdminVoucherService.deleteVoucher(x.id);
            ToastService.success('Đã xóa voucher.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    const valueText = (x: Voucher) => x.type === 'DISCOUNT_PERCENT' ? `${x.value}%` : x.type === 'FREE_SHIP' ? 'Miễn phí vận chuyển' : formatCurrency(x.value);
    return <AdminPage title="Voucher"
                      description="Quản lý mã ưu đãi theo phạm vi sản phẩm, danh mục, đơn hàng hoặc vận chuyển."
                      actions={<AdminButton onClick={() => edit()}><Plus className="h-4 w-4"/>Tạo
                          voucher</AdminButton>}><AdminCard>
        <div className="border-b border-outline-variant/30 p-4"><AdminSearch value={query} onChange={x => {
            setQuery(x);
            setPage(0);
        }} placeholder="Tìm theo mã hoặc tên voucher"/></div>
        {loading ? <AdminLoading/> : error ? <AdminError message={error} onRetry={() => void load()}/> : !items.length ?
            <AdminEmpty title="Chưa có voucher phù hợp"/> : <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-sm">
                    <thead className="bg-surface-container text-left text-xs uppercase text-outline">
                    <tr>
                        <th className="px-4 py-3">Mã / Tên</th>
                        <th className="px-4 py-3">Ưu đãi</th>
                        <th className="px-4 py-3">Phạm vi</th>
                        <th className="px-4 py-3">Đã dùng</th>
                        <th className="px-4 py-3">Trạng thái</th>
                        <th className="px-4 py-3 text-right">Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>{items.map(x => <tr key={x.id} className="border-t border-outline-variant/25">
                        <td className="px-4 py-3"><strong className="font-mono text-primary">{x.code}</strong><p
                            className="mt-1 text-xs text-outline">{x.name}</p></td>
                        <td className="px-4 py-3 font-bold">{valueText(x)}{x.autoApply &&
                            <p className="text-xs font-normal text-outline">Tự động áp dụng</p>}</td>
                        <td className="px-4 py-3">{x.scope} · {x.slot}<p
                            className="text-xs text-outline">{x.geoScope}</p></td>
                        <td className="px-4 py-3">{x.usedCount || 0}/{x.usageLimit ?? '∞'}</td>
                        <td className="px-4 py-3"><AdminStatus active={x.active !== false}/></td>
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
            </div>}<AdminPagination page={page} totalPages={totalPages} onChange={setPage}/></AdminCard>
        <AdminModal open={open} onClose={() => setOpen(false)} title={id ? 'Cập nhật voucher' : 'Tạo voucher'}
                    size="xl">
            <form onSubmit={submit} className="space-y-5 p-5">
                <div className="grid gap-4 md:grid-cols-2"><Field label="Mã voucher"><input required
                                                                                            className={inputClass}
                                                                                            value={form.code}
                                                                                            onChange={e => setForm({
                                                                                                ...form,
                                                                                                code: e.target.value.toUpperCase().replace(/\s/g, '')
                                                                                            })}/></Field><Field
                    label="Tên chương trình"><input required className={inputClass} value={form.name}
                                                    onChange={e => setForm({
                                                        ...form,
                                                        name: e.target.value
                                                    })}/></Field><Field label="Loại ưu đãi"><select
                    className={inputClass} value={form.type}
                    onChange={e => setForm({...form, type: e.target.value as VoucherWriteRequest['type']})}>
                    <option value="DISCOUNT_PERCENT">Giảm theo phần trăm</option>
                    <option value="DISCOUNT_AMOUNT">Giảm số tiền cố định</option>
                    <option value="FREE_SHIP">Miễn phí vận chuyển</option>
                </select></Field><Field label="Áp dụng tại"><select className={inputClass} value={form.slot}
                                                                    onChange={e => setForm({
                                                                        ...form,
                                                                        slot: e.target.value as VoucherWriteRequest['slot']
                                                                    })}>
                    <option value="ITEM">Sản phẩm</option>
                    <option value="ORDER">Đơn hàng</option>
                    <option value="SHIPPING">Phí vận chuyển</option>
                </select></Field><Field label="Giá trị"><input required min={0} type="number" className={inputClass}
                                                               value={form.value} onChange={e => setForm({
                    ...form,
                    value: Number(e.target.value)
                })}/></Field><Field label="Giảm tối đa"><input min={0} type="number" className={inputClass}
                                                               value={form.maxDiscountAmount ?? ''}
                                                               onChange={e => setForm({
                                                                   ...form,
                                                                   maxDiscountAmount: e.target.value ? Number(e.target.value) : null
                                                               })}/></Field><Field label="Giá trị đơn tối thiểu"><input
                    min={0} type="number" className={inputClass} value={form.minOrderValue ?? ''}
                    onChange={e => setForm({
                        ...form,
                        minOrderValue: e.target.value ? Number(e.target.value) : 0
                    })}/></Field><Field label="Giới hạn lượt dùng"><input min={1} type="number" className={inputClass}
                                                                          value={form.usageLimit ?? ''}
                                                                          onChange={e => setForm({
                                                                              ...form,
                                                                              usageLimit: e.target.value ? Number(e.target.value) : null
                                                                          })}/></Field><Field
                    label="Ngày bắt đầu"><input type="datetime-local" className={inputClass}
                                                value={form.startDate || ''} onChange={e => setForm({
                    ...form,
                    startDate: e.target.value
                })}/></Field><Field label="Ngày kết thúc"><input type="datetime-local" className={inputClass}
                                                                 value={form.endDate || ''} onChange={e => setForm({
                    ...form,
                    endDate: e.target.value
                })}/></Field><Field label="Phạm vi catalog"><select className={inputClass} value={form.scope}
                                                                    onChange={e => setForm({
                                                                        ...form,
                                                                        scope: e.target.value as VoucherWriteRequest['scope']
                                                                    })}>
                    <option value="ALL">Tất cả</option>
                    <option value="PRODUCT">Sản phẩm chỉ định</option>
                    <option value="CATEGORY">Danh mục chỉ định</option>
                </select></Field><Field label="Khu vực"><select className={inputClass} value={form.geoScope}
                                                                onChange={e => setForm({
                                                                    ...form,
                                                                    geoScope: e.target.value as VoucherWriteRequest['geoScope']
                                                                })}>
                    <option value="ALL">Toàn quốc</option>
                    <option value="HANOI_CENTER">Trung tâm Hà Nội</option>
                </select></Field></div>
                {form.scope === 'PRODUCT' &&
                    <div className="space-y-3"><AdminSearch value={productQuery} onChange={setProductQuery}
                                                            placeholder="Tìm sản phẩm để áp dụng voucher"/><Field
                        label="Sản phẩm áp dụng" hint="Giữ Ctrl/Cmd để chọn nhiều sản phẩm"><select multiple
                                                                                                    className={`${inputClass} min-h-36 py-2`}
                                                                                                    value={(form.applicableProductIds || []).map(String)}
                                                                                                    onChange={e => setForm({
                                                                                                        ...form,
                                                                                                        applicableProductIds: ids(e.target.options)
                                                                                                    })}>{products.map(x =>
                        <option key={x.id} value={x.id}>{x.code} — {x.name}</option>)}</select></Field>
                    </div>}{form.scope === 'CATEGORY' &&
                <Field label="Danh mục áp dụng" hint="Giữ Ctrl/Cmd để chọn nhiều danh mục"><select multiple
                                                                                                   className={`${inputClass} min-h-28 py-2`}
                                                                                                   value={(form.applicableCategoryIds || []).map(String)}
                                                                                                   onChange={e => setForm({
                                                                                                       ...form,
                                                                                                       applicableCategoryIds: ids(e.target.options)
                                                                                                   })}>{categories.map(x =>
                    <option key={x.id} value={x.id}>{x.name}</option>)}</select></Field>}
                <div className="flex flex-wrap gap-5"><label className="flex gap-2 text-sm font-bold"><input
                    type="checkbox" checked={form.autoApply || false}
                    onChange={e => setForm({...form, autoApply: e.target.checked})}/>Tự động áp dụng</label><label
                    className="flex gap-2 text-sm font-bold"><input type="checkbox" checked={form.active !== false}
                                                                    onChange={e => setForm({
                                                                        ...form,
                                                                        active: e.target.checked
                                                                    })}/>Kích hoạt ngay</label></div>
                <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-4"><AdminButton
                    type="button" variant="secondary" onClick={() => setOpen(false)}>Hủy</AdminButton><AdminButton
                    type="submit" disabled={saving}>Lưu voucher</AdminButton></div>
            </form>
        </AdminModal>
    </AdminPage>;
}
