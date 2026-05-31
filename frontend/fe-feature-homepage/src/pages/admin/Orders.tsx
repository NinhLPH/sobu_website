import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {Eye, Search, Loader2, FileText, SlidersHorizontal, RefreshCw} from 'lucide-react';
import {AdminWorkflowService} from '../../service/admin.service';
import {OrderResponseDto} from '../../interface/order.model';
import {formatCurrency} from "../../util/format";

export default function AdminOrders() {
    const [orders, setOrders] = useState<OrderResponseDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState<string>('ALL');
    const [syncFilter, setSyncFilter] = useState<string>('ALL');

    const fetchOrdersList = async () => {
        setIsLoading(true);
        try {
            const data = await AdminWorkflowService.getOrders();
            const list = data && 'content' in data ? (data as any).content : (Array.isArray(data) ? data : []);
            setOrders(list);
        } catch (err) {
            console.error('Error fetching admin orders list:', err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchOrdersList();
    }, []);

    const filteredOrders = orders.filter(o => {
        const matchesSearch =
            String(o.id).toLowerCase().includes(searchTerm.toLowerCase()) ||
            (o.orderCode && o.orderCode.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (o.customerName && o.customerName.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (o.customerMobile && o.customerMobile.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (o.nhanhOrderCode && o.nhanhOrderCode.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesStatus = statusFilter === 'ALL' || o.status === statusFilter;
        const matchesSync = syncFilter === 'ALL' || o.syncStatus === syncFilter;

        return matchesSearch && matchesStatus && matchesSync;
    });

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'NEW':
                return 'bg-amber-100 text-amber-800';
            case 'PENDING':
                return 'bg-amber-100 text-amber-800';
            case 'PROCESSING':
                return 'bg-blue-100 text-blue-800';
            case 'SHIPPED':
                return 'bg-purple-100 text-purple-800';
            case 'DELIVERED':
                return 'bg-green-100 text-green-800';
            case 'CANCELLED':
                return 'bg-red-100 text-red-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'NEW':
                return 'Mới';
            case 'PENDING':
                return 'Chờ duyệt';
            case 'PROCESSING':
                return 'Đang xử lý';
            case 'SHIPPED':
                return 'Đang giao';
            case 'DELIVERED':
                return 'Đã giao';
            case 'CANCELLED':
                return 'Đã hủy';
            default:
                return status;
        }
    };

    const getSyncStatusColor = (sync: string) => {
        switch (sync) {
            case 'SYNCED':
                return 'bg-green-50 text-green-700 border-green-200';
            case 'FAILED':
                return 'bg-red-50 text-red-700 border-red-200';
            case 'PENDING':
                return 'bg-amber-50 text-amber-700 border-amber-200';
            default:
                return 'bg-gray-50 text-gray-700 border-gray-200';
        }
    };

    const getSyncStatusText = (sync: string) => {
        switch (sync) {
            case 'SYNCED':
                return 'Đã đồng bộ';
            case 'FAILED':
                return 'Thất bại';
            case 'PENDING':
                return 'Đang chờ';
            default:
                return sync;
        }
    };

    return (
        <div className="pt-6 space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-black text-on-surface uppercase tracking-tight">Quản lý Đơn hàng ERP</h1>
                <button
                    onClick={fetchOrdersList}
                    className="flex items-center gap-1.5 px-4 py-2 bg-surface-container hover:bg-surface-container-high rounded-xl text-xs font-bold text-on-surface transition-colors cursor-pointer"
                >
                    <RefreshCw className="w-3.5 h-3.5"/> Làm mới
                </button>
            </div>

            {/* Filter Toolbar */}
            <div className="bg-white p-4 rounded-2xl border border-outline-variant/30 shadow-sm space-y-3">
                <div className="flex items-center gap-3 bg-surface-container rounded-xl px-4 py-2.5">
                    <Search className="text-outline w-4 h-4"/>
                    <input
                        type="text"
                        placeholder="Tìm kiếm mã đơn, SĐT, tên khách, hoặc mã Nhanh.vn..."
                        className="bg-transparent border-none outline-none w-full text-xs font-semibold"
                        value={searchTerm}
                        onChange={e => setSearchTerm(e.target.value)}
                    />
                </div>

                <div className="flex flex-wrap gap-3">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-outline">
                        <SlidersHorizontal className="w-3.5 h-3.5"/>
                        <span>Trạng thái đơn:</span>
                    </div>
                    <select
                        value={statusFilter}
                        onChange={e => setStatusFilter(e.target.value)}
                        className="bg-surface-container rounded-lg px-3 py-1.5 text-xs font-bold text-on-surface cursor-pointer outline-none border-none"
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="NEW">Mới (NEW)</option>
                        <option value="PENDING">Chờ duyệt (PENDING)</option>
                        <option value="PROCESSING">Đang xử lý (PROCESSING)</option>
                        <option value="SHIPPED">Đang giao (SHIPPED)</option>
                        <option value="DELIVERED">Đã giao (DELIVERED)</option>
                        <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                    </select>

                    <div className="flex items-center gap-1.5 text-xs font-bold text-outline ml-2">
                        <span>Đồng bộ Nhanh.vn:</span>
                    </div>
                    <select
                        value={syncFilter}
                        onChange={e => setSyncFilter(e.target.value)}
                        className="bg-surface-container rounded-lg px-3 py-1.5 text-xs font-bold text-on-surface cursor-pointer outline-none border-none"
                    >
                        <option value="ALL">Tất cả đồng bộ</option>
                        <option value="SYNCED">Đã đồng bộ (SYNCED)</option>
                        <option value="PENDING">Đang chờ (PENDING)</option>
                        <option value="FAILED">Thất bại (FAILED)</option>
                    </select>
                </div>
            </div>

            {/* List Table */}
            <div className="bg-white rounded-2xl border border-outline-variant/30 overflow-hidden shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-xs text-left">
                        <thead className="bg-surface-variant font-bold text-on-surface-variant">
                        <tr>
                            <th className="px-6 py-4">Mã đơn (SOBU / Nhanh)</th>
                            <th className="px-6 py-4">Khách hàng</th>
                            <th className="px-6 py-4">Ngày tạo</th>
                            <th className="px-6 py-4 text-right">Tổng chi phí</th>
                            <th className="px-6 py-4 text-center">Đồng bộ Nhanh.vn</th>
                            <th className="px-6 py-4 text-center">Trạng thái</th>
                            <th className="px-6 py-4 text-center">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        {isLoading ? (
                            <tr>
                                <td colSpan={7} className="px-6 py-16 text-center text-on-surface-variant">
                                    <div className="flex flex-col items-center justify-center">
                                        <Loader2 className="w-8 h-8 text-primary animate-spin mb-2"/>
                                        <p className="text-outline text-[10px] font-bold">Đang tải danh sách đơn
                                            hàng...</p>
                                    </div>
                                </td>
                            </tr>
                        ) : filteredOrders.map(order => (
                            <tr key={order.id}
                                className="border-b border-outline-variant/20 hover:bg-surface-container-lowest/50">
                                <td className="px-6 py-4 font-bold">
                                    <p className="text-primary font-black">#{order.orderCode || order.id}</p>
                                    {order.nhanhOrderCode && (
                                        <span className="text-[10px] text-outline mt-0.5 block font-semibold">
                                                Nhanh ID: {order.nhanhOrderCode}
                                            </span>
                                    )}
                                </td>
                                <td className="px-6 py-4">
                                    <p className="font-bold text-on-surface">{order.customerName}</p>
                                    <p className="text-[10px] text-outline">{order.customerMobile}</p>
                                </td>
                                <td className="px-6 py-4 text-on-surface-variant font-semibold">
                                    {order.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                                </td>
                                <td className="px-6 py-4 text-right font-black text-on-surface text-sm">
                                    {formatCurrency(order.totalAmount)}
                                </td>
                                <td className="px-6 py-4 text-center">
                                        <span
                                            className={`inline-block px-2.5 py-1 rounded-full border text-[9px] font-black uppercase tracking-wider ${getSyncStatusColor(order.syncStatus)}`}>
                                            {getSyncStatusText(order.syncStatus)}
                                        </span>
                                </td>
                                <td className="px-6 py-4 text-center">
                                        <span
                                            className={`inline-block px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${getStatusColor(order.status)}`}>
                                            {getStatusText(order.status)}
                                        </span>
                                </td>
                                <td className="px-6 py-4 text-center">
                                    <Link to={`/admin/orders/${order.id}`}
                                          className="text-secondary hover:text-primary transition-colors inline-block p-1">
                                        <Eye className="w-5 h-5"/>
                                    </Link>
                                </td>
                            </tr>
                        ))}
                        {!isLoading && filteredOrders.length === 0 && (
                            <tr>
                                <td colSpan={7}
                                    className="px-6 py-16 text-center text-outline font-bold flex flex-col items-center justify-center">
                                    <FileText className="w-8 h-8 text-outline/30 mb-2"/>
                                    <span>Không tìm thấy đơn hàng nào.</span>
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
