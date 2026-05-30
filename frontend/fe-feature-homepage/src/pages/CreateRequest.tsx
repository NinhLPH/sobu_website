import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ChevronRight, Plus, Trash2, Upload, Loader2, ArrowLeft, CheckCircle } from 'lucide-react';
import { useRequestStore } from '../store/useRequestStore';
import { FileService } from '../service/file.service';
import { RequestType } from '../enum/union-types';

export default function CreateRequest() {
    const navigate = useNavigate();
    const { createRequestAction, isSubmitting, error, clearError } = useRequestStore();

    // Form fields
    const [phone, setPhone] = useState('');
    const [type, setType] = useState<RequestType>('PREORDER');
    const [requirements, setRequirements] = useState('');
    
    // Items list
    const [items, setItems] = useState<{ name: string; quantity: number; note: string }[]>([
        { name: '', quantity: 1, note: '' }
    ]);

    // Attachments
    const [attachments, setAttachments] = useState<string[]>([]);
    const [uploadingFiles, setUploadingFiles] = useState(false);
    const [uploadError, setUploadError] = useState<string | null>(null);
    const [successCreated, setSuccessCreated] = useState(false);

    const handleAddItem = () => {
        setItems(prev => [...prev, { name: '', quantity: 1, note: '' }]);
    };

    const handleRemoveItem = (index: number) => {
        if (items.length === 1) return;
        setItems(prev => prev.filter((_, i) => i !== index));
    };

    const handleItemChange = (index: number, field: string, value: any) => {
        setItems(prev => prev.map((item, i) => {
            if (i === index) {
                return { ...item, [field]: value };
            }
            return item;
        }));
    };

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files || files.length === 0) return;

        setUploadingFiles(true);
        setUploadError(null);
        clearError();

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
            setUploadError('Tải lên tệp tin thất bại! Vui lòng chọn tệp nhỏ hơn hoặc thử lại.');
        } finally {
            setUploadingFiles(false);
        }
    };

    const handleRemoveAttachment = (index: number) => {
        setAttachments(prev => prev.filter((_, i) => i !== index));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        clearError();
        setUploadError(null);

        // Validate
        if (!phone.trim()) {
            setUploadError('Vui lòng nhập số điện thoại liên hệ!');
            return;
        }

        const validItems = items.filter(item => item.name.trim());
        if (validItems.length === 0) {
            setUploadError('Vui lòng thêm ít nhất một món hàng yêu cầu có tên!');
            return;
        }

        const payload = {
            customerPhone: phone,
            type,
            customRequirements: requirements ? { note: requirements } : undefined,
            items: validItems.map(item => ({
                name: item.name,
                quantity: item.quantity,
                note: item.note
            })),
            attachments
        };

        try {
            await createRequestAction(payload);
            setSuccessCreated(true);
        } catch (err) {
            console.error('Create request error:', err);
        }
    };

    if (successCreated) {
        return (
            <main className="max-w-xl mx-auto px-6 pt-32 pb-24 bg-surface text-center">
                <div className="bg-surface-container-lowest rounded-[2rem] p-10 shadow-[0_20px_50px_-15px_rgba(14,48,78,0.06)] border border-surface-container/60 flex flex-col items-center justify-center">
                    <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-6">
                        <CheckCircle className="w-10 h-10 stroke-[2]" />
                    </div>
                    <h1 className="text-2xl font-black text-on-surface mb-3 uppercase tracking-tight">Gửi yêu cầu thành công!</h1>
                    <p className="text-sm text-outline/80 leading-relaxed mb-8 max-w-sm">
                        Yêu cầu của bạn đã được gửi đến ban quản trị SOBU Workshop. Chúng tôi sẽ nhanh chóng xem xét, cập nhật báo giá và liên hệ lại với bạn.
                    </p>
                    <div className="flex gap-4 w-full">
                        <Link 
                            to="/requests"
                            className="flex-1 py-3 bg-primary text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-[1.01] transition-transform"
                        >
                            Xem danh sách
                        </Link>
                        <button 
                            onClick={() => {
                                setSuccessCreated(false);
                                setPhone('');
                                setRequirements('');
                                setItems([{ name: '', quantity: 1, note: '' }]);
                                setAttachments([]);
                            }}
                            className="flex-1 py-3 bg-surface-container text-on-surface border border-outline-variant/30 rounded-2xl text-xs font-black uppercase tracking-widest text-center hover:bg-surface-container-high transition-colors"
                        >
                            Tạo yêu cầu khác
                        </button>
                    </div>
                </div>
            </main>
        );
    }

    return (
        <main className="max-w-3xl mx-auto px-6 pt-32 pb-24 bg-surface">
            {/* Navigation & Back */}
            <nav className="flex items-center gap-2 text-xs font-bold text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-3.5 h-3.5" />
                <Link to="/requests" className="hover:text-primary transition-colors">Yêu cầu</Link>
                <ChevronRight className="w-3.5 h-3.5" />
                <span className="text-primary">Tạo yêu cầu mới</span>
            </nav>

            <div className="flex items-center gap-4 mb-8">
                <Link 
                    to="/requests"
                    className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-on-surface hover:bg-surface-container-high transition-colors"
                    title="Quay lại"
                >
                    <ArrowLeft className="w-5 h-5" />
                </Link>
                <div>
                    <h1 className="text-3xl font-black text-on-surface tracking-tight leading-tight uppercase">
                        Tạo Yêu Cầu Mới
                    </h1>
                    <p className="text-xs text-outline font-bold mt-1">Tìm hàng độc lạ, custom ráp độ, đặt hàng trước (Pre-order)</p>
                </div>
            </div>

            {/* Error notifications */}
            {(error || uploadError) && (
                <div className="mb-6 p-4 rounded-2xl bg-error/10 border border-error/20 text-error text-xs font-bold flex gap-3 items-start">
                    <span className="shrink-0 w-1.5 h-1.5 rounded-full bg-error mt-2" />
                    <span>{uploadError || error}</span>
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
                {/* Section 1: Basic Info */}
                <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 space-y-4">
                    <h2 className="text-sm font-black text-on-surface uppercase tracking-wider border-b border-surface-container/80 pb-3">1. Thông tin liên hệ & Loại yêu cầu</h2>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">
                                Số điện thoại liên hệ
                            </label>
                            <input
                                type="tel"
                                value={phone}
                                onChange={(e) => setPhone(e.target.value)}
                                placeholder="0912345678"
                                disabled={isSubmitting}
                                className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">
                                Phân loại yêu cầu
                            </label>
                            <select
                                value={type}
                                onChange={(e) => setType(e.target.value as RequestType)}
                                disabled={isSubmitting}
                                className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all text-on-surface cursor-pointer"
                            >
                                <option value="PREORDER">Đặt trước mô hình hiếm (Pre-order)</option>
                                <option value="FINDING">Tìm kiếm hàng hiếm giới hạn (Finding)</option>
                                <option value="CUSTOM">Custom ráp độ / Đắp LED / Sơn phủ (Custom)</option>
                            </select>
                        </div>
                    </div>
                </div>

                {/* Section 2: Items manager */}
                <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 space-y-4">
                    <div className="flex justify-between items-center border-b border-surface-container/80 pb-3">
                        <h2 className="text-sm font-black text-on-surface uppercase tracking-wider">2. Danh sách sản phẩm mong muốn</h2>
                        <button
                            type="button"
                            onClick={handleAddItem}
                            disabled={isSubmitting}
                            className="flex items-center gap-1.5 px-3.5 py-1.5 bg-primary/10 text-primary hover:bg-primary/20 rounded-full text-xs font-black uppercase tracking-wider transition-colors"
                        >
                            <Plus className="w-3.5 h-3.5" /> Thêm dòng
                        </button>
                    </div>

                    <div className="space-y-4">
                        {items.map((item, index) => (
                            <div 
                                key={index} 
                                className="p-4 bg-surface-container rounded-2xl flex flex-col md:flex-row gap-4 items-start md:items-center relative"
                            >
                                <div className="flex-1 w-full grid grid-cols-1 md:grid-cols-12 gap-3">
                                    <div className="md:col-span-6">
                                        <label className="block text-[10px] font-bold text-outline uppercase tracking-wider mb-1">Tên mô hình / Sản phẩm</label>
                                        <input
                                            type="text"
                                            value={item.name}
                                            onChange={(e) => handleItemChange(index, 'name', e.target.value)}
                                            placeholder="Ví dụ: Gundam PG Unleashed RX-78-2..."
                                            disabled={isSubmitting}
                                            className="w-full bg-surface-container-lowest rounded-xl px-3 py-2 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent transition-all placeholder:text-outline/40 text-on-surface"
                                            required
                                        />
                                    </div>
                                    <div className="md:col-span-2">
                                        <label className="block text-[10px] font-bold text-outline uppercase tracking-wider mb-1">Số lượng</label>
                                        <input
                                            type="number"
                                            min="1"
                                            value={item.quantity}
                                            onChange={(e) => handleItemChange(index, 'quantity', parseInt(e.target.value) || 1)}
                                            disabled={isSubmitting}
                                            className="w-full bg-surface-container-lowest rounded-xl px-3 py-2 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent transition-all text-on-surface"
                                            required
                                        />
                                    </div>
                                    <div className="md:col-span-4">
                                        <label className="block text-[10px] font-bold text-outline uppercase tracking-wider mb-1">Ghi chú riêng</label>
                                        <input
                                            type="text"
                                            value={item.note}
                                            onChange={(e) => handleItemChange(index, 'note', e.target.value)}
                                            placeholder="Yêu cầu riêng/Tỷ lệ..."
                                            disabled={isSubmitting}
                                            className="w-full bg-surface-container-lowest rounded-xl px-3 py-2 text-xs font-semibold focus:ring-1 focus:ring-primary outline-none border border-transparent transition-all placeholder:text-outline/40 text-on-surface"
                                        />
                                    </div>
                                </div>

                                {items.length > 1 && (
                                    <button
                                        type="button"
                                        onClick={() => handleRemoveItem(index)}
                                        disabled={isSubmitting}
                                        className="text-outline/60 hover:text-error transition-colors p-2 shrink-0 md:mt-4 self-end md:self-center"
                                        title="Xóa sản phẩm này"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>

                {/* Section 3: Custom Requirements & Attachments */}
                <div className="bg-surface-container-lowest rounded-[2rem] p-6 shadow-sm border border-surface-container/60 space-y-4">
                    <h2 className="text-sm font-black text-on-surface uppercase tracking-wider border-b border-surface-container/80 pb-3">3. Mô tả chi tiết & Đính kèm hình ảnh</h2>
                    
                    <div>
                        <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">
                            Yêu cầu chi tiết / Ghi chú cho Workshop
                        </label>
                        <textarea
                            value={requirements}
                            onChange={(e) => setRequirements(e.target.value)}
                            placeholder="Mô tả kỹ thêm về màu sơn tĩnh điện mong muốn, cách đi bóng LED, hoặc nguồn gốc mô hình bạn cần tìm..."
                            disabled={isSubmitting}
                            className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface min-h-[100px]"
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">
                            Hình ảnh đính kèm minh họa
                        </label>
                        
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
                            {attachments.map((url, idx) => (
                                <div key={idx} className="relative aspect-square bg-surface-container rounded-xl overflow-hidden group border border-outline-variant/20 p-1">
                                    <img src={url} alt={`Upload ${idx}`} className="w-full h-full object-contain rounded-lg" />
                                    <button
                                        type="button"
                                        onClick={() => handleRemoveAttachment(idx)}
                                        className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-black/60 hover:bg-error text-white flex items-center justify-center transition-colors shadow-md z-10"
                                        title="Xóa ảnh"
                                    >
                                        <Trash2 className="w-3.5 h-3.5" />
                                    </button>
                                </div>
                            ))}

                            {/* Upload Button */}
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
                                        <span className="text-[10px] font-bold text-outline">Chọn tệp hình ảnh</span>
                                    </>
                                )}
                            </label>
                        </div>
                    </div>
                </div>

                {/* Submit button */}
                <button
                    type="submit"
                    disabled={isSubmitting || uploadingFiles}
                    className="w-full py-4 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-lg shadow-primary/10 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                    {isSubmitting ? (
                        <>
                            <Loader2 className="w-4 h-4 animate-spin" />
                            <span>Đang xử lý...</span>
                        </>
                    ) : (
                        <span>Gửi Yêu Cầu Cho SOBU Workshop</span>
                    )}
                </button>
            </form>
        </main>
    );
}
