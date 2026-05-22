import {useState} from 'react';
import {Search, CheckCircle, XCircle} from 'lucide-react';

import {useAdminStore} from '../../store/useAdminStore';
import {formatCurrency} from "../../util/format";
import {ServiceRequest} from "../../interface/service-request";

export default function AdminRequests() {
    const {requests, updateRequest} = useAdminStore();
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedReq, setSelectedReq] = useState<ServiceRequest | null>(null);

    const [editNotes, setEditNotes] = useState('');
    const [editBudget, setEditBudget] = useState('');

    const filteredRequests = requests.filter(r =>
        r.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.customerName.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'PENDING':
                return 'bg-amber-100 text-amber-800 border-amber-200';
            case 'ACCEPTED':
                return 'bg-blue-100 text-blue-800 border-blue-200';
            case 'COMPLETED':
                return 'bg-green-100 text-green-800 border-green-200';
            case 'REJECTED':
                return 'bg-red-100 text-red-800 border-red-200';
            default:
                return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    };

    const handleSelect = (req: ServiceRequest) => {
        setSelectedReq(req);
        setEditNotes(req.adminNotes || '');
        setEditBudget(req.budget ? req.budget.toString() : '');
    };

    const handleSave = () => {
        if (!selectedReq) return;
        updateRequest(selectedReq.id, {
            adminNotes: editNotes,
            budget: editBudget ? parseInt(editBudget) : undefined,
        });
        setSelectedReq({
            ...selectedReq,
            adminNotes: editNotes,
            budget: editBudget ? parseInt(editBudget) : undefined,
        });
    };

    const handleStatusChange = (status: ServiceRequest['status']) => {
        if (!selectedReq) return;
        updateRequest(selectedReq.id, {status});
        setSelectedReq({...selectedReq, status});
    };

    return (
        <div className="pt-6 space-y-6 flex flex-col md:flex-row gap-6">

            {/* Left side: List */}
            <div className="flex-1 space-y-4">
                <div className="flex justify-between items-center">
                    <h1 className="text-2xl font-bold text-on-surface">Yêu cầu Dịch vụ</h1>
                </div>

                <div
                    className="bg-white p-4 rounded-xl border border-outline-variant/30 flex items-center gap-3 shadow-sm">
                    <Search className="text-outline w-5 h-5"/>
                    <input
                        type="text"
                        placeholder="Tìm mã hoặc tên khách..."
                        className="bg-transparent border-none outline-none w-full text-sm"
                        value={searchTerm}
                        onChange={e => setSearchTerm(e.target.value)}
                    />
                </div>

                <div className="space-y-3">
                    {filteredRequests.map(req => (
                        <div
                            key={req.id}
                            onClick={() => handleSelect(req)}
                            className={`bg-white p-4 rounded-xl border cursor-pointer transition-colors ${
                                selectedReq?.id === req.id ? 'border-primary shadow-md' : 'border-outline-variant/30 hover:border-primary/50'
                            }`}
                        >
                            <div className="flex justify-between items-start mb-2">
                                <div>
                  <span
                      className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded-sm ${req.type === 'PRE_ORDER' ? 'bg-purple-100 text-purple-700' : 'bg-orange-100 text-orange-700'}`}>
                    {req.type === 'PRE_ORDER' ? 'Pre-Order' : 'Custom Model'}
                  </span>
                                    <h3 className="font-bold text-on-surface mt-1">{req.customerName}</h3>
                                </div>
                                <span
                                    className={`text-[10px] font-bold uppercase px-2 py-1 rounded-full border ${getStatusColor(req.status)}`}>
                  {req.status}
                </span>
                            </div>
                            <p className="text-sm text-on-surface-variant line-clamp-1">{req.description}</p>
                            <div className="text-xs text-outline mt-2 flex justify-between">
                                <span>Mã: {req.id}</span>
                                <span>{new Date(req.createdAt).toLocaleDateString('vi-VN')}</span>
                            </div>
                        </div>
                    ))}
                    {filteredRequests.length === 0 && (
                        <div
                            className="p-8 text-center text-on-surface-variant bg-white rounded-xl border border-outline-variant/30">
                            Không có yêu cầu nào.
                        </div>
                    )}
                </div>
            </div>

            {/* Right side: Detail Form */}
            {selectedReq && (
                <div className="w-full md:w-96 flex-shrink-0">
                    <div className="bg-white p-6 rounded-xl border border-outline-variant/30 shadow-sm sticky top-32">
                        <h2 className="text-lg font-bold text-on-surface border-b border-outline-variant/30 pb-4 mb-4">
                            Chi tiết Yêu cầu
                        </h2>

                        <div className="space-y-4 text-sm">
                            <div>
                                <p className="text-xs text-outline font-bold uppercase mb-1">Khách hàng</p>
                                <p className="font-medium text-on-surface">{selectedReq.customerName}</p>
                                <p className="text-on-surface-variant">{selectedReq.customerPhone} &bull; {selectedReq.customerEmail}</p>
                            </div>

                            {selectedReq.type === 'PRE_ORDER' && selectedReq.productName && (
                                <div>
                                    <p className="text-xs text-outline font-bold uppercase mb-1">Sản phẩm yêu cầu</p>
                                    <p className="font-bold text-primary">{selectedReq.productName}</p>
                                </div>
                            )}

                            <div>
                                <p className="text-xs text-outline font-bold uppercase mb-1">Nội dung / Mô tả</p>
                                <p className="p-3 bg-surface-container-low rounded-lg text-on-surface">
                                    {selectedReq.description}
                                </p>
                            </div>

                            <div className="border-t border-outline-variant/30 pt-4 mt-4 space-y-4">
                                <h3 className="font-bold text-on-surface">Nội bộ Admin (Khách không thấy)</h3>

                                {selectedReq.type === 'CUSTOM' && (
                                    <div>
                                        <label className="text-xs text-outline font-bold uppercase mb-1 block">Chi phí
                                            dự kiến (VND)</label>
                                        <input
                                            type="number"
                                            className="w-full border border-outline-variant rounded bg-surface p-2 text-sm focus:ring-1 focus:ring-primary outline-none"
                                            value={editBudget}
                                            onChange={(e) => setEditBudget(e.target.value)}
                                            placeholder="VD: 3000000"
                                        />
                                    </div>
                                )}

                                <div>
                                    <label className="text-xs text-outline font-bold uppercase mb-1 block">Ghi chú xử
                                        lý</label>
                                    <textarea
                                        className="w-full border border-outline-variant rounded bg-surface p-2 text-sm min-h-[80px] focus:ring-1 focus:ring-primary outline-none"
                                        value={editNotes}
                                        onChange={(e) => setEditNotes(e.target.value)}
                                        placeholder="Ghi chú thêm trong quá trình làm việc với khách..."
                                    />
                                </div>

                                <button
                                    onClick={handleSave}
                                    className="w-full bg-surface-variant text-on-surface font-bold py-2 rounded border border-outline-variant hover:bg-surface transition-colors"
                                >
                                    Lưu thay đổi nội dung
                                </button>
                            </div>

                            <div className="border-t border-outline-variant/30 pt-4 mt-4">
                                <p className="text-xs text-outline font-bold uppercase mb-3 text-center">Phê duyệt yêu
                                    cầu</p>
                                <div className="flex gap-2">
                                    <button
                                        onClick={() => handleStatusChange('ACCEPTED')}
                                        className="flex-1 flex items-center justify-center gap-2 bg-green-100 text-green-700 hover:bg-green-200 py-2 rounded font-bold transition-colors"
                                    >
                                        <CheckCircle className="w-4 h-4"/> Tiếp nhận
                                    </button>
                                    <button
                                        onClick={() => handleStatusChange('REJECTED')}
                                        className="flex-1 flex items-center justify-center gap-2 bg-red-100 text-red-700 hover:bg-red-200 py-2 rounded font-bold transition-colors"
                                    >
                                        <XCircle className="w-4 h-4"/> Từ chối
                                    </button>
                                </div>
                                {selectedReq.status === 'ACCEPTED' && (
                                    <button
                                        onClick={() => handleStatusChange('COMPLETED')}
                                        className="w-full mt-2 bg-blue-600 text-white hover:bg-blue-700 py-2 rounded font-bold transition-colors shadow-sm"
                                    >
                                        Đánh dấu Hoàn thành
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
