import {FormEvent, useEffect, useState} from 'react';
import {ArrowDownToLine, PackageCheck, RefreshCw} from 'lucide-react';
import {
    AdminProductListItem,
    InventoryAdjustment,
    InventoryAdjustmentType,
    InventoryBalance
} from '../../interface/admin-catalog.model';
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
    Field,
    getApiError,
    inputClass
} from '../../components/admin/AdminUi';

const typeNames: Record<string, string> = {
    OPENING_STOCK: 'Tồn đầu kỳ',
    STOCK_IN: 'Nhập kho',
    STOCK_OUT: 'Xuất kho',
    CORRECTION: 'Điều chỉnh kiểm kê',
    DAMAGED: 'Hàng hỏng',
    RETURNED: 'Hoàn kho',
    ORDER_RESERVATION: 'Giữ cho đơn hàng',
    ORDER_RELEASE: 'Hoàn giữ đơn hàng'
};
type ManualType = Exclude<InventoryAdjustmentType, 'OPENING_STOCK' | 'ORDER_RESERVATION' | 'ORDER_RELEASE'>;
export default function AdminInventory() {
    const [products, setProducts] = useState<AdminProductListItem[]>([]);
    const [productQuery, setProductQuery] = useState('');
    const [productId, setProductId] = useState<number | null>(null);
    const [balance, setBalance] = useState<InventoryBalance | null>(null);
    const [ledger, setLedger] = useState<InventoryAdjustment[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [mode, setMode] = useState<'opening' | 'adjust' | null>(null);
    const [quantity, setQuantity] = useState(0);
    const [type, setType] = useState<ManualType>('STOCK_IN');
    const [note, setNote] = useState('');
    const [saving, setSaving] = useState(false);
    useEffect(() => {
        const timer = window.setTimeout(() => {
            AdminCatalogService.getProducts({
                page: 0,
                pageSize: 100,
                search: productQuery || undefined
            }).then(x => setProducts(x.content || [])).catch(e => setError(getApiError(e))).finally(() => setLoading(false));
        }, 250);
        return () => window.clearTimeout(timer);
    }, [productQuery]);
    const loadInventory = async (id: number) => {
        setLoading(true);
        setError('');
        try {
            const [b, l] = await Promise.all([AdminCatalogService.getInventoryBalance(id), AdminCatalogService.getInventoryLedger(id)]);
            setBalance(b);
            setLedger(l || []);
        } catch (e) {
            setError(getApiError(e, 'Không thể tải dữ liệu tồn kho.'));
        } finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (productId) void loadInventory(productId);
    }, [productId]);
    const submit = async (e: FormEvent) => {
        e.preventDefault();
        if (!productId) return;
        setSaving(true);
        try {
            mode === 'opening' ? await AdminCatalogService.setOpeningStock(productId, quantity, note) : await AdminCatalogService.adjustInventory(productId, type, quantity, note);
            ToastService.success(mode === 'opening' ? 'Đã thiết lập tồn đầu kỳ.' : 'Đã ghi nhận điều chỉnh kho.');
            setMode(null);
            setQuantity(0);
            setNote('');
            await loadInventory(productId);
        } catch (err) {
            ToastService.error(getApiError(err));
        } finally {
            setSaving(false);
        }
    };
    const selected = products.find(x => x.id === productId);
    return <AdminPage title="Tồn kho"
                      description="Theo dõi tồn thực tế, tồn khả dụng, lượng đang giữ và lịch sử điều chỉnh của từng sản phẩm."
                      actions={<><AdminButton variant="secondary" disabled={!productId} onClick={() => {
                          setMode('opening');
                          setQuantity(0);
                      }}><PackageCheck className="h-4 w-4"/>Tồn đầu kỳ</AdminButton><AdminButton disabled={!productId}
                                                                                                 onClick={() => {
                                                                                                     setMode('adjust');
                                                                                                     setQuantity(0);
                                                                                                 }}><ArrowDownToLine
                          className="h-4 w-4"/>Điều chỉnh kho</AdminButton></>}>
        <AdminCard className="space-y-3 p-4"><AdminSearch value={productQuery} onChange={setProductQuery}
                                                          placeholder="Tìm sản phẩm theo tên hoặc mã"/><Field
            label="Chọn sản phẩm"><select className={inputClass} value={productId ?? ''}
                                          onChange={e => setProductId(e.target.value ? Number(e.target.value) : null)}>
            <option value="">Chọn sản phẩm</option>
            {products.map(x => <option key={x.id} value={x.id}>{x.code || `#${x.id}`} — {x.name}</option>)}
        </select></Field></AdminCard>
        {loading ? <AdminCard><AdminLoading/></AdminCard> : error ? <AdminCard><AdminError message={error}
                                                                                           onRetry={() => productId && void loadInventory(productId)}/></AdminCard> : !selected ?
            <AdminCard><AdminEmpty title="Chọn sản phẩm để xem tồn kho"/></AdminCard> : <>
                <div className="grid gap-4 sm:grid-cols-3"><AdminCard className="p-5"><p
                    className="text-xs font-bold uppercase text-outline">Tồn thực tế</p><p
                    className="mt-2 text-3xl font-black">{balance?.stockRemain ?? 0}</p></AdminCard><AdminCard
                    className="p-5"><p className="text-xs font-bold uppercase text-outline">Có thể bán</p><p
                    className="mt-2 text-3xl font-black text-primary">{balance?.stockAvailable ?? 0}</p>
                </AdminCard><AdminCard className="p-5"><p className="text-xs font-bold uppercase text-outline">Đang giữ
                    cho đơn</p><p className="mt-2 text-3xl font-black text-amber-600">{balance?.reserved ?? 0}</p>
                </AdminCard></div>
                <AdminCard>
                    <div className="flex items-center justify-between border-b border-outline-variant/30 px-4 py-3">
                        <div><h2 className="font-black">Sổ kho</h2><p
                            className="text-xs text-outline">{selected.name}</p></div>
                        <button onClick={() => productId && void loadInventory(productId)}
                                className="rounded-lg p-2 text-outline hover:text-primary" aria-label="Làm mới">
                            <RefreshCw className="h-4 w-4"/></button>
                    </div>
                    {!ledger.length ? <AdminEmpty title="Chưa có biến động kho"/> : <div className="overflow-x-auto">
                        <table className="w-full min-w-[760px] text-sm">
                            <thead className="bg-surface-container text-left text-xs uppercase text-outline">
                            <tr>
                                <th className="px-4 py-3">Thời gian</th>
                                <th className="px-4 py-3">Loại</th>
                                <th className="px-4 py-3 text-right">Thay đổi</th>
                                <th className="px-4 py-3 text-right">Sau điều chỉnh</th>
                                <th className="px-4 py-3">Ghi chú</th>
                            </tr>
                            </thead>
                            <tbody>{ledger.map(x => <tr key={x.id} className="border-t border-outline-variant/25">
                                <td className="px-4 py-3 text-outline">{x.createdAt ? new Date(x.createdAt).toLocaleString('vi-VN') : '—'}</td>
                                <td className="px-4 py-3 font-bold">{typeNames[x.type] || x.type}</td>
                                <td className={`px-4 py-3 text-right font-black ${x.quantityDelta >= 0 ? 'text-emerald-700' : 'text-error'}`}>{x.quantityDelta > 0 ? '+' : ''}{x.quantityDelta}</td>
                                <td className="px-4 py-3 text-right">{x.balanceAfter}</td>
                                <td className="px-4 py-3 text-outline">{x.note || x.orderCode || '—'}</td>
                            </tr>)}</tbody>
                        </table>
                    </div>}</AdminCard></>}
        <AdminModal open={!!mode} onClose={() => setMode(null)}
                    title={mode === 'opening' ? 'Thiết lập tồn đầu kỳ' : 'Điều chỉnh tồn kho'}
                    description={selected?.name} size="md">
            <form onSubmit={submit} className="space-y-4 p-5">{mode === 'adjust' &&
                <Field label="Loại điều chỉnh"><select className={inputClass} value={type}
                                                       onChange={e => setType(e.target.value as ManualType)}>
                    <option value="STOCK_IN">Nhập kho</option>
                    <option value="STOCK_OUT">Xuất kho</option>
                    <option value="CORRECTION">Điều chỉnh kiểm kê</option>
                    <option value="DAMAGED">Hàng hỏng</option>
                    <option value="RETURNED">Hoàn kho</option>
                </select></Field>}<Field
                label={mode === 'opening' ? 'Số lượng tồn đầu kỳ' : type === 'CORRECTION' ? 'Tồn thực tế sau kiểm kê' : 'Số lượng'}
                hint={type === 'CORRECTION' && mode === 'adjust' ? 'Nhập tổng tồn thực tế mục tiêu, không phải lượng chênh lệch.' : 'Hệ thống tự xác định chiều tăng/giảm theo loại điều chỉnh.'}><input
                required min={mode === 'opening' || type === 'CORRECTION' ? 0 : 0.01} step="any" type="number"
                className={inputClass} value={quantity === 0 ? '0' : quantity}
                onChange={e => setQuantity(Number(e.target.value))}/></Field><Field label="Ghi chú"><textarea required
                                                                                                              className={`${inputClass} min-h-20 py-2`}
                                                                                                              value={note}
                                                                                                              onChange={e => setNote(e.target.value)}
                                                                                                              placeholder="Lý do và thông tin đối soát"/></Field>
                <div className="flex justify-end gap-2 border-t border-outline-variant/30 pt-4"><AdminButton
                    type="button" variant="secondary" onClick={() => setMode(null)}>Hủy</AdminButton><AdminButton
                    type="submit" disabled={saving}>{saving ? 'Đang ghi nhận...' : 'Xác nhận'}</AdminButton></div>
            </form>
        </AdminModal>
    </AdminPage>;
}
