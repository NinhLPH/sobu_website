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
    slug?: string;
    order?: number;
    image?: string;
    imageAlt?: string;
    status: number;
    children?: CategoryListItemDTO[];
}

export interface CategoryDetailDTO extends Omit<CategoryListItemDTO, 'children'> {
    introContent?: string | null;
    footerContent?: string | null;
    content?: string | null;
    seo?: import('./seo.model').SeoMetadataDTO | null;
}

export const mapCategoryDtoToModel = (dto: CategoryListItemDTO): CategoryModel => {
    return {
        id: String(dto.id),
        name: dto.name,
        parentId: dto.parentId ? String(dto.parentId) : null,
        children: dto.children ? dto.children.map(mapCategoryDtoToModel) : undefined
    };
};
