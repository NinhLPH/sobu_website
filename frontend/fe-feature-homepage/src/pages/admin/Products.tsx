import {useState} from 'react';
import {Plus, Edit, Trash2, Search} from 'lucide-react';

import {useAdminStore} from '../../store/useAdminStore';
import {formatCurrency} from "../../util/format";

export default function AdminProducts() {
    const {products, deleteProduct} = useAdminStore();
    const [searchTerm, setSearchTerm] = useState('');

    const filteredProducts = products.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase()));

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Sản phẩm</h1>
                <button
                    className="bg-primary text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:brightness-110">
                    <Plus className="w-5 h-5"/> Thêm sản phẩm
                </button>
            </div>

            <div className="bg-white p-4 rounded-xl border border-outline-variant/30 flex items-center gap-3">
                <Search className="text-outline w-5 h-5"/>
                <input
                    type="text"
                    placeholder="Tìm kiếm sản phẩm..."
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
                            <th className="px-6 py-4">Mã SP</th>
                            <th className="px-6 py-4">Hình ảnh</th>
                            <th className="px-6 py-4">Tên sản phẩm</th>
                            <th className="px-6 py-4">Danh mục</th>
                            <th className="px-6 py-4 text-right">Giá bán</th>
                            <th className="px-6 py-4 text-right">Tồn kho</th>
                            <th className="px-6 py-4 text-center">Hành động</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredProducts.map(product => (
                            <tr key={product.id}
                                className="border-b border-outline-variant/20 hover:bg-surface-container-lowest/50">
                                <td className="px-6 py-4 font-medium">{product.id}</td>
                                <td className="px-6 py-4">
                                    <img src={product.imageUrl} alt={product.name}
                                         className="w-12 h-12 rounded object-contain bg-surface-container"/>
                                </td>
                                <td className="px-6 py-4 font-bold text-on-surface">{product.name}</td>
                                <td className="px-6 py-4 text-outline">{product.category}</td>
                                <td className="px-6 py-4 text-right text-primary font-bold">{formatCurrency(product.price)}</td>
                                <td className="px-6 py-4 text-right">{product.stock}</td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center justify-center gap-3">
                                        <button className="text-secondary hover:text-primary transition-colors">
                                            <Edit className="w-4 h-4"/>
                                        </button>
                                        <button onClick={() => deleteProduct(product.id)}
                                                className="text-error-dim hover:text-error transition-colors">
                                            <Trash2 className="w-4 h-4"/>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {filteredProducts.length === 0 && (
                            <tr>
                                <td colSpan={7} className="px-6 py-8 text-center text-on-surface-variant">
                                    Không tìm thấy sản phẩm nào.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
