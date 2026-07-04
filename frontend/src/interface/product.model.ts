export interface ProductModel {
    id: string;
    externalId?: string;
    nhanhProductId?: string;
    name: string;
    price: number;
    originalPrice?: number;
    category?: string;
    categoryId?: string;
    scale?: string;
    brand: string;
    imageUrl: string;
    description: string;
    stock: number;
    isNew?: boolean;
    isHot?: boolean;
    rating?: number;
    reviewsCount?: number;
    thumbnailUrls?: string[] | undefined;
}

export interface CartItem {
    product: ProductModel;
    quantity: number;
}

export interface ProductAttributeDTO {
    id?: number;
    name: string;
    value: string;
}

export interface ProductUnitDTO {
    id?: number;
    name: string;
    price: number;
    wholesalePrice?: number;
    quantity: number;
}

export interface ProductListItemDTO {
    id: number;
    externalId?: string | number;
    nhanhProductId?: string | number;
    name: string;
    code: string;
    price: number;
    oldPrice?: number;
    avatarImage?: string;
    brandName?: string;
    categoryName?: string;
    stockAvailable?: number;
    averageRating?: number;
    reviewsCount?: number;
    status?: string;
}

export interface ProductDetailDTO {
    id: number;
    externalId?: string | number;
    nhanhProductId?: string | number;
    name: string;
    code: string;
    description: string;
    content: string;
    price: number;
    oldPrice: number;
    avatarImage: string;
    brandName: string;
    categoryName: string;
    stockAvailable: number;
    stockRemain: number;
    units: ProductUnitDTO[];
    attributes: ProductAttributeDTO[];
    images: string[];
    averageRating?: number;
    reviewsCount?: number;
    updatedAt: string;
}

export const mapListItemToProductModel = (dto: ProductListItemDTO): ProductModel => {
    return {
        id: String(dto.id),
        externalId: dto.externalId === undefined ? undefined : String(dto.externalId),
        nhanhProductId: String(dto.nhanhProductId ?? dto.externalId ?? dto.id),
        name: dto.name,
        price: dto.price,
        originalPrice: dto.oldPrice,
        category: dto.categoryName || '',
        brand: dto.brandName || '',
        imageUrl: dto.avatarImage || 'https://placehold.co/400x300?text=SOBU',
        description: '',
        stock: dto.stockAvailable || 0,
        isNew: dto.status === 'NEW',
        isHot: dto.status === 'HOT',
        rating: dto.averageRating ?? 0,
        reviewsCount: dto.reviewsCount ?? 0,
        thumbnailUrls: dto.avatarImage ? [dto.avatarImage] : []
    };
};

export const mapDetailToProductModel = (dto: ProductDetailDTO): ProductModel => {
    return {
        id: String(dto.id),
        externalId: dto.externalId === undefined ? undefined : String(dto.externalId),
        nhanhProductId: String(dto.nhanhProductId ?? dto.externalId ?? dto.id),
        name: dto.name,
        price: dto.price,
        originalPrice: dto.oldPrice,
        category: dto.categoryName || '',
        brand: dto.brandName || '',
        imageUrl: dto.avatarImage || 'https://placehold.co/400x300?text=SOBU',
        description: dto.description || dto.content || '',
        stock: dto.stockAvailable || dto.stockRemain || 0,
        isNew: false,
        isHot: false,
        rating: dto.averageRating ?? 0,
        reviewsCount: dto.reviewsCount ?? 0,
        thumbnailUrls: dto.images && dto.images.length > 0 
            ? dto.images
            : ['https://placehold.co/400x300?text=SOBU']
    };
};
