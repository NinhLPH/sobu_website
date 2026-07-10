import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Plus, Trash2, Loader2, ArrowLeft, CheckCircle, MessageCircle, ExternalLink, Sparkles } from 'lucide-react';
import ImageUploader from '../components/common/ImageUploader';
import { useRequestStore } from '../store/useRequestStore';
import { ToastService } from '../service/toast.service';
import { RequestWorkflowType } from '../interface/customer-request.model';
import { useProductStore } from '../store/useProductStore';
import CatalogProductCombobox, {
    CatalogProductSelection
} from '../components/common/CatalogProductCombobox';
import { useAuthStore } from '../store/useAuthStore';
import {usePublicUiStore} from '../store/usePublicUiStore';
import {parseJsonConfig} from '../utils/website-config';

interface RequestFormItem {
    nhanhProductId?: string;
    name: string;
    price?: number;
    quantity: number;
    note: string;
}

const emptyItem = (): RequestFormItem => ({
    name: '',
    quantity: 1,
    note: ''
});

type SocialLinks = Record<string, string>;

export default function CreateRequest() {
    const { createRequestAction, isSubmitting, error, clearError } = useRequestStore();
    const userPhone = useAuthStore((state) => state.user?.phone?.trim() || '');
    const configMap = usePublicUiStore((state) => state.configMap);
    const {
        allProducts,
        fetchAllProducts,
        isAllProductsLoading
    } = useProductStore();
    const socialLinks = parseJsonConfig<SocialLinks>(configMap, 'social_links', {});
    const facebookConsultationUrl = socialLinks.facebook?.trim() || '';

    // Form fields
    const [phone, setPhone] = useState(userPhone);
    const [type, setType] = useState<RequestWorkflowType>('PREORDER');
    const [requirements, setRequirements] = useState('');
    
    // Items list
    const [items, setItems] = useState<RequestFormItem[]>([emptyItem()]);

    // Attachments
    const [uploadedImages, setUploadedImages] = useState<string[]>([]);
    const [uploadingFiles, setUploadingFiles] = useState(false);
    const [uploadError, setUploadError] = useState<string | null>(null);
    const [successCreated, setSuccessCreated] = useState(false);
    const isCustomRequest = type === 'CUSTOM';

    useEffect(() => {
        void fetchAllProducts();
    }, [fetchAllProducts]);

    useEffect(() => {
        if (userPhone) {
            setPhone(currentPhone => currentPhone.trim() ? currentPhone : userPhone);
        }
    }, [userPhone]);

    const handleAddItem = () => {
        setItems(prev => [...prev, emptyItem()]);
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

    const handleProductSelection = (
        index: number,
        selection: CatalogProductSelection
    ) => {
        setItems(prev => prev.map((item, itemIndex) => (
            itemIndex === index
                ? {
                    ...item,
                    ...selection
                }
                : item
        )));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        clearError();
        setUploadError(null);

        if (isCustomRequest) {
            return;
        }

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
        if (type === 'FINDING' && !requirements.trim()) {
            setUploadError('Vui lòng nhập yêu cầu chi tiết cho loại yêu cầu đã chọn.');
            return;
        }

        const payload = {
            customerPhone: phone,
            type,
            customRequirements: requirements.trim() || undefined,
            items: validItems.map(item => ({
                nhanhProductId: item.nhanhProductId,
                name: item.name.trim(),
                price: item.price,
                quantity: item.quantity,
                note: item.note.trim() || undefined
            })),
            uploadedImageUrls: uploadedImages
        };

        try {
            await createRequestAction(payload);
            setSuccessCreated(true);
            ToastService.success('Gửi yêu cầu thành công.');
        } catch (err) {
            // The store exposes the backend error below and through a toast.
        }
    };

    React.useEffect(() => {
        if (error) {
            ToastService.error(error);
        }
    }, [error]);

    if (successCreated) {
        return (
            <main className="w-full min-w-0 bg-surface px-4 pb-24 pt-28 text-center sm:px-6 sm:pt-32">
                <div className="max-w-xl mx-auto bg-surface-container-lowest rounded-[2rem] p-10 shadow-[0_20px_50px_-15px_rgba(14,48,78,0.06)] border border-surface-container/60 flex flex-col items-center justify-center">
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
                                setPhone(userPhone);
                                setRequirements('');
                                setItems([emptyItem()]);
                                setUploadedImages([]);
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
        <main className="w-full min-w-0 bg-surface px-4 pb-24 pt-28 sm:px-6 sm:pt-32">
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
                    <h2 className="text-sm font-black text-on-surface uppercase tracking-wider border-b border-surface-container/80 pb-3">
                        {isCustomRequest ? '1. Loại yêu cầu' : '1. Thông tin liên hệ & Loại yêu cầu'}
                    </h2>
                    
                    <div className={`grid grid-cols-1 gap-4 ${isCustomRequest ? 'md:grid-cols-1' : 'md:grid-cols-2'}`}>
                        {!isCustomRequest && (
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
                        )}

                        <div>
                            <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">
                                Phân loại yêu cầu
                            </label>
                            <select
                                value={type}
                                onChange={(e) => {
                                    const nextType = e.target.value as RequestWorkflowType;
                                    setType(nextType);
                                    setItems([emptyItem()]);
                                    setUploadError(null);
                                }}
                                disabled={isSubmitting}
                                className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all text-on-surface cursor-pointer"
                            >
                                <option value="PREORDER">Đặt trước mô hình</option>
                                <option value="FINDING">Tìm kiếm hàng hiếm</option>
                                <option value="CUSTOM">Custom ráp độ / Đắp LED / Sơn phủ</option>
                            </select>
                        </div>
                    </div>
                </div>

                {isCustomRequest ? (
                    <section className="relative overflow-hidden rounded-[2rem] border border-[#1877F2]/20 bg-surface-container-lowest p-6 shadow-[0_24px_70px_-30px_rgba(24,119,242,0.45)] sm:p-8">
                        <div className="absolute -right-16 -top-20 h-56 w-56 rounded-full bg-[#1877F2]/10 blur-3xl motion-reduce:hidden" />
                        <div className="absolute -bottom-24 left-8 h-48 w-48 rounded-full bg-primary/10 blur-3xl motion-reduce:hidden" />

                        <div className="relative grid gap-8 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
                            <div className="space-y-5">
                                <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/10 px-4 py-2 text-[11px] font-black uppercase tracking-widest text-primary">
                                    <Sparkles className="h-4 w-4" />
                                    Custom
                                </div>

                                <div>
                                    <h2 className="text-2xl font-black leading-tight tracking-tight text-on-surface sm:text-3xl">
                                        Trao đổi trực tiếp với SOBU qua Facebook
                                    </h2>
                                    <p className="mt-2 max-w-2xl text-sm font-medium leading-7 text-on-surface-variant">
                                        Đội ngũ SOBU sẽ cùng bạn làm rõ ý tưởng, phạm vi công việc và báo giá trước khi bắt đầu xử lý dịch vụ.
                                    </p>
                                </div>

                                <div className="grid gap-3 text-sm font-bold text-on-surface sm:grid-cols-3">
                                    {['Tư vấn concept', 'Gửi ảnh tham khảo', 'Báo giá chi tiết'].map((item) => (
                                        <div key={item} className="flex items-center gap-2 rounded-2xl bg-surface-container px-4 py-3">
                                            <CheckCircle className="h-4 w-4 shrink-0 text-primary" />
                                            <span>{item}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="relative flex flex-col items-stretch gap-3 rounded-3xl border border-surface-container-high bg-surface-container-low p-4 sm:min-w-[280px]">
                                {facebookConsultationUrl ? (
                                    <a
                                        href={facebookConsultationUrl}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="group inline-flex cursor-pointer items-center justify-center gap-2 rounded-2xl bg-[#1877F2] px-6 py-4 text-sm font-black uppercase tracking-widest text-white shadow-[0_18px_36px_-14px_rgba(24,119,242,0.85)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#166FE5] hover:shadow-[0_24px_42px_-14px_rgba(24,119,242,0.95)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#1877F2]/40 focus-visible:ring-offset-2 focus-visible:ring-offset-surface-container-low active:translate-y-0 motion-reduce:transition-colors motion-reduce:hover:translate-y-0"
                                    >
                                        <MessageCircle className="h-5 w-5 transition-transform duration-300 group-hover:scale-110 motion-reduce:transition-none motion-reduce:group-hover:scale-100" />
                                        Nhắn tin qua Facebook
                                        <ExternalLink className="h-4 w-4 opacity-80" />
                                    </a>
                                ) : (
                                    <span
                                        aria-disabled="true"
                                        className="inline-flex items-center justify-center gap-2 rounded-2xl bg-surface-container-high px-6 py-4 text-sm font-black uppercase tracking-widest text-outline shadow-none"
                                    >
                                        <MessageCircle className="h-5 w-5" />
                                        Facebook chưa được cấu hình
                                    </span>
                                )}
                            </div>
                        </div>
                    </section>
                ) : (
                    <>
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
                                        <CatalogProductCombobox
                                            products={allProducts}
                                            value={item}
                                            onChange={(selection) => handleProductSelection(index, selection)}
                                            disabled={isSubmitting}
                                            isLoading={isAllProductsLoading}
                                            ariaLabel={`Sản phẩm yêu cầu ${index + 1}`}
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
                            aria-label="Yêu cầu chi tiết"
                            placeholder="Mô tả kỹ thêm về màu sơn tĩnh điện mong muốn, cách đi bóng LED, hoặc nguồn gốc mô hình bạn cần tìm..."
                            disabled={isSubmitting}
                            className="w-full bg-surface-container rounded-2xl px-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface min-h-[100px]"
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-2 pl-1">
                            Hình ảnh đính kèm minh họa
                        </label>
                        
                        <ImageUploader
                            uploadedUrls={uploadedImages}
                            onChange={setUploadedImages}
                            disabled={isSubmitting}
                            subDirectory="requests"
                            onUploadingChange={setUploadingFiles}
                        />
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
                    </>
                )}
            </form>
        </main>
    );
}
