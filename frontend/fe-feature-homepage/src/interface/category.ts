export interface Category {
    id: string;
    name: string;
    parentId?: string | null;
    children?: Category[];
}

export interface CategoryNodeProps {
    category: Category;
    level?: number;
    onEdit: (category: Category) => void;
    onAddChild: (parentId: string) => void;
}