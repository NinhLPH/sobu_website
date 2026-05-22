import {useState} from 'react';
import {Plus, Edit, Trash2, ChevronRight, ChevronDown, X} from 'lucide-react';

import {useAdminStore} from '../../store/useAdminStore';
import {Category, CategoryNodeProps} from "../../interface/category";

function CategoryNode({category, level = 0, onEdit, onAddChild}: { category: Category, level?: number, onEdit: (category?: Category, parentId?: string) => void, onAddChild: (parentId: string) => void }) {
    const [expanded, setExpanded] = useState(true);
    const {deleteCategory} = useAdminStore();

    const hasChildren = category.children && category.children.length > 0;

    const handleDelete = () => {
        if (window.confirm('Bạn có chắc chắn muốn xóa danh mục này?\n' +
            'Các danh mục con cũng sẽ bị xóa và sản phẩm có thể bị ảnh hưởng.')) {
            deleteCategory(category.id);
        }
    }

    return (
        <div className="w-full">
            <div
                className="flex items-center justify-between p-3 border-b border-outline-variant/20 hover:bg-surface-variant/30 transition-colors group"
                style={{paddingLeft: `${level * 2 + 1}rem`}}
            >
                <div className="flex items-center gap-2">
                    {hasChildren ? (
                        <button onClick={() => setExpanded(!expanded)}
                                className="p-1 hover:bg-surface-container rounded">
                            {expanded ? <ChevronDown className="w-4 h-4 text-outline"/> :
                                <ChevronRight className="w-4 h-4 text-outline"/>}
                        </button>
                    ) : (
                        <span className="w-6"/> // spacer
                    )}
                    <span className="font-bold text-on-surface">{category.name}</span>
                    <span
                        className="text-xs text-outline px-2 py-0.5 bg-surface-container rounded-md">{category.id}</span>
                </div>

                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button onClick={() => onAddChild(category.id)}
                            className="p-1.5 text-secondary hover:bg-surface-container rounded transition-colors text-xs font-semibold flex items-center gap-1">
                        <Plus className="w-3 h-3"/> Thêm nhánh con
                    </button>
                    <button onClick={() => onEdit(category)}
                            className="p-1.5 text-secondary hover:bg-surface-container rounded transition-colors">
                        <Edit className="w-4 h-4"/>
                    </button>
                    <button onClick={handleDelete}
                            className="p-1.5 text-error-dim hover:bg-error-container hover:text-error rounded transition-colors">
                        <Trash2 className="w-4 h-4"/>
                    </button>
                </div>
            </div>

            {expanded && hasChildren && (
                <div className="w-full">
                    {category.children!.map(child => (
                        <CategoryNode
                            key={child.id}
                            category={child}
                            level={level + 1}
                            onEdit={onEdit}
                            onAddChild={onAddChild}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

export default function AdminCategories() {
    const {categories, addCategory, updateCategory} = useAdminStore();

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCategory, setEditingCategory] = useState<Partial<Category> | null>(null);
    const [isEditing, setIsEditing] = useState(false); // Thêm state này để xác định đúng chế độ

    // Flatten for parent dropdown
    const flatCategories: { id: string; name: string }[] = [];
    const flatten = (cats: Category[], prefix = '') => {
        cats.forEach(c => {
            flatCategories.push({id: c.id, name: `${prefix}${c.name}`});
            if (c.children) flatten(c.children, `${prefix}${c.name} > `);
        });
    };
    flatten(categories);

    const handleOpenModal = (category?: Category, parentId?: string) => {
        if (category) {
            // Cập nhật: Lược bỏ children ra khỏi payload để tránh ghi đè mất danh mục con khi save
            const { children, ...rest } = category;
            setIsEditing(true);
            setEditingCategory(rest);
        } else {
            // Tạo mới
            setIsEditing(false);
            setEditingCategory({
                id: `CAT_${Math.floor(Math.random() * 10000)}`,
                name: '',
                parentId: parentId || null
            });
        }
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingCategory(null);
        setIsEditing(false);
    };

    const handleSave = (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingCategory) return;

        if (isEditing) {
            // Chỉ truyền lên những field cần cập nhật (ví dụ: name)
            updateCategory(editingCategory.id!, { name: editingCategory.name });
        } else {
            // Validate nếu user tự nhập ID bị trùng
            if (flatCategories.some(c => c.id === editingCategory.id)) {
                alert('Mã danh mục đã tồn tại, vui lòng chọn mã khác!');
                return;
            }
            addCategory(editingCategory as Category);
        }
        handleCloseModal();
    };

    return (
        <div className="pt-6 space-y-6">
            {/* Các phần Title và CategoryNode giữ nguyên */}
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Danh mục</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:brightness-110"
                >
                    <Plus className="w-5 h-5"/> Thêm danh mục gốc
                </button>
            </div>

            <div className="bg-white rounded-xl border border-outline-variant/30 overflow-hidden shadow-sm">
                {categories.map(category => (
                    <CategoryNode
                        key={category.id}
                        category={category}
                        onEdit={handleOpenModal}
                        onAddChild={(parentId) => handleOpenModal(undefined, parentId)}
                    />
                ))}
                {categories.length === 0 && (
                    <div className="p-8 text-center text-on-surface-variant">
                        Chưa có danh mục nào.
                    </div>
                )}
            </div>

            {/* Category Modal */}
            {isModalOpen && editingCategory && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
                        <div className="flex justify-between items-center p-6 border-b border-outline-variant/30">
                            <h2 className="text-xl font-bold text-on-surface">
                                {/* Sử dụng isEditing để render Title chính xác */}
                                {isEditing ? 'Cập nhật Danh mục' : 'Thêm Danh mục mới'}
                            </h2>
                            <button onClick={handleCloseModal}
                                    className="text-outline hover:text-error transition-colors">
                                <X className="w-6 h-6"/>
                            </button>
                        </div>

                        <form onSubmit={handleSave} className="p-6 space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Mã danh mục</label>
                                <input
                                    type="text"
                                    required
                                    value={editingCategory.id || ''}
                                    onChange={e => setEditingCategory({...editingCategory, id: e.target.value})}
                                    disabled={isEditing} // Sử dụng isEditing
                                    className="w-full border border-outline-variant rounded p-2 text-sm disabled:bg-surface-variant"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Tên danh mục</label>
                                <input
                                    type="text"
                                    required
                                    value={editingCategory.name || ''}
                                    onChange={e => setEditingCategory({...editingCategory, name: e.target.value})}
                                    className="w-full border border-outline-variant rounded p-2 text-sm"
                                />
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1">Danh mục cha</label>
                                <select
                                    value={editingCategory.parentId || ''}
                                    onChange={e => setEditingCategory({
                                        ...editingCategory,
                                        parentId: e.target.value || null
                                    })}
                                    className="w-full border border-outline-variant rounded p-2 text-sm disabled:bg-surface-variant"
                                    disabled={isEditing} // Sử dụng isEditing
                                >
                                    <option value="">-- Là danh mục gốc --</option>
                                    {flatCategories
                                        .filter(c => c.id !== editingCategory.id)
                                        .map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                </select>
                                <p className="text-[10px] text-outline mt-1">*Chỉ định khi thêm mới. Không thể di chuyển danh mục sau khi đã tạo dữ liệu gốc.</p>
                            </div>

                            <div className="flex justify-end gap-3 pt-4 border-t border-outline-variant/30 mt-6">
                                <button type="button" onClick={handleCloseModal}
                                        className="px-4 py-2 font-bold text-on-surface-variant hover:text-on-surface">
                                    Hủy bỏ
                                </button>
                                <button type="submit"
                                        className="px-6 py-2 bg-primary text-white font-bold rounded hover:brightness-110">
                                    Lưu danh mục
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
