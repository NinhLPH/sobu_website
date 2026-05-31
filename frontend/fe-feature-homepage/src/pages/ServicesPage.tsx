import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Paintbrush, PackageSearch, RefreshCcw} from 'lucide-react';
import {useAuthStore} from '../store/useAuthStore';

export default function ServicesPage() {
    const [activeTab, setActiveTab] = useState<'custom' | 'preorder' | 'tradein'>('custom');
    const navigate = useNavigate();
    const {isAuthenticated} = useAuthStore();

    const handleServiceSubmit = (type: 'CUSTOM' | 'PREORDER' | 'FINDING') => {
        if (!isAuthenticated) {
            navigate('/login');
        } else {
            navigate(`/requests/new?type=${type}`);
        }
    };

    return (
        <main className="pt-32 pb-24 bg-surface px-6 min-h-screen">
            <div className="max-w-4xl mx-auto">
                <div className="text-center mb-12">
                    <h1 className="text-5xl font-black uppercase tracking-tighter text-on-surface mb-4">Dịch vụ đặc
                        thù</h1>
                    <p className="text-lg font-medium text-on-surface-variant">Trải nghiệm dịch vụ chuyên nghiệp nhất
                        dành cho Collector.</p>
                </div>

                {/* TABS */}
                <div className="flex bg-surface-container-low rounded-full p-2 mb-12 shadow-inner">
                    <button onClick={() => setActiveTab('custom')}
                            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-full font-bold text-sm transition-all ${activeTab === 'custom' ? 'bg-white shadow-md text-primary' : 'text-on-surface-variant hover:text-on-surface'}`}>
                        <Paintbrush className="w-4 h-4"/> Custom Mô hình
                    </button>
                    <button onClick={() => setActiveTab('preorder')}
                            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-full font-bold text-sm transition-all ${activeTab === 'preorder' ? 'bg-white shadow-md text-primary' : 'text-on-surface-variant hover:text-on-surface'}`}>
                        <PackageSearch className="w-4 h-4"/> Pre-Order Hàng Hot
                    </button>
                    <button onClick={() => setActiveTab('tradein')}
                            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-full font-bold text-sm transition-all ${activeTab === 'tradein' ? 'bg-white shadow-md text-primary' : 'text-on-surface-variant hover:text-on-surface'}`}>
                        <RefreshCcw className="w-4 h-4"/> Thu Mua & Trade-in
                    </button>
                </div>

                {/* TAB CONTENT */}
                <div
                    className="bg-surface-container-lowest p-8 md:p-12 rounded-[2rem] shadow-[0_20px_40px_-15px_rgba(14,48,78,0.05)]">

                    {activeTab === 'custom' && (
                        <div className="space-y-6">
                            <h2 className="text-2xl font-black uppercase text-on-surface border-b border-outline-variant/20 pb-4">Yêu
                                cầu Custom / Độ Led</h2>
                            <p className="text-on-surface-variant font-medium text-sm mb-6">Điền thông tin mô hình bạn
                                muốn custom. Đội ngũ Sobu sẽ liên hệ báo giá và tiến độ trong 24h.</p>
                            <input type="text" placeholder="Tên mô hình / Hãng sản xuất"
                                   className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-medium placeholder:text-outline"/>
                            <textarea placeholder="Mô tả chi tiết yêu cầu (Màu sơn, loại LED, base...)"
                                      className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-medium placeholder:text-outline min-h-[150px]"/>
                            <div
                                className="bg-surface-container-low border-2 border-dashed border-outline-variant/50 rounded-xl p-8 text-center text-outline font-bold cursor-pointer hover:bg-surface-container transition-colors">
                                + Tải lên hình ảnh tham khảo (Tùy chọn)
                            </div>
                            <button
                                onClick={() => handleServiceSubmit('CUSTOM')}
                                className="w-full py-4 bg-gradient-to-r from-primary to-primary-container text-white rounded-full font-black uppercase tracking-widest hover:shadow-lg transition-all mt-4">Gửi
                                Yêu Cầu
                            </button>
                        </div>
                    )}

                    {activeTab === 'preorder' && (
                        <div className="space-y-6">
                            <h2 className="text-2xl font-black uppercase text-on-surface border-b border-outline-variant/20 pb-4">Pre-Order
                                Sản Phẩm</h2>
                            <p className="text-on-surface-variant font-medium text-sm mb-6">Không tìm thấy hàng trên
                                web? Yêu cầu chúng tôi tìm và đặt trước cho bạn. Cọc linh hoạt từ 30% - 100%.</p>
                            <input type="text" placeholder="Tên sản phẩm / Mã sản phẩm / Link tham khảo"
                                   className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-medium placeholder:text-outline"/>
                            <select
                                className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-bold text-on-surface">
                                <option>Hình thức đặt cọc: 30% (Thanh toán phần còn lại khi nhận)</option>
                                <option>Hình thức đặt cọc: 100% (Ưu tiên xử lý & Miễn phí ship)</option>
                            </select>
                            <textarea placeholder="Ghi chú thêm (Phiên bản, màu sắc...)"
                                      className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-medium placeholder:text-outline min-h-[100px]"/>
                            <button
                                onClick={() => handleServiceSubmit('PREORDER')}
                                className="w-full py-4 bg-gradient-to-r from-primary to-primary-container text-white rounded-full font-black uppercase tracking-widest hover:shadow-lg transition-all mt-4">Tạo
                                đơn Pre-order
                            </button>
                        </div>
                    )}

                    {activeTab === 'tradein' && (
                        <div className="space-y-6">
                            <h2 className="text-2xl font-black uppercase text-on-surface border-b border-outline-variant/20 pb-4">Thu
                                Mua Mô Hình</h2>
                            <p className="text-on-surface-variant font-medium text-sm mb-6">Bạn muốn Pass lại mô hình?
                                Điền form thẩm định, Sobu sẽ báo giá Deal tốt nhất cho bạn.</p>
                            <div className="grid grid-cols-2 gap-4">
                                <input type="text" placeholder="Tên mô hình"
                                       className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-medium placeholder:text-outline"/>
                                <select
                                    className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-bold text-on-surface">
                                    <option value="">Tình trạng</option>
                                    <option>Brand New (Nguyên seal)</option>
                                    <option>Like New (Mở hộp trưng bày)</option>
                                    <option>No Box / Lỗi nhẹ</option>
                                </select>
                            </div>
                            <input type="number" placeholder="Mức giá mong muốn (VNĐ)"
                                   className="w-full bg-surface-container-low px-6 py-4 rounded-xl outline-none focus:ring-2 focus:ring-primary/40 font-medium placeholder:text-outline"/>
                            <div
                                className="bg-surface-container-low border-2 border-dashed border-outline-variant/50 rounded-xl p-8 text-center text-outline font-bold cursor-pointer hover:bg-surface-container transition-colors">
                                + Tải lên hình ảnh thực tế (Bắt buộc)
                            </div>
                            <button
                                onClick={() => handleServiceSubmit('FINDING')}
                                className="w-full py-4 bg-gradient-to-r from-primary to-primary-container text-white rounded-full font-black uppercase tracking-widest hover:shadow-lg transition-all mt-4">Gửi
                                Yêu Cầu Thẩm Định
                            </button>
                        </div>
                    )}

                </div>
            </div>
        </main>
    );
}