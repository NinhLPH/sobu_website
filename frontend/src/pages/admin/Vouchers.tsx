import {FormEvent, useCallback, useEffect, useState} from 'react';
import {AlertTriangle, Edit3, Loader2, Plus, Power, Trash2, X} from 'lucide-react';
import {AdminCategory, AdminProductListItem, Voucher, VoucherScope, VoucherSlot, VoucherWriteRequest} from '../../interface/admin-catalog.model';
import {AdminCatalogService, AdminVoucherService} from '../../service/admin-catalog.service';
import {ToastService} from '../../service/toast.service';
import {formatCurrency} from '../../utils/format';
import {
    AdminButton,
    AdminCard,
    AdminEmpty,
    AdminError,
    AdminFilterGroup,
    AdminFilterReset,
    AdminFilterSelect,
    AdminLoading,
    AdminModal,
    AdminPage,
    AdminPagination,
    AdminSearch,
    AdminStatus,
    AdminToolbar,
    Field,
    getApiError,
    inputClass
} from '../../components/admin/AdminUi';
import {useConfirmDialog} from '../../components/common/ConfirmDialog';

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
const toggleId = (values: number[] = [], id: number) => values.includes(id)
    ? values.filter(value => value !== id)
    : [...values, id];

function UsageIndicator({voucher}: { voucher: Voucher }) {
    const used = voucher.usedCount ?? 0;
    const limit = voucher.usageLimit ?? null;
    const percent = limit && limit > 0 ? Math.min(100, Math.round((used / limit) * 100)) : null;
    const nearlyExhausted = percent !== null && percent >= 80;
    return <div className="min-w-28" aria-label={limit ? `Đã dùng ${used} trên ${limit} lượt` : `Đã dùng ${used} lượt, không giới hạn`}>
        <div className={`flex items-center gap-1 text-xs font-bold ${nearlyExhausted ? 'text-amber-800' : 'text-on-surface'}`}>
            {nearlyExhausted && <AlertTriangle className="h-3.5 w-3.5"/>}
            {used}/{limit ?? '∞'}
        </div>
        {percent !== null && <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-surface-container-high">
            <div className={`h-full rounded-full ${nearlyExhausted ? 'bg-amber-500' : 'bg-primary'}`} style={{width: `${percent}%`}}/>
        </div>}
    </div>;
}

export default function AdminVouchers() {
    const confirm = useConfirmDialog();
    const [items, setItems] = useState<Voucher[]>([]);
    const [products, setProducts] = useState<AdminProductListItem[]>([]);
    const [productQuery, setProductQuery] = useState('');
    const [categories, setCategories] = useState<AdminCategory[]>([]);
    const [query, setQuery] = useState('');
    const [activeFilter, setActiveFilter] = useState<'' | 'true' | 'false'>('');
    const [scopeFilter, setScopeFilter] = useState<'' | VoucherScope>('');
    const [slotFilter, setSlotFilter] = useState<'' | VoucherSlot>('');
    const [autoApplyFilter, setAutoApplyFilter] = useState<'' | 'true' | 'false'>('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [id, setId] = useState<number | null>(null);
    const [form, setForm] = useState<VoucherWriteRequest>(initial);
    const [saving, setSaving] = useState(false);
    const [busyVoucherId, setBusyVoucherId] = useState<number | null>(null);
    const [loadingProducts, setLoadingProducts] = useState(false);
    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const [data, p, c] = await Promise.all([AdminVoucherService.getVouchers({
                page,
                size: 20,
                keyword: query || undefined,
                active: activeFilter ? activeFilter === 'true' : undefined,
                scope: scopeFilter || undefined,
                slot: slotFilter || undefined,
                autoApply: autoApplyFilter ? autoApplyFilter === 'true' : undefined,
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
    }, [activeFilter, autoApplyFilter, page, query, scopeFilter, slotFilter]);
    useEffect(() => {
        const timer = window.setTimeout(() => void load(), 250);
        return () => clearTimeout(timer);
    }, [load]);
    useEffect(() => {
        if (!open || form.scope !== 'PRODUCT') return;
        setLoadingProducts(true);
        const timer = window.setTimeout(() => {
            void AdminCatalogService.getProducts({
                page: 0,
                pageSize: 100,
                search: productQuery || undefined
            }).then(data => setProducts(data.content || []))
                .catch(error => ToastService.error(getApiError(error)))
                .finally(() => setLoadingProducts(false));
        }, 250);
        return () => {
            window.clearTimeout(timer);
            setLoadingProducts(false);
        };
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
        setProductQuery('');
        setOpen(true);
    };
    const submit = async (e: FormEvent) => {
        e.preventDefault();
        if (form.type === 'DISCOUNT_PERCENT' && form.value > 100) {
            ToastService.error('Mức giảm phần trăm không được vượt quá 100%.');
            return;
        }
        if (form.type === 'FREE_SHIP' && form.slot !== 'SHIPPING') {
            ToastService.error('Voucher miễn phí vận chuyển phải áp dụng tại slot SHIPPING.');
            return;
        }
        if (form.type !== 'FREE_SHIP' && form.geoScope !== 'ALL') {
            ToastService.error('Giới hạn khu vực chỉ dùng cho voucher miễn phí vận chuyển.');
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
        setBusyVoucherId(x.id);
        try {
            await AdminVoucherService.toggleVoucher(x.id);
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        } finally {
            setBusyVoucherId(null);
        }
    };
    const remove = async (x: Voucher) => {
        if (!await confirm({title: 'Xóa voucher?', message: `Voucher “${x.code}” sẽ bị xóa và không thể áp dụng cho đơn mới.`, confirmLabel: 'Xóa voucher', tone: 'danger'})) return;
        setBusyVoucherId(x.id);
        try {
            await AdminVoucherService.deleteVoucher(x.id);
            ToastService.success('Đã xóa voucher.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        } finally {
            setBusyVoucherId(null);
        }
    };
    const valueText = (x: Voucher) => x.type === 'DISCOUNT_PERCENT' ? `${x.value}%` : x.type === 'FREE_SHIP' ? 'Miễn phí vận chuyển' : formatCurrency(x.value);
    return <AdminPage title="Voucher"
                      description="Quản lý mã ưu đãi theo phạm vi sản phẩm, danh mục, đơn hàng hoặc vận chuyển."
                      actions={<AdminButton onClick={() => edit()}><Plus className="h-4 w-4"/>Tạo
                          voucher</AdminButton>}><AdminCard>
        <AdminToolbar>
            <AdminSearch value={query} onChange={x => {
                setQuery(x);
                setPage(0);
            }} placeholder="Tìm theo mã hoặc tên voucher"/>
            <AdminFilterGroup>
                <AdminFilterSelect label="Trạng thái voucher" value={activeFilter} onChange={value => {
                    setActiveFilter(value as typeof activeFilter);
                    setPage(0);
                }}><option value="">Mọi trạng thái</option><option value="true">Đang hoạt động</option><option value="false">Tạm dừng</option></AdminFilterSelect>
                <AdminFilterSelect label="Phạm vi voucher" value={scopeFilter} onChange={value => {
                    setScopeFilter(value as typeof scopeFilter);
                    setPage(0);
                }}><option value="">Mọi phạm vi</option><option value="ALL">Tất cả</option><option value="PRODUCT">Sản phẩm</option><option value="CATEGORY">Danh mục</option></AdminFilterSelect>
                <AdminFilterSelect label="Slot voucher" value={slotFilter} onChange={value => {
                    setSlotFilter(value as typeof slotFilter);
                    setPage(0);
                }}><option value="">Mọi slot</option><option value="ITEM">Sản phẩm</option><option value="ORDER">Đơn hàng</option><option value="SHIPPING">Vận chuyển</option></AdminFilterSelect>
                <AdminFilterSelect label="Cách áp dụng voucher" value={autoApplyFilter} onChange={value => {
                    setAutoApplyFilter(value as typeof autoApplyFilter);
                    setPage(0);
                }}><option value="">Manual và tự động</option><option value="true">Tự động áp dụng</option><option value="false">Nhập thủ công</option></AdminFilterSelect>
                <AdminFilterReset onClick={() => {
                    setQuery('');
                    setActiveFilter('');
                    setScopeFilter('');
                    setSlotFilter('');
                    setAutoApplyFilter('');
                    setPage(0);
                }} disabled={!query && !activeFilter && !scopeFilter && !slotFilter && !autoApplyFilter}/>
            </AdminFilterGroup>
        </AdminToolbar>
        {loading ? <AdminLoading/> : error ? <AdminError message={error} onRetry={() => void load()}/> : !items.length ?
            <AdminEmpty title="Chưa có voucher phù hợp" description="Hãy đổi bộ lọc hoặc tạo voucher mới."/> : <>
            <div className="space-y-3 p-3 md:hidden">
                {items.map(x => <article key={x.id} className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-4 shadow-sm">
                    <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0"><strong className="font-mono text-sm text-primary">{x.code}</strong><p className="mt-1 text-xs font-semibold text-outline">{x.name}</p></div>
                        <AdminStatus active={x.active !== false}/>
                    </div>
                    <div className="mt-3 grid grid-cols-2 gap-3 text-xs">
                        <div><p className="text-[10px] font-black uppercase tracking-wide text-outline">Ưu đãi</p><p className="mt-1 font-bold text-on-surface">{valueText(x)}</p></div>
                        <div><p className="text-[10px] font-black uppercase tracking-wide text-outline">Phạm vi</p><p className="mt-1 font-bold text-on-surface">{x.scope} · {x.slot}</p></div>
                        <div><p className="text-[10px] font-black uppercase tracking-wide text-outline">Cách áp dụng</p><p className="mt-1 font-bold text-on-surface">{x.autoApply ? 'Tự động' : 'Nhập mã'}</p></div>
                        <div><p className="text-[10px] font-black uppercase tracking-wide text-outline">Lượt dùng</p><div className="mt-1"><UsageIndicator voucher={x}/></div></div>
                    </div>
                    <div className="mt-4 flex justify-end gap-1 border-t border-outline-variant/25 pt-3">
                        <button type="button" aria-label={`Sửa voucher ${x.code}`} onClick={() => edit(x)} disabled={busyVoucherId === x.id} className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-primary/10 hover:text-primary focus-visible:ring-2 focus-visible:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-50"><Edit3 className="h-4 w-4"/></button>
                        <button type="button" aria-label={`${x.active !== false ? 'Tạm dừng' : 'Kích hoạt'} voucher ${x.code}`} onClick={() => void toggle(x)} disabled={busyVoucherId === x.id} className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-primary/10 hover:text-primary focus-visible:ring-2 focus-visible:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-50">{busyVoucherId === x.id ? <Loader2 className="h-4 w-4 animate-spin"/> : <Power className="h-4 w-4"/>}</button>
                        <button type="button" aria-label={`Xóa voucher ${x.code}`} onClick={() => void remove(x)} disabled={busyVoucherId === x.id} className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-error/10 hover:text-error focus-visible:ring-2 focus-visible:ring-error/30 disabled:cursor-not-allowed disabled:opacity-50"><Trash2 className="h-4 w-4"/></button>
                    </div>
                </article>)}
            </div>
            <div className="hidden overflow-x-auto md:block">
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
                        <td className="px-4 py-3"><UsageIndicator voucher={x}/></td>
                        <td className="px-4 py-3"><AdminStatus active={x.active !== false}/></td>
                        <td className="px-4 py-3">
                            <div className="flex justify-end">
                                <button type="button" aria-label={`Sửa voucher ${x.code}`} onClick={() => edit(x)} disabled={busyVoucherId === x.id}
                                        className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-primary/10 hover:text-primary focus-visible:ring-2 focus-visible:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-50"><Edit3
                                    className="h-4 w-4"/></button>
                                <button type="button" aria-label={`${x.active !== false ? 'Tạm dừng' : 'Kích hoạt'} voucher ${x.code}`} onClick={() => void toggle(x)} disabled={busyVoucherId === x.id}
                                        className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-primary/10 hover:text-primary focus-visible:ring-2 focus-visible:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-50">{busyVoucherId === x.id ? <Loader2 className="h-4 w-4 animate-spin"/> : <Power
                                    className="h-4 w-4"/>}</button>
                                <button type="button" aria-label={`Xóa voucher ${x.code}`} onClick={() => void remove(x)} disabled={busyVoucherId === x.id}
                                        className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg text-outline transition-colors hover:bg-error/10 hover:text-error focus-visible:ring-2 focus-visible:ring-error/30 disabled:cursor-not-allowed disabled:opacity-50"><Trash2
                                    className="h-4 w-4"/></button>
                            </div>
                        </td>
                    </tr>)}</tbody>
                </table>
            </div></>}<AdminPagination page={page} totalPages={totalPages} onChange={setPage}/></AdminCard>
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
                    onChange={e => {
                        const type = e.target.value as VoucherWriteRequest['type'];
                        setForm({...form, type, slot: type === 'FREE_SHIP' ? 'SHIPPING' : form.slot, geoScope: type === 'FREE_SHIP' ? form.geoScope : 'ALL'});
                    }}>
                    <option value="DISCOUNT_PERCENT">Giảm theo phần trăm</option>
                    <option value="DISCOUNT_AMOUNT">Giảm số tiền cố định</option>
                    <option value="FREE_SHIP">Miễn phí vận chuyển</option>
                </select></Field><Field label="Áp dụng tại"><select className={inputClass} value={form.slot}
                                                                    onChange={e => setForm({...form, slot: e.target.value as VoucherWriteRequest['slot']})}>
                    <option value="ITEM" disabled={form.type === 'FREE_SHIP'}>Sản phẩm</option>
                    <option value="ORDER" disabled={form.type === 'FREE_SHIP'}>Đơn hàng</option>
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
                                                                    onChange={e => {
                                                                        const scope = e.target.value as VoucherWriteRequest['scope'];
                                                                        setForm({
                                                                            ...form,
                                                                            scope,
                                                                            applicableProductIds: scope === 'PRODUCT' ? form.applicableProductIds : [],
                                                                            applicableCategoryIds: scope === 'CATEGORY' ? form.applicableCategoryIds : []
                                                                        });
                                                                    }}>
                    <option value="ALL">Tất cả</option>
                    <option value="PRODUCT">Sản phẩm chỉ định</option>
                    <option value="CATEGORY">Danh mục chỉ định</option>
                </select></Field><Field label="Khu vực"><select className={inputClass} value={form.geoScope}
                                                                disabled={form.type !== 'FREE_SHIP'}
                                                                onChange={e => setForm({
                                                                    ...form,
                                                                    geoScope: e.target.value as VoucherWriteRequest['geoScope']
                                                                })}>
                    <option value="ALL">Toàn quốc</option>
                    <option value="HANOI_CENTER">Trung tâm Hà Nội</option>
                </select></Field></div>
                {form.scope === 'PRODUCT' && <fieldset className="space-y-3 rounded-xl border border-outline-variant/40 p-4">
                    <legend className="px-1 text-xs font-black uppercase tracking-wide text-outline">Sản phẩm áp dụng</legend>
                    {(form.applicableProductIds || []).length > 0 && <div className="flex flex-wrap gap-2" aria-label="Sản phẩm đã chọn">
                        {(form.applicableProductIds || []).map(productId => {
                            const product = products.find(item => item.id === productId);
                            return <span key={productId} className="inline-flex items-center gap-1.5 rounded-full border border-primary/20 bg-primary/10 px-3 py-1.5 text-xs font-bold text-primary">
                                {product?.name || `Sản phẩm #${productId}`}
                                <button type="button" aria-label={`Bỏ chọn ${product?.name || `sản phẩm ${productId}`}`} onClick={() => setForm({...form, applicableProductIds: toggleId(form.applicableProductIds, productId)})} className="cursor-pointer rounded-full p-0.5 transition-colors hover:bg-primary/10 focus-visible:ring-2 focus-visible:ring-primary/30"><X className="h-3 w-3"/></button>
                            </span>;
                        })}
                    </div>}
                    <AdminSearch value={productQuery} onChange={setProductQuery} placeholder="Tìm sản phẩm để áp dụng voucher"/>
                    <div className="max-h-52 overflow-y-auto rounded-lg border border-outline-variant/35 bg-surface-container-lowest p-2" aria-live="polite">
                        {loadingProducts ? <div className="flex min-h-24 items-center justify-center gap-2 text-xs font-semibold text-outline"><Loader2 className="h-4 w-4 animate-spin text-primary"/>Đang tải sản phẩm...</div> : products.length === 0 ? <p className="px-3 py-8 text-center text-xs font-semibold text-outline">Không tìm thấy sản phẩm.</p> : products.map(product => <label key={product.id} className="flex min-h-11 cursor-pointer items-start gap-3 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-surface-container focus-within:ring-2 focus-within:ring-primary/20">
                            <input type="checkbox" className="mt-1 h-4 w-4 accent-primary" checked={(form.applicableProductIds || []).includes(product.id)} onChange={() => setForm({...form, applicableProductIds: toggleId(form.applicableProductIds, product.id)})}/>
                            <span className="min-w-0"><strong className="block truncate text-on-surface">{product.name}</strong><span className="text-xs text-outline">{product.code}</span></span>
                        </label>)}
                    </div>
                </fieldset>}
                {form.scope === 'CATEGORY' && <fieldset className="space-y-3 rounded-xl border border-outline-variant/40 p-4">
                    <legend className="px-1 text-xs font-black uppercase tracking-wide text-outline">Danh mục áp dụng</legend>
                    {(form.applicableCategoryIds || []).length > 0 && <div className="flex flex-wrap gap-2" aria-label="Danh mục đã chọn">
                        {(form.applicableCategoryIds || []).map(categoryId => {
                            const category = categories.find(item => item.id === categoryId);
                            return <span key={categoryId} className="inline-flex items-center gap-1.5 rounded-full border border-primary/20 bg-primary/10 px-3 py-1.5 text-xs font-bold text-primary">
                                {category?.name || `Danh mục #${categoryId}`}
                                <button type="button" aria-label={`Bỏ chọn ${category?.name || `danh mục ${categoryId}`}`} onClick={() => setForm({...form, applicableCategoryIds: toggleId(form.applicableCategoryIds, categoryId)})} className="cursor-pointer rounded-full p-0.5 transition-colors hover:bg-primary/10 focus-visible:ring-2 focus-visible:ring-primary/30"><X className="h-3 w-3"/></button>
                            </span>;
                        })}
                    </div>}
                    <div className="max-h-52 overflow-y-auto rounded-lg border border-outline-variant/35 bg-surface-container-lowest p-2">
                        {categories.length === 0 ? <p className="px-3 py-8 text-center text-xs font-semibold text-outline">Không có danh mục để chọn.</p> : categories.map(category => <label key={category.id} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-surface-container focus-within:ring-2 focus-within:ring-primary/20">
                            <input type="checkbox" className="h-4 w-4 accent-primary" checked={(form.applicableCategoryIds || []).includes(category.id)} onChange={() => setForm({...form, applicableCategoryIds: toggleId(form.applicableCategoryIds, category.id)})}/>
                            <span className="font-semibold text-on-surface">{category.name}</span>
                        </label>)}
                    </div>
                </fieldset>}
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
                    type="submit" disabled={saving}>{saving && <Loader2 className="h-4 w-4 animate-spin"/>}Lưu voucher</AdminButton></div>
            </form>
        </AdminModal>
    </AdminPage>;
}
