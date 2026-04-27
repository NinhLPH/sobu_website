import {useState} from 'react';
import {Eye, Search} from 'lucide-react';

import {useAdminStore} from '../../store/useAdminStore';
import {formatCurrency} from "../../util/format";

export default function AdminOrders() {
    const {orders, updateOrderStatus} = useAdminStore();
    const [searchTerm, setSearchTerm] = useState('');

    const filteredOrders = orders.filter(o =>
        o.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
        o.customerName.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const getStatusColor = (status: string) => {
        switch (status) {
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

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Đơn hàng</h1>
            </div>

            <div className="bg-white p-4 rounded-xl border border-outline-variant/30 flex items-center gap-3 shadow-sm">
                <Search className="text-outline w-5 h-5"/>
                <input
                    type="text"
                    placeholder="Tìm kiếm theo mã đơn hoặc tên khách..."
                    className="bg-transparent border-none outline-none w-full text-sm"
                    value={searchTerm}
                    onChange={e => setSearchTerm(e.target.value)}
                />
            </div>

            <div className="bg-white rounded-xl border border-outline-variant/30 overflow-hidden shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-surface-variant font-bold text-on-surface-variant">
                        <tr>
                            <th className="px-6 py-4">Mã đơn</th>
                            <th className="px-6 py-4">Khách hàng</th>
                            <th className="px-6 py-4">Ngày tạo</th>
                            <th className="px-6 py-4 text-right">Tổng tiền</th>
                            <th className="px-6 py-4 text-center">Trạng thái</th>
                            <th className="px-6 py-4 text-center">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredOrders.map(order => (
                            <tr key={order.id}
                                className="border-b border-outline-variant/20 hover:bg-surface-container-lowest/50">
                                <td className="px-6 py-4 font-bold text-primary">{order.id}</td>
                                <td className="px-6 py-4">
                                    <p className="font-bold text-on-surface">{order.customerName}</p>
                                    <p className="text-xs text-outline">{order.customerPhone}</p>
                                </td>
                                <td className="px-6 py-4 text-on-surface-variant">
                                    {new Date(order.createdAt).toLocaleDateString('vi-VN')}
                                </td>
                                <td className="px-6 py-4 text-right font-bold text-on-surface">
                                    {formatCurrency(order.total)}
                                </td>
                                <td className="px-6 py-4 text-center">
                                    <select
                                        className={`px-3 py-1 rounded-full text-xs font-bold font-body border-none cursor-pointer outline-none ${getStatusColor(order.status)}`}
                                        value={order.status}
                                        onChange={(e) => updateOrderStatus(order.id, e.target.value as any)}
                                    >
                                        <option value="PENDING">Chờ xác nhận</option>
                                        <option value="PROCESSING">Đang xử lý</option>
                                        <option value="SHIPPED">Đang giao</option>
                                        <option value="DELIVERED">Đã giao</option>
                                        <option value="CANCELLED">Đã hủy</option>
                                    </select>
                                </td>
                                <td className="px-6 py-4 text-center">
                                    <button
                                        className="text-secondary hover:text-primary transition-colors inline-block p-1">
                                        <Eye className="w-5 h-5"/>
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {filteredOrders.length === 0 && (
                            <tr>
                                <td colSpan={6} className="px-6 py-8 text-center text-on-surface-variant">
                                    Không tìm thấy đơn hàng nào.
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
