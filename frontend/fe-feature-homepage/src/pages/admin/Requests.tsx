import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, ShieldAlert, CheckCircle, SlidersHorizontal, Loader2, FileText, CheckCircle2, ChevronLeft, ChevronRight, PackageCheck } from 'lucide-react';
import { useRequestStore } from '../../store/useRequestStore';
import { formatCurrency } from '../../util/format';
import { RequestStatus } from '../../enum/union-types';
import { ToastService } from '../../service/toast.service';
import { getPublicImageUrl } from '../../utils/file-url';

const allowedTransitions: Record<RequestStatus, RequestStatus[]> = {
    PENDING: ['REVIEWING', 'SOURCING', 'REJECTED', 'CANCELLED'],
    REVIEWING: ['SOURCING', 'WAITING_CUSTOMER', 'APPROVED', 'REJECTED', 'CANCELLED'],
    SOURCING: ['REVIEWING', 'WAITING_CUSTOMER', 'APPROVED', 'REJECTED', 'CANCELLED'],
    WAITING_CUSTOMER: ['REVIEWING', 'SOURCING', 'APPROVED', 'REJECTED', 'CANCELLED'],
    APPROVED: ['CANCELLED'],
    REJECTED: [],
    CANCELLED: []
};

export default function AdminRequests() {
    const { 
        adminRequests, 
        adminRequestsPage,
        currentRequestDetail, 
        fetchAdminRequests, 
        getRequestDetail, 
        processRequestAction, 
        isLoading, 
        isSubmitting, 
        error,
        clearError,
        clearCurrentDetail
    } = useRequestStore();

    // Filters
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState<string>('ALL');
    const [typeFilter, setTypeFilter] = useState<string>('ALL');
    const [page, setPage] = useState(0);

    // Process Form states (inside Modal)
    const [isProcessModalOpen, setIsProcessModalOpen] = useState(false);
    const [processAction, setProcessAction] = useState<RequestStatus>('REVIEWING');
    const [processNote, setProcessNote] = useState('');
    const [processDeposit, setProcessDeposit] = useState('');
    const [processError, setProcessError] = useState<string | null>(null);

    useEffect(() => {
        fetchAdminRequests({ page, size: 10 });
    }, [fetchAdminRequests, page]);

    useEffect(() => {
        return () => {
            clearCurrentDetail();
        };
    }, [clearCurrentDetail]);

    useEffect(() => {
        if (error) {
            ToastService.error(error);
        }
    }, [error]);

    // Handle selecting request detail
    const handleSelectRequest = (id: string | number) => {
        clearError();
        setProcessError(null);
        getRequestDetail(id, 'admin');
    };

    // Filter requests
    const filteredRequests = adminRequests.filter(r => {
        const matchesSearch = 
            String(r.id).toLowerCase().includes(searchTerm.toLowerCase()) ||
            (r.customerPhone && r.customerPhone.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (r.requestCode && r.requestCode.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesStatus = statusFilter === 'ALL' || r.status === statusFilter;
        
        const matchesType = typeFilter === 'ALL' || r.type === typeFilter;

        return matchesSearch && matchesStatus && matchesType;
    });

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'PENDING': return 'bg-amber-50 text-amber-700 border-amber-200';
            case 'REVIEWING': return 'bg-blue-50 text-blue-700 border-blue-200';
            case 'SOURCING': return 'bg-purple-50 text-purple-700 border-purple-200';
            case 'WAITING_CUSTOMER': return 'bg-indigo-50 text-indigo-700 border-indigo-200';
            case 'APPROVED': return 'bg-green-50 text-green-700 border-green-200';
            case 'REJECTED': return 'bg-red-50 text-red-700 border-red-200';
            case 'CANCELLED': return 'bg-gray-50 text-gray-700 border-gray-200';
            default: return 'bg-gray-50 text-gray-700 border-gray-200';
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'PENDING': return 'Chờ Duyệt';
            case 'REVIEWING': return 'Đang Xem Xét';
            case 'SOURCING': return 'Đang Tìm Nguồn';
            case 'WAITING_CUSTOMER': return 'Chờ Khách';
            case 'APPROVED': return 'Đã Duyệt';
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
            case 'CUSTOM': return 'Độ ráp custom';
            default: return type;
        }
    };

    const handleOpenProcessModal = () => {
        if (!currentRequestDetail) return;
        const transitions = allowedTransitions[currentRequestDetail.status];
        if (transitions.length === 0) return;
        setProcessAction(transitions[0]);
        setProcessNote('');
        setProcessDeposit(currentRequestDetail.depositAmount > 0 ? currentRequestDetail.depositAmount.toString() : '');
        setProcessError(null);
        setIsProcessModalOpen(true);
    };

    const handleProcessSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!currentRequestDetail) return;

        setProcessError(null);
        clearError();

        const requiresDeposit = processAction === 'WAITING_CUSTOMER' || processAction === 'APPROVED';
        const depositInput = processDeposit.trim();

        if (requiresDeposit && !depositInput) {
            setProcessError('Vui lòng nhập số tiền cọc cho trạng thái đã chọn.');
            return;
        }

        const depositVal = depositInput ? Number(depositInput) : undefined;
        if (depositVal !== undefined && (!Number.isFinite(depositVal) || depositVal < 0)) {
            setProcessError('Tiền cọc phải là số lớn hơn hoặc bằng 0.');
            return;
        }

        try {
            const processedRequest = await processRequestAction(currentRequestDetail.id, {
                targetStatus: processAction,
                note: processNote.trim() || undefined,
                depositAmount: requiresDeposit ? depositVal : undefined
            });
            setIsProcessModalOpen(false);
            ToastService.success(
                processedRequest.status === 'APPROVED'
                    ? 'Đã duyệt yêu cầu và tạo đơn hàng.'
                    : 'Đã cập nhật trạng thái yêu cầu.'
            );
            await fetchAdminRequests({ page, size: 10 });
        } catch (err: any) {
            const message = err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi xử lý yêu cầu!';
            setProcessError(message);
        }
    };

    const transitions = currentRequestDetail
        ? allowedTransitions[currentRequestDetail.status]
        : [];
    const requiresDeposit = processAction === 'WAITING_CUSTOMER' || processAction === 'APPROVED';

    return (
        <div className="pt-6 space-y-6 flex flex-col md:flex-row gap-6 relative">
            
            {/* Left side: List with Filters */}
            <div className="flex-1 space-y-4">
                <div className="flex justify-between items-center">
                    <h1 className="text-2xl font-black text-on-surface uppercase tracking-tight">Yêu Cầu Từ Khách Hàng</h1>
                </div>

                {/* Filter Toolbar */}
                <div className="bg-white p-4 rounded-2xl border border-outline-variant/30 shadow-sm space-y-3">
                    <div className="flex items-center gap-3 bg-surface-container rounded-xl px-4 py-2.5">
                        <Search className="text-outline w-4 h-4"/>
                        <input
                            type="text"
                            placeholder="Tìm kiếm mã, SĐT, hoặc ID yêu cầu..."
                            className="bg-transparent border-none outline-none w-full text-xs font-semibold"
                            value={searchTerm}
                            onChange={e => setSearchTerm(e.target.value)}
                        />
                    </div>
                    
                    <div className="flex flex-wrap gap-3">
                        <div className="flex items-center gap-1.5 text-xs font-bold text-outline">
                            <SlidersHorizontal className="w-3.5 h-3.5" />
                            <span>Trạng thái:</span>
                        </div>
                        <select
                            value={statusFilter}
                            onChange={e => setStatusFilter(e.target.value)}
                            className="bg-surface-container rounded-lg px-3 py-1.5 text-xs font-bold text-on-surface cursor-pointer outline-none border-none"
                        >
                            <option value="ALL">Tất cả trạng thái</option>
                            <option value="PENDING">Chờ duyệt (PENDING)</option>
                            <option value="REVIEWING">Đang xem xét (REVIEWING)</option>
                            <option value="SOURCING">Đang tìm nguồn (SOURCING)</option>
                            <option value="WAITING_CUSTOMER">Chờ khách (WAITING_CUSTOMER)</option>
                            <option value="APPROVED">Đã duyệt (APPROVED)</option>
                            <option value="REJECTED">Từ chối (REJECTED)</option>
                            <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                        </select>

                        <div className="flex items-center gap-1.5 text-xs font-bold text-outline ml-2">
                            <span>Phân loại:</span>
                        </div>
                        <select
                            value={typeFilter}
                            onChange={e => setTypeFilter(e.target.value)}
                            className="bg-surface-container rounded-lg px-3 py-1.5 text-xs font-bold text-on-surface cursor-pointer outline-none border-none"
                        >
                            <option value="ALL">Tất cả loại</option>
                            <option value="NORMAL">Thông thường (NORMAL)</option>
                            <option value="PREORDER">Pre-order (PREORDER)</option>
                            <option value="FINDING">Tìm hàng (FINDING)</option>
                            <option value="CUSTOM">Ráp độ custom (CUSTOM)</option>
                        </select>
                    </div>
                </div>

                {error && (
                    <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">
                        {error}
                    </div>
                )}

                {/* List contents */}
                {isLoading && adminRequests.length === 0 ? (
                    <div className="py-24 flex flex-col items-center justify-center bg-white rounded-2xl border border-outline-variant/30 shadow-sm">
                        <Loader2 className="w-8 h-8 text-primary animate-spin mb-3" />
                        <p className="text-outline text-xs font-bold">Đang tải danh sách yêu cầu...</p>
                    </div>
                ) : (
                    <div className="space-y-3">
                        {filteredRequests.map(req => (
                            <div
                                key={req.id}
                                onClick={() => handleSelectRequest(req.id)}
                                className={`bg-white p-5 rounded-2xl border cursor-pointer transition-all ${
                                    currentRequestDetail?.id === req.id 
                                        ? 'border-primary shadow-md scale-[1.01]' 
                                        : 'border-outline-variant/30 hover:border-primary/50 hover:shadow-sm'
                                }`}
                            >
                                <div className="flex justify-between items-start mb-3">
                                    <div>
                                        <span className="text-[9px] bg-primary/10 text-primary font-black px-2.5 py-0.5 rounded-full uppercase tracking-wider">
                                            #{req.requestCode || req.id}
                                        </span>
                                        <h3 className="font-black text-on-surface text-sm mt-2">{req.customerPhone}</h3>
                                    </div>
                                    <span className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full border ${getStatusColor(req.status)}`}>
                                        {getStatusText(req.status)}
                                    </span>
                                </div>
                                
                                <p className="text-xs text-outline/80 line-clamp-1 mb-3">
                                    Loại: <span className="font-bold text-on-surface">{getTypeLabel(req.type)}</span> &bull; Sản phẩm: <span className="font-bold text-on-surface">{req.items?.map(i => i.name).join(', ') || 'N/A'}</span>
                                </p>

                                <div className="text-[10px] text-outline/60 flex justify-between border-t border-surface-container pt-3">
                                    <span>Tổng: <strong className="text-primary font-black">{req.totalAmount > 0 ? formatCurrency(req.totalAmount) : 'Chờ báo giá'}</strong></span>
                                    <span>{new Date(req.createdAt).toLocaleString('vi-VN')}</span>
                                </div>
                            </div>
                        ))}

                        {filteredRequests.length === 0 && (
                            <div className="p-16 text-center text-outline bg-white rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col items-center justify-center">
                                <FileText className="w-10 h-10 text-outline/30 mb-3" />
                                <h3 className="font-black text-on-surface text-base uppercase mb-1">Không tìm thấy yêu cầu</h3>
                                <p className="text-xs text-outline/80">Vui lòng thử thay đổi từ khóa hoặc bộ lọc trạng thái.</p>
                            </div>
                        )}

                        {adminRequestsPage.totalPages > 1 && (
                            <div className="flex items-center justify-between rounded-xl border border-outline-variant/30 bg-white px-4 py-3 shadow-sm">
                                <span className="text-xs font-bold text-outline">
                                    Trang {adminRequestsPage.pageNumber + 1}/{adminRequestsPage.totalPages}
                                </span>
                                <div className="flex gap-2">
                                    <button
                                        type="button"
                                        onClick={() => setPage((current) => Math.max(0, current - 1))}
                                        disabled={!adminRequestsPage.hasPrevious || isLoading}
                                        className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                        aria-label="Trang trước"
                                    >
                                        <ChevronLeft className="h-4 w-4" />
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setPage((current) => current + 1)}
                                        disabled={!adminRequestsPage.hasNext || isLoading}
                                        className="rounded-lg bg-surface-container p-2 text-on-surface disabled:opacity-40"
                                        aria-label="Trang sau"
                                    >
                                        <ChevronRight className="h-4 w-4" />
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Right side: Detail & Process Request */}
            <div className="w-full md:w-96 flex-shrink-0">
                {currentRequestDetail ? (
                    <div className="bg-white p-6 rounded-2xl border border-outline-variant/30 shadow-md sticky top-28 space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
                        <div className="flex justify-between items-center border-b border-surface-container pb-4">
                            <h2 className="text-base font-black text-on-surface uppercase tracking-tight">Chi Tiết Yêu Cầu</h2>
                            <span className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-md border ${getStatusColor(currentRequestDetail.status)}`}>
                                {getStatusText(currentRequestDetail.status)}
                            </span>
                        </div>

                        <div className="space-y-4 text-xs">
                            <div>
                                <p className="text-[10px] text-outline font-black uppercase mb-1">Khách hàng liên hệ</p>
                                <p className="font-black text-on-surface text-sm">{currentRequestDetail.customerPhone}</p>
                                <p className="text-[10px] text-outline/70 font-semibold mt-0.5">Phân loại: {getTypeLabel(currentRequestDetail.type)}</p>
                            </div>

                            <div>
                                <p className="text-[10px] text-outline font-black uppercase mb-2">Danh sách mô hình yêu cầu</p>
                                <div className="space-y-2">
                                    {currentRequestDetail.items?.map((item, idx) => (
                                        <div key={idx} className="p-3 bg-surface-container rounded-xl font-bold flex justify-between items-center gap-2">
                                            <div>
                                                <p className="text-on-surface line-clamp-2 leading-tight">{item.name}</p>
                                                {item.note && <p className="text-[9px] text-outline font-medium mt-0.5">Ghi chú: {item.note}</p>}
                                            </div>
                                            <span className="shrink-0 text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded-full font-black">
                                                x{item.quantity}
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {currentRequestDetail.customRequirements && (
                                <div>
                                    <p className="text-[10px] text-outline font-black uppercase mb-1">Yêu cầu chi tiết từ khách</p>
                                    <p className="p-3 bg-surface-container rounded-xl text-on-surface-variant leading-relaxed">
                                        {currentRequestDetail.customRequirements}
                                    </p>
                                </div>
                            )}

                            {/* Attachments list */}
                            {currentRequestDetail.attachments && currentRequestDetail.attachments.length > 0 && (
                                <div>
                                    <p className="text-[10px] text-outline font-black uppercase mb-2">Tệp ảnh đính kèm minh họa</p>
                                    <div className="grid grid-cols-3 gap-2">
                                        {currentRequestDetail.attachments.map((att, idx) => (
                                            <a 
                                                key={idx} 
                                                href={getPublicImageUrl(att.url)}
                                                target="_blank" 
                                                rel="noopener noreferrer"
                                                className="aspect-square bg-surface-container rounded-lg overflow-hidden border border-outline-variant/20 p-0.5 flex items-center justify-center hover:scale-105 transition-transform"
                                                title="Mở ảnh gốc"
                                            >
                                                <img
                                                    src={getPublicImageUrl(att.url)}
                                                    alt={`Attach ${idx}`}
                                                    className="w-full h-full object-contain rounded-md"
                                                />
                                            </a>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Quotation / Price details */}
                            <div className="border-t border-surface-container pt-4 space-y-2">
                                <div className="flex justify-between items-center">
                                    <span className="text-outline font-bold">Giá cọc tối thiểu:</span>
                                    <span className="text-on-surface font-black">
                                        {currentRequestDetail.depositAmount > 0 ? formatCurrency(currentRequestDetail.depositAmount) : 'Chưa định giá'}
                                    </span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-outline font-bold">Tổng chi phí dự kiến:</span>
                                    <span className="text-primary font-black text-sm">
                                        {currentRequestDetail.totalAmount > 0 ? formatCurrency(currentRequestDetail.totalAmount) : 'Chưa tính cọc'}
                                    </span>
                                </div>
                            </div>

                            {currentRequestDetail.status === 'APPROVED' &&
                                (currentRequestDetail.nhanhOrderCode || currentRequestDetail.nhanhOrderId) && (
                                <div className="rounded-xl border border-green-200 bg-green-50 p-3 text-green-900">
                                    <div className="flex items-start gap-2">
                                        <PackageCheck className="mt-0.5 h-4 w-4 shrink-0 text-green-600" />
                                        <div>
                                            <p className="text-[10px] font-black uppercase tracking-wide text-green-700">
                                                Đã tạo đơn hàng
                                            </p>
                                            <p className="mt-1 font-black">
                                                {currentRequestDetail.nhanhOrderCode || currentRequestDetail.nhanhOrderId}
                                            </p>
                                        </div>
                                    </div>
                                    <Link
                                        to="/admin/orders"
                                        className="mt-3 inline-flex text-[10px] font-black uppercase text-primary hover:underline"
                                    >
                                        Xem danh sách đơn hàng
                                    </Link>
                                </div>
                            )}

                            {/* Process Action Trigger */}
                            {transitions.length > 0 ? (
                                <button
                                    type="button"
                                    onClick={handleOpenProcessModal}
                                    className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform mt-2 cursor-pointer flex items-center justify-center gap-1.5"
                                >
                                    <CheckCircle2 className="w-4 h-4" /> Cập nhật trạng thái
                                </button>
                            ) : (
                                <div className="rounded-xl bg-surface-container p-3 text-center text-[10px] font-black uppercase text-outline">
                                    Yêu cầu đã kết thúc xử lý
                                </div>
                            )}
                        </div>
                    </div>
                ) : isLoading ? (
                    <div className="bg-white p-8 rounded-2xl border border-outline-variant/30 shadow-sm text-center py-20 sticky top-28">
                        <Loader2 className="mx-auto mb-3 h-8 w-8 animate-spin text-primary" />
                        <p className="text-xs font-bold text-outline">Đang tải chi tiết yêu cầu...</p>
                    </div>
                ) : (
                    <div className="bg-white p-8 rounded-2xl border border-outline-variant/30 shadow-sm text-center py-20 sticky top-28 flex flex-col items-center justify-center">
                        <ShieldAlert className="w-10 h-10 text-outline/30 mb-3" />
                        <h3 className="font-black text-on-surface text-sm uppercase mb-1">Chưa chọn yêu cầu</h3>
                        <p className="text-xs text-outline/80 leading-relaxed max-w-[200px] mx-auto">Chọn một yêu cầu bên trái để xem nội dung chi tiết và phê duyệt.</p>
                    </div>
                )}
            </div>

            {/* --- PROCESS REQUEST MODAL OVERLAY --- */}
            {isProcessModalOpen && currentRequestDetail && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    {/* Backdrop */}
                    <div 
                        className="absolute inset-0 bg-black/40 backdrop-blur-xs cursor-pointer"
                        onClick={() => setIsProcessModalOpen(false)}
                    />
                    
                    {/* Modal body */}
                    <form 
                        onSubmit={handleProcessSubmit}
                        className="relative w-full max-w-md bg-white rounded-3xl shadow-xl border border-outline-variant/30 overflow-hidden z-10 animate-in fade-in zoom-in-95 p-6 space-y-4"
                    >
                        <div className="flex justify-between items-center border-b border-surface-container pb-3">
                            <h3 className="text-sm font-black text-on-surface uppercase tracking-tight">Duyệt Yêu Cầu #{currentRequestDetail.requestCode || currentRequestDetail.id}</h3>
                            <button 
                                type="button" 
                                onClick={() => setIsProcessModalOpen(false)}
                                className="text-outline hover:text-error transition-colors font-bold text-xs"
                            >
                                Đóng
                            </button>
                        </div>

                        {processError && (
                            <div className="p-3.5 rounded-xl bg-error/10 border border-error/20 text-error text-xs font-bold">
                                {processError}
                            </div>
                        )}

                        <div>
                            <label className="block text-[10px] font-black text-outline uppercase tracking-wider mb-2">Chuyển trạng thái yêu cầu</label>
                            <select
                                value={processAction}
                                onChange={(e) => {
                                    setProcessAction(e.target.value as RequestStatus);
                                    setProcessError(null);
                                }}
                                className="w-full bg-surface-container rounded-xl px-3 py-2.5 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent text-on-surface cursor-pointer"
                            >
                                {transitions.map((status) => (
                                    <option key={status} value={status}>
                                        {getStatusText(status)} ({status})
                                    </option>
                                ))}
                            </select>
                        </div>

                        {requiresDeposit && (
                            <div>
                                <label className="block text-[10px] font-black text-outline tracking-wider mb-2 uppercase">
                                    Số tiền cọc (VND)
                                </label>
                                <input
                                    type="number"
                                    min="0"
                                    step="1000"
                                    value={processDeposit}
                                    onChange={(e) => {
                                        setProcessDeposit(e.target.value);
                                        setProcessError(null);
                                    }}
                                    placeholder="Ví dụ: 500000"
                                    required
                                    className="w-full bg-surface-container rounded-xl px-3 py-2.5 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent text-on-surface placeholder:text-outline/40"
                                />
                                <p className="mt-1.5 text-[10px] font-semibold text-outline">
                                    Tiền cọc sẽ được cập nhật trước khi backend chuyển yêu cầu thành đơn hàng.
                                </p>
                            </div>
                        )}

                        <div>
                            <label className="block text-[10px] font-black text-outline tracking-wider mb-2 uppercase">Ghi chú xử lý / Phản hồi khách</label>
                            <textarea
                                value={processNote}
                                onChange={(e) => setProcessNote(e.target.value)}
                                placeholder="Nhập tin nhắn phản hồi báo giá, mô tả sản phẩm tìm được..."
                                className="w-full bg-surface-container rounded-xl px-3 py-2.5 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent text-on-surface placeholder:text-outline/40 min-h-[90px]"
                            />
                        </div>

                        {/* Actions buttons */}
                        <div className="flex gap-3 pt-2">
                            <button
                                type="button"
                                onClick={() => setIsProcessModalOpen(false)}
                                className="flex-1 py-3 bg-surface-container text-on-surface rounded-xl text-xs font-black uppercase tracking-wider text-center hover:bg-surface-container-high transition-colors"
                            >
                                Hủy bỏ
                            </button>
                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="flex-1 py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl text-xs font-black uppercase tracking-wider text-center shadow-md hover:scale-[1.01] transition-transform flex items-center justify-center gap-1.5"
                            >
                                {isSubmitting ? (
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                ) : (
                                    <CheckCircle className="w-4 h-4" />
                                )}
                                <span>Phê Duyệt</span>
                            </button>
                        </div>
                    </form>

                    {/* Loading overlay during submits */}
                    {isSubmitting && (
                        <div className="absolute inset-0 bg-white/40 backdrop-blur-xs flex items-center justify-center z-25">
                            <Loader2 className="w-10 h-10 text-primary animate-spin" />
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
