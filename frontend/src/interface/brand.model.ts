export interface BrandListItemDTO {
    id: number;
    parentId?: number;
    code: string;
    name: string;
    slug?: string;
    logoUrl?: string | null;
    logoAlt?: string | null;
    description?: string | null;
    status: number;
}

export interface BrandDetailDTO extends BrandListItemDTO {
    externalId?: number | null;
    seo?: import('./seo.model').SeoMetadataDTO | null;
}
