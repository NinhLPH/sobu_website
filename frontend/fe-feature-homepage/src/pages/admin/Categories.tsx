import {useState, useEffect, useMemo} from 'react';
import {Plus, Edit, Trash2, ChevronRight, ChevronDown, X} from 'lucide-react';

import {useProductStore} from '../../store/useProductStore';
import {CategoryModel, mapCategoryDtoToModel} from "../../interface/category.model";

const collectCategoryTreeIds = (categories: CategoryModel[], rootId: string) => {
    const ids = new Set<string>([rootId]);
    const pending = [rootId];

    while (pending.length > 0) {
        const parentId = pending.shift()!;
        for (const category of categories) {
            if (category.parentId === parentId && !ids.has(category.id)) {
                ids.add(category.id);
                pending.push(category.id);
            }
        }
    }

    return ids;
};

function CategoryNode({category, level = 0, onEdit, onAddChild, onDelete}: { category: CategoryModel, level?: number, onEdit: (category?: CategoryModel, parentId?: string) => void, onAddChild: (parentId: string) => void, onDelete: (id: string) => void }) {
    const [expanded, setExpanded] = useState(true);

    const hasChildren = category.children && category.children.length > 0;

    const handleDelete = () => {
        if (window.confirm('Bạn có chắc chắn muốn xóa danh mục này?\n' +
            'Các danh mục con cũng sẽ bị xóa và sản phẩm có thể bị ảnh hưởng.')) {
            onDelete(category.id);
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
                                className="p-1 hover:bg-surface-container rounded cursor-pointer">
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
                            className="p-1.5 text-secondary hover:bg-surface-container rounded transition-colors text-xs font-semibold flex items-center gap-1 cursor-pointer">
                        <Plus className="w-3 h-3"/> Thêm nhánh con
                    </button>
                    <button onClick={() => onEdit(category)}
                            className="p-1.5 text-secondary hover:bg-surface-container rounded transition-colors cursor-pointer">
                        <Edit className="w-4 h-4"/>
                    </button>
                    <button onClick={handleDelete}
                            className="p-1.5 text-error-dim hover:bg-error-container hover:text-error rounded transition-colors cursor-pointer">
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
                            onDelete={onDelete}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

export default function AdminCategories() {
    const { categories: dbCategories, fetchCategories } = useProductStore();

    const [localCategories, setLocalCategories] = useState<CategoryModel[]>([]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCategory, setEditingCategory] = useState<Partial<CategoryModel> | null>(null);
    const [isEditing, setIsEditing] = useState(false);

    useEffect(() => {
        fetchCategories();
    }, [fetchCategories]);

    useEffect(() => {
        if (dbCategories && dbCategories.length > 0) {
            const flatList: CategoryModel[] = [];

            const extractToFlat = (cats: any[]) => {
                cats.forEach(cat => {
                    const model = mapCategoryDtoToModel(cat);
                    const { children, ...flatModel } = model;
                    flatList.push(flatModel);

                    if (cat.children && cat.children.length > 0) {
                        extractToFlat(cat.children);
                    }
                });
            };

            extractToFlat(dbCategories);
            setLocalCategories(flatList);
        }
    }, [dbCategories]);

    const hierarchicalCategories = useMemo(() => {
        const map = new Map<string, CategoryModel>();
        const roots: CategoryModel[] = [];

        localCategories.forEach(cat => {
            map.set(cat.id, { ...cat, children: [] });
        });

        localCategories.forEach(cat => {
            if (cat.parentId && map.has(cat.parentId)) {
                map.get(cat.parentId)!.children!.push(map.get(cat.id)!);
            } else {
                roots.push(map.get(cat.id)!);
            }
        });

        return roots;
    }, [localCategories]);
   const flatCategoriesForDropdown = useMemo(() => {
        const getFullName = (cat: CategoryModel): string => {
            if (!cat.parentId) return cat.name;
            const parent = localCategories.find(c => c.id === cat.parentId);
            return parent ? `${getFullName(parent)} > ${cat.name}` : cat.name;
        };

        return localCategories.map(cat => ({
            id: cat.id,
            name: getFullName(cat)
        })).sort((a, b) => a.name.localeCompare(b.name));
    }, [localCategories]);
    const getInvalidParentIds = () => {
        if (!editingCategory?.id) return [];
        return Array.from(collectCategoryTreeIds(localCategories, editingCategory.id));
    };

    const handleOpenModal = (category?: CategoryModel, parentId?: string) => {
        if (category) {
            const { children, ...rest } = category;
            setIsEditing(true);
            setEditingCategory(rest);
        } else {
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
            setLocalCategories(prev => prev.map(c =>
                c.id === editingCategory.id ? { ...c, name: editingCategory.name! } : c
            ));
        } else {
            if (localCategories.some(c => c.id === editingCategory.id)) {
                alert('Mã danh mục đã tồn tại, vui lòng chọn mã khác!');
                return;
            }
            const newCat = editingCategory as CategoryModel;
            setLocalCategories(prev => [...prev, newCat]);
        }
        handleCloseModal();
    };

    const handleDeleteCategory = (id: string) => {
        const idsToDelete = collectCategoryTreeIds(localCategories, id);

         setLocalCategories(prev => prev.filter(c => !idsToDelete.has(c.id)));
    };

    const invalidParentIds = getInvalidParentIds();

    return (
        <div className="pt-6 space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Danh mục</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:brightness-110 cursor-pointer"
                >
                    <Plus className="w-5 h-5"/> Thêm danh mục gốc
                </button>
            </div>

            <div className="bg-white rounded-xl border border-outline-variant/30 overflow-hidden shadow-sm">
                {/* Dùng hierarchicalCategories để render dạng cây */}
                {hierarchicalCategories.map(category => (
                    <CategoryNode
                        key={category.id}
                        category={category}
                        onEdit={handleOpenModal}
                        onAddChild={(parentId) => handleOpenModal(undefined, parentId)}
                        onDelete={handleDeleteCategory}
                    />
                ))}
                {hierarchicalCategories.length === 0 && (
                    <div className="p-8 text-center text-on-surface-variant">
                        Chưa có danh mục nào.
                    </div>
                )}
            </div>

            {/* Modal */}
            {isModalOpen && editingCategory && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
                        <div className="flex justify-between items-center p-6 border-b border-outline-variant/30">
                            <h2 className="text-xl font-bold text-on-surface">
                                {isEditing ? 'Cập nhật Danh mục' : 'Thêm Danh mục mới'}
                            </h2>
                            <button onClick={handleCloseModal}
                                    className="text-outline hover:text-error transition-colors cursor-pointer">
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
                                    disabled={isEditing}
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
                                    className="w-full border border-outline-variant rounded p-2 text-sm disabled:bg-surface-variant cursor-pointer"
                                    disabled={isEditing}
                                >
                                    <option value="">-- Là danh mục gốc --</option>
                                    {flatCategoriesForDropdown
                                        .filter(c => !invalidParentIds.includes(c.id)) // Ẩn các danh mục con để tránh bị loop logic
                                        .map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                </select>
                                <p className="text-[10px] text-outline mt-1">*Chỉ định khi thêm mới. Không thể di chuyển danh mục sau khi đã tạo dữ liệu gốc.</p>
                            </div>

                            <div className="flex justify-end gap-3 pt-4 border-t border-outline-variant/30 mt-6">
                                <button type="button" onClick={handleCloseModal}
                                        className="px-4 py-2 font-bold text-on-surface-variant hover:text-on-surface cursor-pointer">
                                    Hủy bỏ
                                </button>
                                <button type="submit"
                                        className="px-6 py-2 bg-primary text-white font-bold rounded hover:brightness-110 cursor-pointer">
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
