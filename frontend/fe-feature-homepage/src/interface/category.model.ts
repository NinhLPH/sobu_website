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

export const mapCategoryDtoToModel = (dto: CategoryListItemDTO): CategoryModel => {
    return {
        id: String(dto.id),
        name: dto.name,
        parentId: dto.parentId ? String(dto.parentId) : null,
        children: dto.children ? dto.children.map(mapCategoryDtoToModel) : undefined
    };
};