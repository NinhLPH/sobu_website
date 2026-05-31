import {useEffect, useState} from 'react';
import {useParams, Link} from 'react-router-dom';
import {
    ArrowLeft,
    User,
    MapPin,
    Package,
    CreditCard,
    RefreshCw,
    AlertCircle,
    CheckCircle2,
    Loader2
} from 'lucide-react';
import {AdminWorkflowService} from '../../service/admin.service';
import {OrderResponseDto} from '../../interface/order.model';
import {formatCurrency} from "../../util/format";

export default function AdminOrderDetail() {
    const {id} = useParams();
    const [order, setOrder] = useState<OrderResponseDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSyncing, setIsSyncing] = useState(false);
    const [isRetrying, setIsRetrying] = useState(false);
    const [actionError, setActionError] = useState<string | null>(null);
    const [actionSuccess, setActionSuccess] = useState<string | null>(null);

    const fetchOrderDetail = async () => {
        if (!id) return;
        setIsLoading(true);
        setActionError(null);
        try {
            const data = await AdminWorkflowService.getOrderDetail(id);
            setOrder(data);
        } catch (err: any) {
            console.error('Error fetching admin order detail:', err);
            setActionError('Không thể tải thông tin chi tiết đơn hàng!');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchOrderDetail();
    }, [id]);

    const handleRetrySync = async () => {
        if (!id) return;
        setIsRetrying(true);
        setActionError(null);
        setActionSuccess(null);
        try {
            const res = await AdminWorkflowService.retryOrderSyncNhanh(id);
            if (res.syncStatus === 'SYNCED') {
                setActionSuccess('Đồng bộ đơn hàng lên Nhanh.vn thành công!');
            } else {
                setActionError(res.syncError || 'Đồng bộ thất bại. Vui lòng kiểm tra lại cấu hình Nhanh.vn!');
            }
            // Cập nhật state cục bộ từ kết quả trả về, không reload trang hay gọi API fetch lại
            setOrder(prev => prev ? {
                ...prev,
                syncStatus: res.syncStatus,
                nhanhOrderId: res.nhanhOrderId,
                nhanhOrderCode: res.nhanhOrderCode,
                syncError: res.syncError
            } : null);
        } catch (err: any) {
            setActionError(err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi đồng bộ đơn hàng!');
        } finally {
            setIsRetrying(false);
        }
    };

    if (isLoading && !order) {
        return (
            <div className="py-24 flex flex-col items-center justify-center">
                <Loader2 className="w-10 h-10 text-primary animate-spin mb-4"/>
                <p className="text-outline text-xs font-bold">Đang tải chi tiết đơn hàng...</p>
            </div>
        );
    }

    if (!order) {
        return (
            <div
                className="p-8 text-center text-on-surface-variant flex flex-col items-center justify-center min-h-[50vh]">
                <AlertCircle className="w-12 h-12 text-error mb-4"/>
                <h2 className="text-lg font-black text-on-surface uppercase">Không tìm thấy đơn hàng</h2>
                <Link to="/admin/orders" className="text-primary font-bold hover:underline mt-4 inline-block">
                    Quay lại danh sách đơn hàng
                </Link>
            </div>
        );
    }

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'NEW':
                return 'bg-amber-100 text-amber-800 border-amber-200';
            case 'PENDING':
                return 'bg-amber-100 text-amber-800 border-amber-200';
            case 'PROCESSING':
                return 'bg-blue-100 text-blue-800 border-blue-200';
            case 'SHIPPED':
                return 'bg-purple-100 text-purple-800 border-purple-200';
            case 'DELIVERED':
                return 'bg-green-100 text-green-800 border-green-200';
            case 'CANCELLED':
                return 'bg-red-100 text-red-800 border-red-200';
            default:
                return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'NEW':
                return 'Mới';
            case 'PENDING':
                return 'Chờ xác nhận';
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
                return 'Đã đồng bộ thành công';
            case 'FAILED':
                return 'Lỗi đồng bộ ERP';
            case 'PENDING':
                return 'Chờ đồng bộ';
            default:
                return sync;
        }
    };

    return (
        <div className="space-y-6">
            {/* Header Title Bar */}
            <div className="flex items-center gap-4">
                <Link to="/admin/orders"
                      className="p-2 bg-white rounded-full border border-outline-variant/30 hover:bg-surface-variant transition-colors"
                      title="Quay lại">
                    <ArrowLeft className="w-5 h-5 text-on-surface"/>
                </Link>
                <div>
                    <div className="flex flex-wrap items-center gap-3">
                        <h1 className="text-2xl font-black text-on-surface uppercase tracking-tight">Chi tiết đơn hàng
                            #{order.orderCode || order.id}</h1>
                        <span
                            className={`px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${getStatusColor(order.status)}`}>
                            {getStatusText(order.status)}
                        </span>
                    </div>
                    {order.nhanhOrderCode && (
                        <p className="text-[10px] text-outline font-bold mt-1">Mã đơn Nhanh.vn: <strong
                            className="text-on-surface">{order.nhanhOrderCode}</strong></p>
                    )}
                </div>
            </div>

            {/* Success / Error Alerts */}
            {actionSuccess && (
                <div
                    className="p-4 bg-green-50 border border-green-200 rounded-2xl text-green-800 text-xs font-bold flex gap-3 items-center">
                    <CheckCircle2 className="w-5 h-5 text-green-600 shrink-0"/>
                    <span>{actionSuccess}</span>
                </div>
            )}

            {actionError && (
                <div
                    className="p-4 bg-error/10 border border-error/20 rounded-2xl text-error text-xs font-bold flex gap-3 items-start">
                    <AlertCircle className="w-5 h-5 text-error shrink-0 mt-0.5"/>
                    <span>{actionError}</span>
                </div>
            )}

            {/* Sync Alert details if FAILED */}
            {order.syncStatus === 'FAILED' && order.syncError && (
                <div
                    className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-800 text-xs font-medium leading-relaxed">
                    <span className="font-black text-red-600 uppercase tracking-wide block mb-1">❌ Chi tiết lỗi đồng bộ ERP Nhanh.vn:</span>
                    {order.syncError}
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Left Side: Product table & Sync */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white rounded-2xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div
                            className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <Package className="w-5 h-5 text-primary"/>
                            <h2 className="font-black text-on-surface uppercase tracking-wider text-xs">Danh sách sản
                                phẩm</h2>
                        </div>
                        <div className="p-4">
                            <table className="w-full text-xs text-left">
                                <thead className="text-outline uppercase text-[10px] tracking-wider font-bold">
                                <tr>
                                    <th className="pb-3">Sản phẩm</th>
                                    <th className="pb-3 text-center">Nhanh SKU</th>
                                    <th className="pb-3 text-center">SL</th>
                                    <th className="pb-3 text-right">Đơn giá</th>
                                    <th className="pb-3 text-right">Thành tiền</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/20 font-bold">
                                {order.items?.map(item => (
                                    <tr key={item.id}>
                                        <td className="py-3 font-bold text-on-surface">{item.name}</td>
                                        <td className="py-3 text-center text-outline">{item.nhanhProductId || 'N/A'}</td>
                                        <td className="py-3 text-center text-on-surface">{item.quantity}</td>
                                        <td className="py-3 text-right text-on-surface">{formatCurrency(item.price)}</td>
                                        <td className="py-3 text-right font-black text-primary">{formatCurrency(item.price * item.quantity)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>

                            <div
                                className="mt-6 pt-4 border-t border-outline-variant/20 w-full sm:w-1/2 sm:ml-auto space-y-2 text-xs font-bold">
                                <div className="flex justify-between text-outline">
                                    <span>Tạm tính (Chi phí gốc):</span>
                                    <span className="text-on-surface">{formatCurrency(order.totalAmount)}</span>
                                </div>
                                <div className="flex justify-between text-outline">
                                    <span>Đã cọc (Deposit):</span>
                                    <span className="text-on-surface">{formatCurrency(order.depositAmount)}</span>
                                </div>
                                <div
                                    className="flex justify-between font-black text-primary text-base pt-2 border-t border-outline-variant/20">
                                    <span>Tổng thanh toán còn lại:</span>
                                    <span>{formatCurrency(order.totalAmount - order.depositAmount)}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* ERP Nhanh.vn Sync Card details */}
                    <div
                        className="bg-white rounded-2xl border border-outline-variant/30 shadow-sm overflow-hidden p-6 space-y-4">
                        <div className="flex justify-between items-center border-b border-surface-container pb-3">
                            <div className="flex items-center gap-2">
                                <RefreshCw className="w-5 h-5 text-primary"/>
                                <h3 className="font-black text-on-surface uppercase tracking-wider text-xs">Đồng bộ tích
                                    hợp ERP Nhanh.vn</h3>
                            </div>
                            <span
                                className={`px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-wider border ${getSyncStatusColor(order.syncStatus)}`}>
                                {getSyncStatusText(order.syncStatus)}
                            </span>
                        </div>
                        <p className="text-xs text-outline/80 leading-relaxed font-semibold">
                            Đơn hàng được ghi nhận đồng bộ trực tiếp lên hệ thống ERP Nhanh.vn để phân bổ kho hàng, theo
                            dõi đơn giao và in hóa đơn vận chuyển.
                        </p>

                        {/* Nhanh.vn details list */}
                        <div
                            className="bg-surface-container/30 rounded-xl p-4 space-y-3 font-bold border border-outline-variant/10 text-xs">
                            <div className="flex justify-between items-center">
                                <span className="text-outline">Trạng thái đồng bộ:</span>
                                <span
                                    className={`px-2 py-0.5 rounded text-[10px] uppercase font-black tracking-wider ${getSyncStatusColor(order.syncStatus)}`}>
                                    {order.syncStatus}
                                </span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-outline">ID hệ thống Nhanh:</span>
                                <span
                                    className="text-on-surface select-all">{order.nhanhOrderId || 'Chưa kết nối'}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-outline">Mã đơn Nhanh:</span>
                                <span
                                    className="text-on-surface select-all">{order.nhanhOrderCode || 'Chưa kết nối'}</span>
                            </div>
                            {order.syncError && (
                                <div className="pt-2 border-t border-outline-variant/20">
                                    <span className="text-error uppercase text-[10px] tracking-wide block mb-1">Lý do lỗi đồng bộ:</span>
                                    <p className="text-[11px] text-error leading-relaxed font-semibold bg-error/5 p-2.5 rounded-lg border border-error/10">
                                        {order.syncError}
                                    </p>
                                </div>
                            )}
                        </div>

                        {order.syncStatus === 'FAILED' && (
                            <button
                                onClick={handleRetrySync}
                                disabled={isRetrying}
                                className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform cursor-pointer flex items-center justify-center gap-1.5 disabled:opacity-50 disabled:pointer-events-none"
                            >
                                {isRetrying ? (
                                    <>
                                        <Loader2 className="w-4 h-4 animate-spin"/>
                                        <span>Đang gửi đồng bộ lên Nhanh.vn...</span>
                                    </>
                                ) : (
                                    <>
                                        <RefreshCw className="w-4 h-4"/>
                                        <span>Retry Đồng Bộ</span>
                                    </>
                                )}
                            </button>
                        )}
                    </div>
                </div>

                {/* Right Side: Customer info & delivery */}
                <div className="space-y-6">
                    <div className="bg-white rounded-2xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div
                            className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <User className="w-5 h-5 text-primary"/>
                            <h2 className="font-black text-on-surface uppercase tracking-wider text-xs">Thông tin khách
                                hàng</h2>
                        </div>
                        <div className="p-6 text-xs space-y-4 font-bold">
                            <div>
                                <p className="text-[10px] text-outline uppercase mb-1">Họ tên khách</p>
                                <p className="font-black text-on-surface text-sm">{order.customerName || 'N/A'}</p>
                            </div>
                            <div>
                                <p className="text-[10px] text-outline uppercase mb-1">Số điện thoại</p>
                                <p className="text-on-surface font-black">{order.customerMobile}</p>
                            </div>
                            {order.requestCode && (
                                <div>
                                    <p className="text-[10px] text-outline uppercase mb-1">Liên kết mã yêu cầu</p>
                                    <Link to={`/admin/requests`} className="text-primary font-black hover:underline">
                                        #{order.requestCode}
                                    </Link>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="bg-white rounded-2xl border border-outline-variant/30 shadow-sm overflow-hidden">
                        <div
                            className="p-4 border-b border-outline-variant/20 bg-surface-container-lowest flex items-center gap-2">
                            <MapPin className="w-5 h-5 text-primary"/>
                            <h2 className="font-black text-on-surface uppercase tracking-wider text-xs">Thông tin giao
                                hàng</h2>
                        </div>
                        <div className="p-6 text-xs space-y-4 font-bold">
                            <div>
                                <p className="text-[10px] text-outline uppercase mb-1">Địa chỉ nhận hàng</p>
                                <p className="text-on-surface leading-relaxed font-semibold">{order.customerAddress || 'Nhận tại SOBU Workshop'}</p>
                            </div>
                            <div>
                                <p className="text-[10px] text-outline uppercase mb-1">Thời gian đặt</p>
                                <p className="text-on-surface font-semibold">
                                    {order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : 'N/A'}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
