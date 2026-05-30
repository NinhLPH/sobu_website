import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronRight, ArrowLeft, Loader2, Save, Trash2, Plus, Upload, CheckCircle2, AlertCircle } from 'lucide-react';
import { useRequestStore } from '../store/useRequestStore';
import { FileService } from '../service/file.service';
import { formatCurrency } from '../util/format';
import { RequestType } from '../enum/union-types';

export default function RequestDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { currentRequestDetail, getRequestDetail, updateRequestAction, isLoading, isSubmitting, error, clearError } = useRequestStore();

    // Local form states (synced from currentRequestDetail)
    const [phone, setPhone] = useState('');
    const [type, setType] = useState<RequestType>('NORMAL');
    const [requirements, setRequirements] = useState('');
    const [items, setItems] = useState<{ name: string; quantity: number; note: string }[]>([]);
    const [attachments, setAttachments] = useState<string[]>([]);
    
    // UI Helpers
    const [uploadingFiles, setUploadingFiles] = useState(false);
    const [localError, setLocalError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    useEffect(() => {
        if (id) {
            getRequestDetail(id, 'user');
        }
    }, [id, getRequestDetail]);

    // Populate local states when API detail resolves
    useEffect(() => {
        if (currentRequestDetail) {
            setPhone(currentRequestDetail.customerPhone || '');
            setType(currentRequestDetail.type || 'NORMAL');
            setRequirements(currentRequestDetail.customRequirements?.note || '');
            setItems(currentRequestDetail.items?.map(item => ({
                name: item.name,
                quantity: item.quantity,
                note: item.note || ''
            })) || []);
            setAttachments(currentRequestDetail.attachments?.map(att => att.url) || []);
            setSuccessMessage(null);
            setLocalError(null);
        }
    }, [currentRequestDetail]);

    if (isLoading) {
        return (
            <main className="max-w-screen-2xl mx-auto px-6 pt-32 pb-24 bg-surface flex flex-col items-center justify-center min-h-[50vh]">
                <Loader2 className="w-10 h-10 text-primary animate-spin" />
                <p className="text-outline text-xs font-bold mt-4">Đang tải chi tiết yêu cầu...</p>
            </main>
        );
    }

    if (!currentRequestDetail) {
        return (
            <main className="max-w-screen-2xl mx-auto px-6 pt-32 pb-24 bg-surface flex flex-col items-center justify-center min-h-[50vh] text-center">
                <AlertCircle className="w-12 h-12 text-error mb-4" />
                <h1 className="text-xl font-black text-on-surface uppercase">Không tìm thấy yêu cầu</h1>
                <p className="text-outline text-sm mt-2 mb-6">Yêu cầu này không tồn tại hoặc bạn không có quyền xem.</p>
                <Link to="/requests" className="px-6 py-2.5 bg-primary text-white rounded-full font-bold text-xs uppercase shadow-sm">
                    Quay lại danh sách
                </Link>
            </main>
        );
    }

    const isPending = currentRequestDetail.status === 'PENDING';

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
            case 'CUSTOM': return 'Độ chế / ráp custom';
            default: return type;
        }
    };

    // Item list handlers (only allowed if pending)
    const handleAddItem = () => {
        if (!isPending) return;
        setItems(prev => [...prev, { name: '', quantity: 1, note: '' }]);
    };

    const handleRemoveItem = (index: number) => {
        if (!isPending || items.length === 1) return;
        setItems(prev => prev.filter((_, i) => i !== index));
    };

    const handleItemChange = (index: number, field: string, value: any) => {
        if (!isPending) return;
        setItems(prev => prev.map((item, i) => {
            if (i === index) {
                return { ...item, [field]: value };
            }
            return item;
        }));
    };

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!isPending) return;
        const files = e.target.files;
        if (!files || files.length === 0) return;

        setUploadingFiles(true);
        setLocalError(null);
        setSuccessMessage(null);

        try {
            const uploadedUrls: string[] = [];
            for (let i = 0; i < files.length; i++) {
                const res = await FileService.uploadFile(files[i], 'requests');
                if (res && res.url) {
                    uploadedUrls.push(res.url);
                }
            }
            setAttachments(prev => [...prev, ...uploadedUrls]);
        } catch (err: any) {
            setLocalError('Tải tệp hình ảnh lên thất bại!');
        } finally {
            setUploadingFiles(false);
        }
    };

    const handleRemoveAttachment = (index: number) => {
        if (!isPending) return;
        setAttachments(prev => prev.filter((_, i) => i !== index));
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!isPending || !id) return;

        clearError();
        setLocalError(null);
        setSuccessMessage(null);

        if (!phone.trim()) {
            setLocalError('Số điện thoại liên hệ không được bỏ trống!');
            return;
        }

        const validItems = items.filter(item => item.name.trim());
        if (validItems.length === 0) {
            setLocalError('Danh sách sản phẩm cần ít nhất 1 mặt hàng có tên!');
            return;
        }

        const payload = {
            customerPhone: phone,
            type,
            customRequirements: requirements ? { note: requirements } : null,
            items: validItems.map(item => ({
                name: item.name,
                quantity: item.quantity,
                note: item.note
            })),
            attachments
        };

        try {
            await updateRequestAction(id, payload);
            setSuccessMessage('Lưu thông tin thay đổi yêu cầu thành công!');
        } catch (err) {
            console.error('Update request error:', err);
        }
    };

    return (
        <main className="max-w-4xl mx-auto px-6 pt-32 pb-24 bg-surface">
            {/* Breadcrumb */}
            <nav className="flex items-center gap-2 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5" />
                <Link to="/requests" className="hover:text-primary transition-colors">Yêu cầu</Link>
                <ChevronRight className="w-3.5 h-3.5" />
                <span className="text-primary">Chi tiết yêu cầu</span>
            </nav>

            {/* Title Bar */}
            <div className="flex items-center gap-4 mb-8">
                <button 
                    onClick={() => navigate('/requests')}
                    className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-on-surface hover:bg-surface-container-high transition-colors cursor-pointer"
                    title="Quay lại"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <div className="flex-1">
                    <div className="flex flex-wrap items-center gap-3">
                        <h1 className="text-2xl font-black text-on-surface uppercase tracking-tight">
                            Yêu Cầu #{currentRequestDetail.requestCode || currentRequestDetail.id}
                        </h1>
                        <span className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full border ${getStatusColor(currentRequestDetail.status)}`}>
                            {getStatusText(currentRequestDetail.status)}
                        </span>
                    </div>
                    <p className="text-xs text-outline font-bold mt-1">
                        Ngày khởi tạo: {new Date(currentRequestDetail.createdAt).toLocaleString('vi-VN')}
                    </p>
                </div>
            </div>

            {/* Notifications */}
            {successMessage && (
                <div className="mb-6 p-4 rounded-2xl bg-green-50 border border-green-200 text-green-800 text-xs font-bold flex gap-3 items-center">
                    <CheckCircle2 className="w-5 h-5 text-green-600 shrink-0" />
                    <span>{successMessage}</span>
                </div>
            )}

            {(error || localError) && (
                <div className="mb-6 p-4 rounded-2xl bg-error/10 border border-error/20 text-error text-xs font-bold flex gap-3 items-start">
                    <AlertCircle className="w-5 h-5 text-error shrink-0 mt-0.5" />
                    <span>{localError || error}</span>
                </div>
            )}

            {/* Read-only / Status lock notice */}
            {!isPending && (
                <div className="mb-6 p-4 rounded-2xl bg-indigo-50 border border-indigo-100 text-indigo-900 text-xs font-medium leading-relaxed">
                    <span className="font-black text-indigo-700 uppercase tracking-wide block mb-1">🔒 Trạng thái yêu cầu đã được khóa:</span>
                    Yêu cầu hiện đang ở trạng thái **{getStatusText(currentRequestDetail.status)}** và đang được ban quản lý xử lý.
                    Bạn không thể chỉnh sửa thông tin yêu cầu lúc này.
                </div>
            )}

            <form onSubmit={handleSave} className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                
                {/* Left side: Main Form details */}
                <div className="lg:col-span-8 space-y-6">
                    
                    {/* Basic info section */}
                    <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 space-y-4">
                        <h2 className="text-xs font-black text-on-surface uppercase tracking-wider border-b border-surface-container/80 pb-3">1. Thông tin liên hệ & Loại yêu cầu</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">Số điện thoại liên hệ</label>
                                <input
                                    type="tel"
                                    value={phone}
                                    onChange={(e) => setPhone(e.target.value)}
                                    disabled={!isPending || isSubmitting}
                                    className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all text-on-surface disabled:opacity-75 disabled:cursor-not-allowed"
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">Phân loại yêu cầu</label>
                                <select
                                    value={type}
                                    onChange={(e) => setType(e.target.value as RequestType)}
                                    disabled={!isPending || isSubmitting}
                                    className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all text-on-surface disabled:opacity-75 disabled:cursor-not-allowed cursor-pointer"
                                >
                                    <option value="NORMAL">Tìm hàng thông thường (Normal)</option>
                                    <option value="PREORDER">Đặt trước mô hình hiếm (Pre-order)</option>
                                    <option value="FINDING">Tìm kiếm hàng hiếm giới hạn (Finding)</option>
                                    <option value="CUSTOM">Custom ráp độ / Đắp LED / Sơn phủ (Custom)</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    {/* Items List */}
                    <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 space-y-4">
                        <div className="flex justify-between items-center border-b border-surface-container/80 pb-3">
                            <h2 className="text-xs font-black text-on-surface uppercase tracking-wider">2. Danh sách sản phẩm mong muốn</h2>
                            {isPending && (
                                <button
                                    type="button"
                                    onClick={handleAddItem}
                                    disabled={isSubmitting}
                                    className="flex items-center gap-1.5 px-3.5 py-1.5 bg-primary/10 text-primary hover:bg-primary/20 rounded-full text-xs font-black uppercase tracking-wider transition-colors"
                                >
                                    <Plus className="w-3.5 h-3.5" /> Thêm dòng
                                </button>
                            )}
                        </div>

                        <div className="space-y-3">
                            {items.map((item, index) => (
                                <div key={index} className="p-4 bg-surface-container rounded-2xl flex flex-col md:flex-row gap-4 items-start md:items-center relative">
                                    <div className="flex-1 w-full grid grid-cols-1 md:grid-cols-12 gap-3">
                                        <div className="md:col-span-6">
                                            <label className="block text-[9px] font-black text-outline uppercase tracking-wider mb-1">Tên mô hình / Sản phẩm</label>
                                            <input
                                                type="text"
                                                value={item.name}
                                                onChange={(e) => handleItemChange(index, 'name', e.target.value)}
                                                disabled={!isPending || isSubmitting}
                                                className="w-full bg-surface-container-lowest rounded-xl px-3 py-2 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent transition-all text-on-surface disabled:opacity-75 disabled:cursor-not-allowed"
                                                required
                                            />
                                        </div>
                                        <div className="md:col-span-2">
                                            <label className="block text-[9px] font-black text-outline uppercase tracking-wider mb-1">Số lượng</label>
                                            <input
                                                type="number"
                                                min="1"
                                                value={item.quantity}
                                                onChange={(e) => handleItemChange(index, 'quantity', parseInt(e.target.value) || 1)}
                                                disabled={!isPending || isSubmitting}
                                                className="w-full bg-surface-container-lowest rounded-xl px-3 py-2 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent transition-all text-on-surface disabled:opacity-75 disabled:cursor-not-allowed"
                                                required
                                            />
                                        </div>
                                        <div className="md:col-span-4">
                                            <label className="block text-[9px] font-black text-outline uppercase tracking-wider mb-1">Ghi chú</label>
                                            <input
                                                type="text"
                                                value={item.note}
                                                onChange={(e) => handleItemChange(index, 'note', e.target.value)}
                                                disabled={!isPending || isSubmitting}
                                                className="w-full bg-surface-container-lowest rounded-xl px-3 py-2 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent transition-all text-on-surface disabled:opacity-75 disabled:cursor-not-allowed"
                                            />
                                        </div>
                                    </div>
                                    {isPending && items.length > 1 && (
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveItem(index)}
                                            disabled={isSubmitting}
                                            className="text-outline/60 hover:text-error transition-colors p-2 shrink-0 md:mt-4 self-end md:self-center"
                                            title="Xóa dòng này"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Requirements and images */}
                    <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 space-y-4">
                        <h2 className="text-xs font-black text-on-surface uppercase tracking-wider border-b border-surface-container/80 pb-3">3. Yêu cầu chi tiết & Đính kèm hình ảnh</h2>
                        <div>
                            <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">Yêu cầu chi tiết cho Workshop</label>
                            <textarea
                                value={requirements}
                                onChange={(e) => setRequirements(e.target.value)}
                                disabled={!isPending || isSubmitting}
                                className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all text-on-surface disabled:opacity-75 disabled:cursor-not-allowed min-h-[120px]"
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">Hình ảnh đính kèm minh họa</label>
                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                                {attachments.map((url, idx) => (
                                    <div key={idx} className="relative aspect-square bg-surface-container rounded-xl overflow-hidden border border-outline-variant/20 p-1 group">
                                        <img src={url} alt={`Attachment ${idx}`} className="w-full h-full object-contain rounded-lg" />
                                        {isPending && (
                                            <button
                                                type="button"
                                                onClick={() => handleRemoveAttachment(idx)}
                                                className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-black/60 hover:bg-error text-white flex items-center justify-center transition-colors shadow-md z-10"
                                            >
                                                <Trash2 className="w-3.5 h-3.5" />
                                            </button>
                                        )}
                                    </div>
                                ))}

                                {isPending && (
                                    <label className="aspect-square bg-surface-container border-2 border-dashed border-outline-variant/30 hover:border-primary/50 rounded-xl flex flex-col items-center justify-center cursor-pointer transition-all hover:bg-surface-container-high relative">
                                        <input
                                            type="file"
                                            multiple
                                            accept="image/*"
                                            onChange={handleFileUpload}
                                            disabled={isSubmitting || uploadingFiles}
                                            className="hidden"
                                        />
                                        {uploadingFiles ? (
                                            <>
                                                <Loader2 className="w-6 h-6 text-primary animate-spin mb-1.5" />
                                                <span className="text-[10px] font-bold text-outline">Đang tải...</span>
                                            </>
                                        ) : (
                                            <>
                                                <Upload className="w-6 h-6 text-outline/80 mb-1.5" />
                                                <span className="text-[10px] font-bold text-outline">Chọn tệp ảnh</span>
                                            </>
                                        )}
                                    </label>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right side: Summary quotation details */}
                <div className="lg:col-span-4 space-y-6">
                    <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 sticky top-28 space-y-5">
                        <h2 className="text-xs font-black text-on-surface uppercase tracking-wider border-b border-surface-container/80 pb-3">Phản hồi & Báo giá</h2>
                        
                        <div className="space-y-4 text-xs">
                            <div className="flex justify-between py-2 border-b border-surface-container-low font-bold">
                                <span className="text-outline">Tổng chi phí dự kiến:</span>
                                <span className="text-primary font-black text-base">
                                    {currentRequestDetail.totalAmount > 0 ? formatCurrency(currentRequestDetail.totalAmount) : 'Chờ báo giá'}
                                </span>
                            </div>
                            <div className="flex justify-between py-2 border-b border-surface-container-low font-bold">
                                <span className="text-outline">Cọc tối thiểu (Deposit):</span>
                                <span className="text-on-surface font-black text-sm">
                                    {currentRequestDetail.depositAmount > 0 ? formatCurrency(currentRequestDetail.depositAmount) : 'Chờ tính cọc'}
                                </span>
                            </div>
                            <div className="flex justify-between py-2 border-b border-surface-container-low font-bold">
                                <span className="text-outline">Loại yêu cầu gốc:</span>
                                <span className="text-on-surface font-black uppercase">{getTypeLabel(currentRequestDetail.type)}</span>
                            </div>
                            {currentRequestDetail.nhanhOrderCode && (
                                <div className="flex justify-between py-2 border-b border-surface-container-low font-bold">
                                    <span className="text-outline">Mã Đơn hàng Nhanh:</span>
                                    <span className="text-primary font-black">{currentRequestDetail.nhanhOrderCode}</span>
                                </div>
                            )}
                        </div>

                        {/* Save Action if pending */}
                        {isPending ? (
                            <button
                                type="submit"
                                disabled={isSubmitting || uploadingFiles}
                                className="w-full py-3.5 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-lg hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                            >
                                {isSubmitting ? (
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                ) : (
                                    <Save className="w-4 h-4" />
                                )}
                                <span>Lưu thay đổi</span>
                            </button>
                        ) : (
                            <div className="p-4 bg-surface-container rounded-2xl text-[10px] text-outline font-bold leading-relaxed text-center uppercase tracking-wider">
                                🚫 Đã chốt & Khóa cập nhật
                            </div>
                        )}
                    </div>
                </div>

            </form>
        </main>
    );
}
