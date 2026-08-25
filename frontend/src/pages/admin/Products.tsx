import {FormEvent, useCallback, useEffect, useState} from 'react';
import {Archive, Edit3, Eye, Plus, Power} from 'lucide-react';
import {
    AdminBrand,
    AdminCategory,
    AdminProductDetail,
    AdminProductListItem,
    ProductBadge,
    ProductWriteRequest
} from '../../interface/admin-catalog.model';
import {AdminCatalogService} from '../../service/admin-catalog.service';
import {ToastService} from '../../service/toast.service';
import {formatCurrency} from '../../utils/format';
import {getPublicImageUrl} from '../../utils/file-url';
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

const emptyForm: ProductWriteRequest = {
    code: '',
    name: '',
    retailPrice: 0,
    oldPrice: null,
    categoryId: null,
    brandId: null,
    badgeId: null,
    description: '',
    avatarImage: '',
    active: true,
    status: 'ACTIVE'
};
const numberOrNull = (value: string) => value === '' ? null : Number(value);

export default function AdminProducts() {
    const [items, setItems] = useState<AdminProductListItem[]>([]);
    const [categories, setCategories] = useState<AdminCategory[]>([]);
    const [brands, setBrands] = useState<AdminBrand[]>([]);
    const [badges, setBadges] = useState<ProductBadge[]>([]);
    const [query, setQuery] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [modal, setModal] = useState<'form' | 'detail' | null>(null);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [form, setForm] = useState<ProductWriteRequest>(emptyForm);
    const [detail, setDetail] = useState<AdminProductDetail | null>(null);
    const [saving, setSaving] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const [products, categoryData, brandData, badgeData] = await Promise.all([
                AdminCatalogService.getProducts({page, pageSize: 20, search: query || undefined}),
                AdminCatalogService.getCategories(), AdminCatalogService.getBrands(), AdminCatalogService.getBadges(),
            ]);
            setItems(products.content || []);
            setTotalPages(products.totalPages || 0);
            setCategories(categoryData || []);
            setBrands(brandData || []);
            setBadges(badgeData || []);
        } catch (e) {
            setError(getApiError(e, 'Không thể tải danh sách sản phẩm.'));
        } finally {
            setLoading(false);
        }
    }, [page, query]);

    useEffect(() => {
        const timer = window.setTimeout(() => void load(), 250);
        return () => window.clearTimeout(timer);
    }, [load]);

    const openCreate = () => {
        setEditingId(null);
        setForm({...emptyForm});
        setModal('form');
    };
    const openEdit = async (id: number) => {
        setSaving(true);
        try {
            const p = await AdminCatalogService.getProduct(id);
            setEditingId(id);
            setForm({
                code: p.code || '',
                barcode: p.barcode || '',
                name: p.name,
                otherName: p.otherName || '',
                categoryId: p.categoryId,
                brandId: p.brandId,
                badgeId: p.badgeId,
                retailPrice: p.retailPrice ?? p.price ?? 0,
                importPrice: p.importPrice,
                wholesalePrice: p.wholesalePrice,
                oldPrice: p.oldPrice,
                vat: p.vat,
                avatarImage: p.avatarImage || '',
                images: p.images || [],
                description: p.description || '',
                content: p.content || '',
                length: p.length,
                width: p.width,
                height: p.height,
                weight: p.weight,
                status: p.status || 'ACTIVE',
                active: p.active !== false
            });
            setModal('form');
        } catch (e) {
            ToastService.error(getApiError(e, 'Không thể tải sản phẩm.'));
        } finally {
            setSaving(false);
        }
    };
    const openDetail = async (id: number) => {
        setSaving(true);
        try {
            setDetail(await AdminCatalogService.getProduct(id));
            setModal('detail');
        } catch (e) {
            ToastService.error(getApiError(e));
        } finally {
            setSaving(false);
        }
    };
    const submit = async (event: FormEvent) => {
        event.preventDefault();
        if (form.oldPrice != null && form.oldPrice <= form.retailPrice) {
            ToastService.error('Giá cũ phải lớn hơn giá bán mới.');
            return;
        }
        setSaving(true);
        try {
            editingId ? await AdminCatalogService.updateProduct(editingId, form) : await AdminCatalogService.createProduct(form);
            ToastService.success(editingId ? 'Đã cập nhật sản phẩm.' : 'Đã tạo sản phẩm.');
            setModal(null);
            await load();
        } catch (e) {
            ToastService.error(getApiError(e, 'Không thể lưu sản phẩm.'));
        } finally {
            setSaving(false);
        }
    };
    const toggle = async (item: AdminProductListItem) => {
        try {
            await AdminCatalogService.setProductActive(item.id, item.active === false, 'Cập nhật từ trang quản trị');
            ToastService.success('Đã cập nhật trạng thái sản phẩm.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    const archive = async (item: AdminProductListItem) => {
        if (!window.confirm(`Lưu trữ sản phẩm “${item.name}”?`)) return;
        try {
            await AdminCatalogService.archiveProduct(item.id, 'Lưu trữ từ trang quản trị');
            ToastService.success('Đã lưu trữ sản phẩm.');
            await load();
        } catch (e) {
            ToastService.error(getApiError(e));
        }
    };
    const set = <K extends keyof ProductWriteRequest>(key: K, value: ProductWriteRequest[K]) => setForm(prev => ({
        ...prev,
        [key]: value
    }));

    return <AdminPage title="Sản phẩm"
                      description="Quản lý thông tin bán hàng, giá niêm yết/giá bán và tag hiển thị của sản phẩm."
                      actions={<AdminButton onClick={openCreate}><Plus className="h-4 w-4"/>Thêm sản
                          phẩm</AdminButton>}>
        <AdminCard>
            <div className="flex flex-col gap-3 border-b border-outline-variant/30 p-4 sm:flex-row"><AdminSearch
                value={query} onChange={value => {
                setQuery(value);
                setPage(0);
            }} placeholder="Tìm theo tên hoặc mã sản phẩm" ariaLabel="Tìm kiếm sản phẩm quản trị"/>
                <div className="self-center whitespace-nowrap text-xs text-outline">{items.length} sản phẩm trên trang
                </div>
            </div>
            {loading ? <AdminLoading/> : error ?
                <AdminError message={error} onRetry={() => void load()}/> : !items.length ?
                    <AdminEmpty title="Chưa có sản phẩm phù hợp"
                                description="Thử từ khóa khác hoặc thêm sản phẩm mới."/> :
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[920px] text-left text-sm">
                            <thead className="bg-surface-container text-xs uppercase tracking-wide text-outline">
                            <tr>
                                <th className="px-4 py-3">Sản phẩm</th>
                                <th className="px-4 py-3">Phân loại</th>
                                <th className="px-4 py-3 text-right">Giá bán</th>
                                <th className="px-4 py-3 text-right">Tồn khả dụng</th>
                                <th className="px-4 py-3">Trạng thái</th>
                                <th className="px-4 py-3 text-right">Thao tác</th>
                            </tr>
                            </thead>
                            <tbody>{items.map(item => <tr key={item.id}
                                                          className="border-t border-outline-variant/25 hover:bg-surface-container-low">
                                <td className="px-4 py-3">
                                    <div className="flex items-center gap-3">
                                        <div
                                            className="h-12 w-12 shrink-0 overflow-hidden rounded-lg bg-surface-container p-1">{item.avatarImage ?
                                            <img src={getPublicImageUrl(item.avatarImage)} alt=""
                                                 className="h-full w-full object-contain"/> : null}</div>
                                        <div><p className="max-w-xs font-bold text-on-surface">{item.name}</p><p
                                            className="text-xs text-outline">{item.code || `#${item.id}`}{item.badgeName ? ` · ${item.badgeName}` : ''}</p>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-4 py-3 text-outline">{item.categoryName || '—'}<br/><span
                                    className="text-xs">{item.brandName || 'Chưa có thương hiệu'}</span></td>
                                <td className="px-4 py-3 text-right"><strong
                                    className="text-primary">{formatCurrency(item.retailPrice ?? item.price ?? 0)}</strong>{item.oldPrice ?
                                    <div
                                        className="text-xs text-outline line-through">{formatCurrency(item.oldPrice)}</div> : null}
                                </td>
                                <td className="px-4 py-3 text-right font-bold">{item.stockAvailable ?? item.stockRemain ?? '—'}</td>
                                <td className="px-4 py-3"><AdminStatus active={item.active !== false}/></td>
                                <td className="px-4 py-3">
                                    <div className="flex justify-end gap-1">
                                        <button onClick={() => void openDetail(item.id)}
                                                className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-primary"
                                                title="Xem chi tiết"><Eye className="h-4 w-4"/></button>
                                        <button onClick={() => void openEdit(item.id)}
                                                className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-primary"
                                                title="Chỉnh sửa"><Edit3 className="h-4 w-4"/></button>
                                        <button onClick={() => void toggle(item)}
                                                className="rounded-lg p-2 text-outline hover:bg-surface-container hover:text-primary"
                                                title="Đổi trạng thái"><Power className="h-4 w-4"/></button>
                                        <button onClick={() => void archive(item)}
                                                className="rounded-lg p-2 text-outline hover:bg-error/10 hover:text-error"
                                                title="Lưu trữ"><Archive className="h-4 w-4"/></button>
                                    </div>
                                </td>
                            </tr>)}</tbody>
                        </table>
                    </div>}
            <AdminPagination page={page} totalPages={totalPages} onChange={setPage}/></AdminCard>

        <AdminModal open={modal === 'form'} onClose={() => setModal(null)}
                    title={editingId ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm'}
                    description="Các trường giá dùng VNĐ. Giá cũ phải lớn hơn giá bán để hiển thị trạng thái giảm giá."
                    size="xl">
            <form onSubmit={submit} className="space-y-6 p-5">
                <div className="grid gap-4 md:grid-cols-2"><Field label="Mã sản phẩm"><input required
                                                                                             className={inputClass}
                                                                                             value={form.code}
                                                                                             onChange={e => set('code', e.target.value.toUpperCase())}/></Field><Field
                    label="Tên sản phẩm"><input required className={inputClass} value={form.name}
                                                onChange={e => set('name', e.target.value)}/></Field><Field
                    label="Danh mục"><select className={inputClass} value={form.categoryId ?? ''}
                                             onChange={e => set('categoryId', numberOrNull(e.target.value))}>
                    <option value="">Chưa phân loại</option>
                    {categories.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}</select></Field><Field
                    label="Thương hiệu"><select className={inputClass} value={form.brandId ?? ''}
                                                onChange={e => set('brandId', numberOrNull(e.target.value))}>
                    <option value="">Chưa có</option>
                    {brands.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}</select></Field><Field
                    label="Giá bán mới"><input required min={0} type="number" className={inputClass}
                                               value={form.retailPrice}
                                               onChange={e => set('retailPrice', Number(e.target.value))}/></Field><Field
                    label="Giá cũ" hint="Để trống nếu sản phẩm không giảm giá"><input min={0} type="number"
                                                                                      className={inputClass}
                                                                                      value={form.oldPrice ?? ''}
                                                                                      onChange={e => set('oldPrice', numberOrNull(e.target.value))}/></Field><Field
                    label="Tag hiển thị"><select className={inputClass} value={form.badgeId ?? ''}
                                                 onChange={e => set('badgeId', numberOrNull(e.target.value))}>
                    <option value="">Không có tag</option>
                    {badges.filter(x => x.status === 1).map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
                </select></Field><Field label="Ảnh đại diện (URL hoặc path)"><input className={inputClass}
                                                                                    value={form.avatarImage || ''}
                                                                                    onChange={e => set('avatarImage', e.target.value)}/></Field>
                </div>
                <details className="rounded-xl border border-outline-variant/35 bg-surface-container-low p-4">
                    <summary className="cursor-pointer text-sm font-black text-on-surface">Thông tin nâng cao</summary>
                    <div className="mt-4 grid gap-4 md:grid-cols-2"><Field label="Mã vạch"><input className={inputClass}
                                                                                                  value={form.barcode || ''}
                                                                                                  onChange={e => set('barcode', e.target.value)}/></Field><Field
                        label="Tên khác"><input className={inputClass} value={form.otherName || ''}
                                                onChange={e => set('otherName', e.target.value)}/></Field><Field
                        label="Giá nhập"><input min={0} type="number" className={inputClass}
                                                value={form.importPrice ?? ''}
                                                onChange={e => set('importPrice', numberOrNull(e.target.value))}/></Field><Field
                        label="Giá bán sỉ"><input min={0} type="number" className={inputClass}
                                                  value={form.wholesalePrice ?? ''}
                                                  onChange={e => set('wholesalePrice', numberOrNull(e.target.value))}/></Field><Field
                        label="VAT (%)"><input min={0} type="number" className={inputClass} value={form.vat ?? ''}
                                               onChange={e => set('vat', numberOrNull(e.target.value))}/></Field><Field
                        label="Trạng thái ERP"><input className={inputClass} value={form.status || ''}
                                                      onChange={e => set('status', e.target.value)}/></Field><Field
                        label="Dài (mm)"><input min={0} type="number" className={inputClass} value={form.length ?? ''}
                                                onChange={e => set('length', numberOrNull(e.target.value))}/></Field><Field
                        label="Rộng (mm)"><input min={0} type="number" className={inputClass} value={form.width ?? ''}
                                                 onChange={e => set('width', numberOrNull(e.target.value))}/></Field><Field
                        label="Cao (mm)"><input min={0} type="number" className={inputClass} value={form.height ?? ''}
                                                onChange={e => set('height', numberOrNull(e.target.value))}/></Field><Field
                        label="Khối lượng (g)"><input min={0} type="number" className={inputClass}
                                                      value={form.weight ?? ''}
                                                      onChange={e => set('weight', numberOrNull(e.target.value))}/></Field>
                    </div>
                    <div className="mt-4"><Field label="Album ảnh" hint="Mỗi dòng là một URL hoặc path ảnh"><textarea
                        className={`${inputClass} min-h-24 py-2`} value={(form.images || []).join('\n')}
                        onChange={e => set('images', e.target.value.split('\n').map(x => x.trim()).filter(Boolean))}/></Field>
                    </div>
                </details>
                <Field label="Mô tả ngắn"><textarea className={`${inputClass} min-h-24 py-2`}
                                                    value={form.description || ''}
                                                    onChange={e => set('description', e.target.value)}/></Field><Field
                label="Nội dung chi tiết"><textarea className={`${inputClass} min-h-32 py-2`} value={form.content || ''}
                                                    onChange={e => set('content', e.target.value)}/></Field><label
                className="flex items-center gap-3 text-sm font-bold"><input type="checkbox"
                                                                             checked={form.active !== false}
                                                                             onChange={e => set('active', e.target.checked)}
                                                                             className="h-4 w-4 accent-primary"/>Cho
                phép hiển thị/bán sản phẩm</label>
                <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-4"><AdminButton
                    type="button" variant="secondary" onClick={() => setModal(null)}>Hủy</AdminButton><AdminButton
                    type="submit" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu sản phẩm'}</AdminButton></div>
            </form>
        </AdminModal>

        <AdminModal open={modal === 'detail'} onClose={() => setModal(null)} title="Chi tiết sản phẩm"
                    size="lg">{detail && <div className="grid gap-5 p-5 sm:grid-cols-[180px_1fr]">
            <div className="aspect-square rounded-xl bg-surface-container p-3">{detail.avatarImage &&
                <img src={getPublicImageUrl(detail.avatarImage)} alt={detail.name}
                     className="h-full w-full object-contain"/>}</div>
            <div><h3 className="text-xl font-black">{detail.name}</h3><p
                className="mt-1 text-sm text-outline">{detail.code} · {detail.categoryName || 'Chưa phân loại'} · {detail.brandName || 'Chưa có thương hiệu'}</p>
                <div className="mt-4 flex items-end gap-3"><span
                    className="text-xl font-black text-primary">{formatCurrency(detail.retailPrice ?? detail.price ?? 0)}</span>{detail.oldPrice ?
                    <span className="text-sm text-outline line-through">{formatCurrency(detail.oldPrice)}</span> : null}
                </div>
                <div className="mt-4"><AdminStatus active={detail.active !== false}/>{detail.badgeName &&
                    <span className="ml-2 inline-flex rounded-full px-2.5 py-1 text-xs font-bold" style={{
                        backgroundColor: detail.badgeColor || '#00618e',
                        color: detail.badgeTextColor || '#ffffff'
                    }}>{detail.badgeName}</span>}</div>
                <p className="mt-5 whitespace-pre-wrap text-sm leading-6 text-on-surface-variant">{detail.description || 'Chưa có mô tả.'}</p>
            </div>
        </div>}</AdminModal>
    </AdminPage>;
}
