import {useState, useEffect, useMemo} from 'react';
import {Plus, Edit, Trash2, Search, X} from 'lucide-react';

import {ProductModel, mapListItemToProductModel} from "../../interface/product.model";
import {CategoryModel, mapCategoryDtoToModel} from "../../interface/category.model";
import {useProductStore} from '../../store/useProductStore';
import {formatCurrency} from "../../utils/format";
import SearchSuggestInput, {SearchSuggestion} from '../../components/common/SearchSuggestInput';

export default function AdminProducts() {
    const { products: dbProducts, categories: dbCategories, fetchProducts, fetchCategories } = useProductStore();
    
    const [localProducts, setLocalProducts] = useState<ProductModel[]>([]);
    const [localCategories, setLocalCategories] = useState<CategoryModel[]>([]);
    const [searchTerm, setSearchTerm] = useState('');

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingProduct, setEditingProduct] = useState<Partial<ProductModel> | null>(null);

    useEffect(() => {
        fetchProducts();
        fetchCategories();
    }, [fetchProducts, fetchCategories]);

    useEffect(() => {
        if (dbProducts && dbProducts.length > 0) {
            setLocalProducts(dbProducts.map(mapListItemToProductModel));
        }
    }, [dbProducts]);

    useEffect(() => {
        if (dbCategories && dbCategories.length > 0) {
            setLocalCategories(dbCategories.map(mapCategoryDtoToModel));
        }
    }, [dbCategories]);

    // Flatten categories for dropdown
    const flatCategories: { id: string; name: string }[] = [];
    const flatten = (cats: CategoryModel[], prefix = '') => {
        cats.forEach(c => {
            flatCategories.push({ id: c.id, name: `${prefix}${c.name}` });
            if (c.children) flatten(c.children, `${prefix}${c.name} > `);
        });
    };
    flatten(localCategories);

    const filteredProducts = localProducts.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase()));

    const searchSuggestions = useMemo<SearchSuggestion[]>(() => localProducts.map((product) => ({
        id: product.id,
        label: product.name,
        description: [product.id, product.category, product.brand].filter(Boolean).join(' • '),
        searchValue: product.name,
    })), [localProducts]);

    const handleOpenModal = (product?: ProductModel) => {
        if (product) {
            setEditingProduct({ ...product });
        } else {
            setEditingProduct({
                id: `PROD-${Math.floor(Math.random() * 10000)}`,
                name: '',
                price: 0,
                categoryId: flatCategories[0]?.id || '',
                category: flatCategories[0]?.name || '',
                brand: '',
                imageUrl: '',
                description: '',
                stock: 0,
            });
        }
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingProduct(null);
    };

    const handleSave = (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingProduct) return;

        // Find category name based on categoryId to keep them in sync
        const cat = flatCategories.find(c => c.id === editingProduct.categoryId);
        const productToSave = { ...editingProduct, category: cat ? cat.name : editingProduct.category } as ProductModel;

        if (localProducts.some(p => p.id === productToSave.id)) {
            setLocalProducts(prev => prev.map(p => p.id === productToSave.id ? productToSave : p));
        } else {
            setLocalProducts(prev => [...prev, productToSave]);
        }
        handleCloseModal();
    };

    const handleDelete = (id: string) => {
        if (window.confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')) {
            setLocalProducts(prev => prev.filter(p => p.id !== id));
        }
    };


    return (
        <div className="pt-6 space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Sản phẩm</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:brightness-110">
                    <Plus className="w-5 h-5"/> Thêm sản phẩm
                </button>
            </div>

            <div className="relative flex items-center gap-3 rounded-xl border border-outline-variant/30 bg-white p-4">
                <Search className="text-outline w-5 h-5"/>
                <SearchSuggestInput
                    placeholder="Tìm kiếm sản phẩm..."
                    className="bg-transparent border-none outline-none w-full text-sm"
                    value={searchTerm}
                    onChange={setSearchTerm}
                    onSubmit={setSearchTerm}
                    suggestions={searchSuggestions}
                    ariaLabel="Tìm kiếm sản phẩm quản trị"
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
                                        <button onClick={() => handleOpenModal(product)}
                                                className="text-secondary hover:text-primary transition-colors">
                                            <Edit className="w-4 h-4"/>
                                        </button>
                                        <button onClick={() => handleDelete(product.id)}
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
            {/* ProductModel Modal */}
            {isModalOpen && editingProduct && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                        <div className="flex justify-between items-center p-6 border-b border-outline-variant/30 sticky top-0 bg-white">
                            <h2 className="text-xl font-bold text-on-surface">
                                {localProducts.some(p => p.id === editingProduct.id) ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới'}
                            </h2>
                            <button onClick={handleCloseModal} className="text-outline hover:text-error transition-colors">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <form onSubmit={handleSave} className="p-6 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Mã sản phẩm</label>
                                    <input
                                        type="text"
                                        required
                                        value={editingProduct.id || ''}
                                        onChange={e => setEditingProduct({...editingProduct, id: e.target.value})}
                                        disabled={localProducts.some(p => p.id === editingProduct.id)}
                                        className="w-full border border-outline-variant rounded p-2 text-sm disabled:bg-surface-variant"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Tên sản phẩm</label>
                                    <input
                                        type="text"
                                        required
                                        value={editingProduct.name || ''}
                                        onChange={e => setEditingProduct({...editingProduct, name: e.target.value})}
                                        className="w-full border border-outline-variant rounded p-2 text-sm"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Danh mục</label>
                                    <select
                                        required
                                        value={editingProduct.categoryId || ''}
                                        onChange={e => setEditingProduct({...editingProduct, categoryId: e.target.value})}
                                        className="w-full border border-outline-variant rounded p-2 text-sm"
                                    >
                                        <option value="" disabled>Chọn danh mục</option>
                                        {flatCategories.map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Thương hiệu</label>
                                    <input
                                        type="text"
                                        required
                                        value={editingProduct.brand || ''}
                                        onChange={e => setEditingProduct({...editingProduct, brand: e.target.value})}
                                        className="w-full border border-outline-variant rounded p-2 text-sm"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Giá bán (đ)</label>
                                    <input
                                        type="number"
                                        required min={0}
                                        value={editingProduct.price || ''}
                                        onChange={e => setEditingProduct({...editingProduct, price: Number(e.target.value)})}
                                        className="w-full border border-outline-variant rounded p-2 text-sm"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Giá gốc (đ)</label>
                                    <input
                                        type="number"
                                        min={0}
                                        value={editingProduct.originalPrice || ''}
                                        onChange={e => setEditingProduct({...editingProduct, originalPrice: Number(e.target.value)})}
                                        className="w-full border border-outline-variant rounded p-2 text-sm"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1">Tồn kho</label>
                                    <input
                                        type="number"
                                        required min={0}
                                        value={editingProduct.stock || ''}
                                        onChange={e => setEditingProduct({...editingProduct, stock: Number(e.target.value)})}
                                        className="w-full border border-outline-variant rounded p-2 text-sm"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">URL Hình ảnh</label>
                                <input
                                    type="url"
                                    required
                                    value={editingProduct.imageUrl || ''}
                                    onChange={e => setEditingProduct({...editingProduct, imageUrl: e.target.value})}
                                    className="w-full border border-outline-variant rounded p-2 text-sm"
                                />
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Mô tả</label>
                                <textarea
                                    required
                                    value={editingProduct.description || ''}
                                    onChange={e => setEditingProduct({...editingProduct, description: e.target.value})}
                                    className="w-full border border-outline-variant rounded p-2 text-sm min-h-[100px]"
                                />
                            </div>

                            <div className="flex justify-end gap-3 pt-4 border-t border-outline-variant/30">
                                <button type="button" onClick={handleCloseModal} className="px-4 py-2 font-bold text-on-surface-variant hover:text-on-surface">
                                    Huỷ bỏ
                                </button>
                                <button type="submit" className="px-6 py-2 bg-primary text-white font-bold rounded hover:brightness-110">
                                    Lưu sản phẩm
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
