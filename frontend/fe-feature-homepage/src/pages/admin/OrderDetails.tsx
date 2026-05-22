import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, User, MapPin, Package, CreditCard } from 'lucide-react';
import { useAdminStore } from '../../store/useAdminStore';
import {formatCurrency} from "../../util/format";

export default function AdminOrderDetail() {
    const { id } = useParams();
    const { orders, updateOrderStatus } = useAdminStore();
    const order = orders.find(o => o.id === id);

    if (!order) {
        return (
            <div className="p-8 text-center text-on-surface-variant">
                Kh\u00f4ng t\u00ecm th\u1ea5y \u0111\u01a1n h\u00e0ng.
                <br />
                <Link to="/admin/orders" className="text-primary hover:underline mt-4 inline-block">
                    Quay l\u1ea1i danh s\u00e1ch \u0111\u01a1n h\u00e0ng
                </Link>
            </div>
        );
    }

    const getStatusColor = (status: string) => {
        switch(status) {
            case 'PENDING': return 'bg-amber-100 text-amber-800';
            case 'PROCESSING': return 'bg-blue-100 text-blue-800';
            case 'SHIPPED': return 'bg-purple-100 text-purple-800';
            case 'DELIVERED': return 'bg-green-100 text-green-800';
            case 'CANCELLED': return 'bg-red-100 text-red-800';
            default: return 'bg-gray-100 text-gray-800';
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-4">
                <Link to="/admin/orders" className="p-2 bg-white rounded-full border border-outline-variant/30 hover:bg-surface-variant transition-colors">
                    <ArrowLeft className="w-5 h-5 text-on-surface" />
                </Link>
                <h1 className="text-2xl font-bold text-on-surface">Chi tiết đơn hàng #{order.id}</h1>
                <div className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ml-auto ${getStatusColor(order.status)}`}>
                    {order.status}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white rounded-xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <Package className="w-5 h-5 text-primary" />
                            <h2 className="font-bold text-on-surface">Sản phẩm</h2>
                        </div>
                        <div className="p-4">
                            <table className="w-full text-sm text-left">
                                <thead className="text-outline uppercase text-[10px] tracking-wider">
                                <tr>
                                    <th className="pb-3">Sản phẩm</th>
                                    <th className="pb-3 text-center">SL</th>
                                    <th className="pb-3 text-right">Đơn giá</th>
                                    <th className="pb-3 text-right">Thành tiền</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/20">
                                {order.items.map(item => (
                                    <tr key={item.productId}>
                                        <td className="py-3 font-medium text-on-surface">{item.productName} <span className="text-xs text-outline font-normal ml-2">({item.productId})</span></td>
                                        <td className="py-3 text-center">{item.quantity}</td>
                                        <td className="py-3 text-right">{formatCurrency(item.price)}</td>
                                        <td className="py-3 text-right font-bold text-on-surface">{formatCurrency(item.price * item.quantity)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>

                            <div className="mt-6 pt-4 border-t border-outline-variant/20 w-1/2 ml-auto space-y-2 text-sm">
                                <div className="flex justify-between text-on-surface-variant">
                                    <span>Tạm tính:</span>
                                    <span>{formatCurrency(order.subtotal)}</span>
                                </div>
                                <div className="flex justify-between text-on-surface-variant">
                                    <span>Thuế VAT:</span>
                                    <span>{formatCurrency(order.tax)}</span>
                                </div>
                                <div className="flex justify-between text-on-surface-variant">
                                    <span>Phí giao hàng:</span>
                                    <span>{order.shippingFee === 0 ? 'Miễn phí' : formatCurrency(order.shippingFee)}</span>
                                </div>
                                <div className="flex justify-between font-black text-primary text-lg pt-2 border-t border-outline-variant/20">
                                    <span>Tổng cộng:</span>
                                    <span>{formatCurrency(order.total)}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white rounded-xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <CreditCard className="w-5 h-5 text-primary" />
                            <h2 className="font-bold text-on-surface">Cập nhật trạng thái</h2>
                        </div>
                        <div className="p-6">
                            <select
                                className="w-full bg-surface-container-low border border-outline-variant rounded-lg p-3 outline-none focus:ring-2 focus:ring-primary text-sm font-bold"
                                value={order.status}
                                onChange={(e) => updateOrderStatus(order.id, e.target.value as any)}
                            >
                                <option value="PENDING">Chờ xác nhận</option>
                                <option value="PROCESSING">Đang xử lí</option>
                                <option value="SHIPPED">Đang giao</option>
                                <option value="DELIVERED">Đã giao</option>
                                <option value="CANCELLED">Đã hủy</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div className="space-y-6">
                    <div className="bg-white rounded-xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <User className="w-5 h-5 text-primary" />
                            <h2 className="font-bold text-on-surface">Khách hàng</h2>
                        </div>
                        <div className="p-6 text-sm space-y-4">
                            <div>
                                <p className="text-xs text-outline uppercase font-bold mb-1">Họ tên</p>
                                <p className="font-bold text-on-surface">{order.customerName}</p>
                            </div>
                            <div>
                                <p className="text-xs text-outline uppercase font-bold mb-1">Số điện thoại</p>
                                <p className="text-on-surface">{order.customerPhone}</p>
                            </div>
                            <div>
                                <p className="text-xs text-outline uppercase font-bold mb-1">Email</p>
                                <p className="text-on-surface">{order.customerEmail}</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white rounded-xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <MapPin className="w-5 h-5 text-primary" />
                            <h2 className="font-bold text-on-surface">Giao hàng</h2>
                        </div>
                        <div className="p-6 text-sm space-y-4">
                            <div>
                                <p className="text-xs text-outline uppercase font-bold mb-1">Địa chỉ</p>
                                <p className="text-on-surface leading-relaxed">{order.shippingAddress}</p>
                            </div>
                            <div>
                                <p className="text-xs text-outline uppercase font-bold mb-1">Ngày đặt</p>
                                <p className="text-on-surface">{new Date(order.createdAt).toLocaleString('vi-VN')}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
