import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronRight, FileText, Plus, Eye, Calendar, DollarSign, Layers } from 'lucide-react';
import { useRequestStore } from '../store/useRequestStore';
import { formatCurrency } from '../util/format';

export default function MyRequests() {
    const navigate = useNavigate();
    const { myRequests, fetchMyRequests, isLoading, error } = useRequestStore();

    useEffect(() => {
        fetchMyRequests();
    }, [fetchMyRequests]);

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'PENDING':
                return 'bg-amber-50 text-amber-700 border-amber-200';
            case 'REVIEWING':
                return 'bg-blue-50 text-blue-700 border-blue-200';
            case 'SOURCING':
                return 'bg-purple-50 text-purple-700 border-purple-200';
            case 'WAITING_CUSTOMER':
                return 'bg-indigo-50 text-indigo-700 border-indigo-200';
            case 'APPROVED':
                return 'bg-green-50 text-green-700 border-green-200';
            case 'REJECTED':
                return 'bg-red-50 text-red-700 border-red-200';
            case 'CANCELLED':
                return 'bg-gray-50 text-gray-700 border-gray-200';
            default:
                return 'bg-gray-50 text-gray-700 border-gray-200';
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'PENDING': return 'Đang Chờ Duyệt';
            case 'REVIEWING': return 'Đang Xem Xét';
            case 'SOURCING': return 'Đang Tìm Nguồn';
            case 'WAITING_CUSTOMER': return 'Chờ Phản Hồi';
            case 'APPROVED': return 'Đã Đồng Ý';
            case 'REJECTED': return 'Từ Chối';
            case 'CANCELLED': return 'Đã Hủy';
            default: return status;
        }
    };

    const getTypeLabel = (type: string) => {
        switch (type) {
            case 'NORMAL': return 'Thông thường';
            case 'PREORDER': return 'Pre-order';
            case 'FINDING': return 'Tìm hàng';
            case 'CUSTOM': return 'Độ chế / Led / Sơn';
            default: return type;
        }
    };

    const getTypeColor = (type: string) => {
        switch (type) {
            case 'NORMAL': return 'bg-gray-100 text-gray-700';
            case 'PREORDER': return 'bg-purple-100 text-purple-700';
            case 'FINDING': return 'bg-amber-100 text-amber-700';
            case 'CUSTOM': return 'bg-blue-100 text-blue-700';
            default: return 'bg-gray-100 text-gray-700';
        }
    };

    return (
        <main className="max-w-screen-2xl mx-auto px-6 pt-32 pb-24 bg-surface">
            {/* Header & Breadcrumb */}
            <nav className="flex items-center gap-2 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5" />
                <span className="text-primary">Yêu cầu của tôi</span>
            </nav>

            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                <div>
                    <h1 className="text-3xl font-black tracking-tight mb-2 text-on-surface uppercase">Danh Sách Yêu Cầu</h1>
                    <p className="text-xs text-outline font-bold">Theo dõi và quản lý các yêu cầu tìm hàng, pre-order hoặc custom độ chế của bạn.</p>
                </div>
                
                <Link
                    to="/requests/new"
                    className="flex items-center gap-2 px-6 py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest shadow-md hover:scale-102 transition-transform cursor-pointer"
                >
                    <Plus className="w-4 h-4" /> Gửi yêu cầu mới
                </Link>
            </div>

            {error && (
                <div className="mb-6 p-4 rounded-2xl bg-error/10 border border-error/20 text-error text-xs font-bold">
                    {error}
                </div>
            )}

            {isLoading ? (
                <div className="py-24 flex flex-col items-center justify-center">
                    <Loader2 className="w-10 h-10 text-primary animate-spin mb-4" />
                    <p className="text-outline text-xs font-bold">Đang tải danh sách yêu cầu...</p>
                </div>
            ) : myRequests.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in">
                    {myRequests.map((req) => (
                        <div 
                            key={req.id}
                            onClick={() => navigate(`/requests/${req.id}`)}
                            className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 hover:shadow-md transition-all duration-300 cursor-pointer flex flex-col justify-between group"
                        >
                            <div>
                                <div className="flex justify-between items-start gap-4 mb-4">
                                    <span className="text-[10px] bg-surface-container-high text-on-surface font-black px-3 py-1 rounded-full uppercase tracking-wider">
                                        #{req.requestCode || req.id}
                                    </span>
                                    <span className={`text-[10px] font-black uppercase px-2.5 py-1 rounded-full border ${getStatusColor(req.status)}`}>
                                        {getStatusText(req.status)}
                                    </span>
                                </div>

                                <div className="mb-4">
                                    <span className={`text-[9px] font-black uppercase px-2 py-0.5 rounded-md ${getTypeColor(req.type)}`}>
                                        {getTypeLabel(req.type)}
                                    </span>
                                    <h3 className="font-black text-on-surface text-base mt-2 line-clamp-1 group-hover:text-primary transition-colors">
                                        {req.items && req.items.length > 0 ? req.items.map(item => item.name).join(', ') : 'Yêu cầu dịch vụ'}
                                    </h3>
                                    {req.customRequirements?.note && (
                                        <p className="text-xs text-outline/80 mt-1 line-clamp-2 leading-relaxed">
                                            {req.customRequirements.note}
                                        </p>
                                    )}
                                </div>
                            </div>

                            <div className="border-t border-surface-container/60 pt-4 mt-4 flex flex-col gap-2.5 text-xs font-bold text-outline">
                                <div className="flex justify-between items-center">
                                    <span className="flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5" /> Ngày tạo:</span>
                                    <span className="text-on-surface">{new Date(req.createdAt).toLocaleDateString('vi-VN')}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="flex items-center gap-1.5"><DollarSign className="w-3.5 h-3.5" /> Báo giá:</span>
                                    <span className="text-primary font-black text-sm">
                                        {req.totalAmount > 0 ? formatCurrency(req.totalAmount) : 'Chờ báo giá'}
                                    </span>
                                </div>
                                
                                <button className="mt-2 w-full py-2 bg-surface-container text-on-surface hover:bg-primary/10 hover:text-primary rounded-xl text-[10px] font-black uppercase tracking-wider flex items-center justify-center gap-1.5 transition-colors">
                                    <Eye className="w-3.5 h-3.5" /> Xem chi tiết
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                /* EMPTY STATE */
                <div className="bg-surface-container-lowest rounded-[2rem] p-16 text-center border border-dashed border-outline-variant/30 flex flex-col items-center justify-center max-w-xl mx-auto">
                    <FileText className="w-12 h-12 text-outline/40 mb-4 stroke-[1.5]" />
                    <h3 className="text-xl font-black mb-2 text-on-surface uppercase tracking-tight">Chưa có yêu cầu nào</h3>
                    <p className="text-outline/80 text-sm leading-relaxed max-w-xs mb-8">
                        Bạn chưa gửi bất kỳ yêu cầu dịch vụ đặc biệt nào. Hãy nhấn nút bên dưới để tạo yêu cầu tìm hàng hoặc ráp custom.
                    </p>
                    <Link
                        to="/requests/new"
                        className="px-8 py-3 bg-primary text-white rounded-full font-bold shadow-md hover:scale-105 transition-transform"
                    >
                        Tạo yêu cầu ngay
                    </Link>
                </div>
            )}
        </main>
    );
}

const Loader2 = ({ className, ...props }: React.SVGProps<SVGSVGElement>) => (
    <svg
        className={`animate-spin ${className}`}
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        {...props}
    >
        <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
        />
        <path
            className="opacity-75"
            fill="currentColor"
            pathLength="360"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
        />
    </svg>
);
