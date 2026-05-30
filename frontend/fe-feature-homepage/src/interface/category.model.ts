export interface CategoryModel {
    id: string;
    name: string;
    parentId?: string | null;
    children?: CategoryModel[];
}

export interface CategoryNodeProps {
    category: CategoryModel;
    level?: number;
    onEdit: (category: CategoryModel) => void;
    onAddChild: (parentId: string) => void;
}

export interface CategoryListItemDTO {
    id: number;
    parentId?: number;
    code: string;
    name: string;
    order?: number;
    image?: string;
    status: number;
    children?: CategoryListItemDTO[];
}