import {useState, useEffect} from 'react';
import {Plus, Edit, Trash2, Search, X} from 'lucide-react';

import {useProductStore} from '../../store/useProductStore';
import {BrandListItemDTO} from '../../interface/brand.model';

export default function AdminBrands() {
    const { brands: dbBrands, fetchBrands } = useProductStore();
    
    const [localBrands, setLocalBrands] = useState<BrandListItemDTO[]>([]);
    const [searchTerm, setSearchTerm] = useState('');

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingBrand, setEditingBrand] = useState<Partial<BrandListItemDTO> | null>(null);

    useEffect(() => {
        fetchBrands();
    }, [fetchBrands]);

    useEffect(() => {
        if (dbBrands && dbBrands.length > 0) {
            setLocalBrands(dbBrands);
        }
    }, [dbBrands]);

    const filteredBrands = localBrands.filter(b => 
        b.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
        b.code.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const handleOpenModal = (brand?: BrandListItemDTO) => {
        if (brand) {
            setEditingBrand({ ...brand });
        } else {
            setEditingBrand({
                id: Math.floor(Math.random() * 10000),
                name: '',
                code: '',
                status: 1
            });
        }
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingBrand(null);
    };

    const handleSave = (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingBrand) return;

        const brandToSave = editingBrand as BrandListItemDTO;

        if (localBrands.some(b => b.id === brandToSave.id)) {
            setLocalBrands(prev => prev.map(b => b.id === brandToSave.id ? brandToSave : b));
        } else {
            setLocalBrands(prev => [...prev, brandToSave]);
        }
        handleCloseModal();
    };

    const handleDelete = (id: number) => {
        if (window.confirm('Bạn có chắc chắn muốn xóa thương hiệu này?')) {
            setLocalBrands(prev => prev.filter(b => b.id !== id));
        }
    };

    return (
        <div className="pt-6 space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Thương hiệu</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:brightness-110">
                    <Plus className="w-5 h-5"/> Thêm thương hiệu
                </button>
            </div>

            <div className="bg-white p-4 rounded-xl border border-outline-variant/30 flex items-center gap-3">
                <Search className="text-outline w-5 h-5"/>
                <input
                    type="text"
                    placeholder="Tìm kiếm thương hiệu..."
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
                            <th className="px-6 py-4">Mã TH</th>
                            <th className="px-6 py-4">Mã Code</th>
                            <th className="px-6 py-4">Tên thương hiệu</th>
                            <th className="px-6 py-4 text-center">Trạng thái</th>
                            <th className="px-6 py-4 text-center">Hành động</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredBrands.map(brand => (
                            <tr key={brand.id}
                                className="border-b border-outline-variant/20 hover:bg-surface-container-lowest/50">
                                <td className="px-6 py-4 font-medium">{brand.id}</td>
                                <td className="px-6 py-4 font-mono text-xs text-outline">{brand.code}</td>
                                <td className="px-6 py-4 font-bold text-on-surface">{brand.name}</td>
                                <td className="px-6 py-4 text-center">
                                    <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                                        brand.status === 1 
                                            ? 'bg-primary/10 text-primary' 
                                            : 'bg-outline-variant/35 text-outline'
                                    }`}>
                                        {brand.status === 1 ? 'Hoạt động' : 'Ngừng hoạt động'}
                                    </span>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center justify-center gap-3">
                                        <button onClick={() => handleOpenModal(brand)}
                                                className="text-secondary hover:text-primary transition-colors">
                                            <Edit className="w-4 h-4"/>
                                        </button>
                                        <button onClick={() => handleDelete(brand.id)}
                                                className="text-error-dim hover:text-error transition-colors">
                                            <Trash2 className="w-4 h-4"/>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {filteredBrands.length === 0 && (
                            <tr>
                                <td colSpan={5} className="px-6 py-8 text-center text-on-surface-variant">
                                    Không tìm thấy thương hiệu nào.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* BrandListItemDTO Modal */}
            {isModalOpen && editingBrand && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
                        <div className="flex justify-between items-center p-6 border-b border-outline-variant/30">
                            <h2 className="text-xl font-bold text-on-surface">
                                {localBrands.some(b => b.id === editingBrand.id) ? 'Cập nhật thương hiệu' : 'Thêm thương hiệu mới'}
                            </h2>
                            <button onClick={handleCloseModal} className="text-outline hover:text-error transition-colors">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <form onSubmit={handleSave} className="p-6 space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Mã thương hiệu</label>
                                <input
                                    type="number"
                                    required
                                    value={editingBrand.id || ''}
                                    onChange={e => setEditingBrand({...editingBrand, id: Number(e.target.value)})}
                                    disabled={localBrands.some(b => b.id === editingBrand.id)}
                                    className="w-full border border-outline-variant rounded p-2 text-sm disabled:bg-surface-variant"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Tên thương hiệu</label>
                                <input
                                    type="text"
                                    required
                                    value={editingBrand.name || ''}
                                    onChange={e => setEditingBrand({...editingBrand, name: e.target.value})}
                                    className="w-full border border-outline-variant rounded p-2 text-sm"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Mã Code (Slug)</label>
                                <input
                                    type="text"
                                    required
                                    value={editingBrand.code || ''}
                                    onChange={e => setEditingBrand({...editingBrand, code: e.target.value})}
                                    placeholder="Ví dụ: HOTWHEELS, TOMICA..."
                                    className="w-full border border-outline-variant rounded p-2 text-sm uppercase"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Trạng thái</label>
                                <select
                                    required
                                    value={editingBrand.status}
                                    onChange={e => setEditingBrand({...editingBrand, status: Number(e.target.value)})}
                                    className="w-full border border-outline-variant rounded p-2 text-sm"
                                >
                                    <option value={1}>Hoạt động</option>
                                    <option value={0}>Ngừng hoạt động</option>
                                </select>
                            </div>

                            <div className="flex justify-end gap-3 pt-4 border-t border-outline-variant/30 mt-6">
                                <button type="button" onClick={handleCloseModal} className="px-4 py-2 font-bold text-on-surface-variant hover:text-on-surface">
                                    Hủy bỏ
                                </button>
                                <button type="submit" className="px-6 py-2 bg-primary text-white font-bold rounded hover:brightness-110">
                                    Lưu thương hiệu
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
