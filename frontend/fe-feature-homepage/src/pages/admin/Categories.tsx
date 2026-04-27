import {useState} from 'react';
import {Plus, Edit, Trash2, ChevronRight, ChevronDown} from 'lucide-react';

import {useAdminStore} from '../../store/useAdminStore';
import {Category} from "../../interface/category";

function CategoryNode({category, level = 0}: { category: Category, level?: number }) {
    const [expanded, setExpanded] = useState(true);
    const {deleteCategory} = useAdminStore();

    const hasChildren = category.children && category.children.length > 0;

    return (
        <div className="w-full">
            <div
                className="flex items-center justify-between p-3 border-b border-outline-variant/20 hover:bg-surface-variant/30 transition-colors"
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

                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity"
                     style={{opacity: 1}}>
                    <button
                        className="p-1.5 text-secondary hover:bg-surface-container rounded transition-colors text-xs font-semibold flex items-center gap-1">
                        <Plus className="w-3 h-3"/> Thêm nhánh con
                    </button>
                    <button className="p-1.5 text-secondary hover:bg-surface-container rounded transition-colors">
                        <Edit className="w-4 h-4"/>
                    </button>
                    <button onClick={() => deleteCategory(category.id)}
                            className="p-1.5 text-error-dim hover:bg-error-container hover:text-error rounded transition-colors">
                        <Trash2 className="w-4 h-4"/>
                    </button>
                </div>
            </div>

            {expanded && hasChildren && (
                <div className="w-full">
                    {category.children!.map(child => (
                        <CategoryNode key={child.id} category={child} level={level + 1}/>
                    ))}
                </div>
            )}
        </div>
    );
}

export default function AdminCategories() {
    const {categories} = useAdminStore();

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-on-surface">Quản lý Danh mục</h1>
                <button
                    className="bg-primary text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:brightness-110">
                    <Plus className="w-5 h-5"/> Thêm danh mục gốc
                </button>
            </div>

            <div className="bg-white rounded-xl border border-outline-variant/30 overflow-hidden shadow-sm">
                {categories.map(category => (
                    <CategoryNode key={category.id} category={category}/>
                ))}
                {categories.length === 0 && (
                    <div className="p-8 text-center text-on-surface-variant">
                        Chưa có danh mục nào.
                    </div>
                )}
            </div>
        </div>
    );
}
