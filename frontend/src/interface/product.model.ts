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
    manualTag?: ProductTagModel;
    rating?: number;
    reviewsCount?: number;
    thumbnailUrls?: string[] | undefined;
}

export interface ProductTagModel {
    label: string;
    backgroundColor: string;
    textColor: string;
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
    code?: string | null;
    price?: number | null;
    oldPrice?: number | null;
    salePrice?: number | null;
    badgeId?: number | null;
    badgeName?: string | null;
    badgeColor?: string | null;
    badgeTextColor?: string | null;
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
    oldPrice?: number | null;
    salePrice?: number | null;
    saleValidFrom?: string | null;
    saleValidThrough?: string | null;
    badgeId?: number | null;
    badgeName?: string | null;
    badgeColor?: string | null;
    badgeTextColor?: string | null;
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

const toManualTag = (dto: {
    badgeName?: string | null;
    badgeColor?: string | null;
    badgeTextColor?: string | null;
}): ProductTagModel | undefined => {
    const label = dto.badgeName?.trim();
    if (!label || label.toUpperCase() === 'SALE') return undefined;
    return {
        label,
        backgroundColor: dto.badgeColor || '#00618e',
        textColor: dto.badgeTextColor || '#ffffff'
    };
};

export const isSaleProduct = (product: Pick<ProductModel, 'price' | 'originalPrice'>): boolean =>
    product.price >= 0
    && product.originalPrice != null
    && product.originalPrice > product.price;

export const getDiscountPercent = (product: Pick<ProductModel, 'price' | 'originalPrice'>): number => {
    if (!isSaleProduct(product) || !product.originalPrice) return 0;
    return Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100);
};

export const mapListItemToProductModel = (dto: ProductListItemDTO): ProductModel => {
    const price = dto.price ?? 0;
    const originalPrice = dto.oldPrice != null && dto.oldPrice > price && price >= 0
        ? dto.oldPrice
        : undefined;
    const manualTag = toManualTag(dto);
    return {
        id: String(dto.id),
        externalId: dto.externalId === undefined ? undefined : String(dto.externalId),
        nhanhProductId: String(dto.nhanhProductId ?? dto.externalId ?? dto.id),
        name: dto.name,
        price,
        originalPrice,
        category: dto.categoryName || '',
        brand: dto.brandName || '',
        imageUrl: dto.avatarImage || 'https://placehold.co/400x300?text=SOBU',
        description: '',
        stock: dto.stockAvailable || 0,
        isNew: manualTag?.label.toUpperCase() === 'NEW',
        isHot: manualTag?.label.toUpperCase() === 'HOT',
        manualTag,
        rating: dto.averageRating ?? 0,
        reviewsCount: dto.reviewsCount ?? 0,
        thumbnailUrls: dto.avatarImage ? [dto.avatarImage] : []
    };
};

export const mapDetailToProductModel = (dto: ProductDetailDTO): ProductModel => {
    const originalPrice = dto.oldPrice != null && dto.oldPrice > dto.price && dto.price >= 0
        ? dto.oldPrice
        : undefined;
    const manualTag = toManualTag(dto);
    return {
        id: String(dto.id),
        externalId: dto.externalId === undefined ? undefined : String(dto.externalId),
        nhanhProductId: String(dto.nhanhProductId ?? dto.externalId ?? dto.id),
        name: dto.name,
        price: dto.price,
        originalPrice,
        category: dto.categoryName || '',
        brand: dto.brandName || '',
        imageUrl: dto.avatarImage || 'https://placehold.co/400x300?text=SOBU',
        description: dto.description || dto.content || '',
        stock: dto.stockAvailable || dto.stockRemain || 0,
        isNew: manualTag?.label.toUpperCase() === 'NEW',
        isHot: manualTag?.label.toUpperCase() === 'HOT',
        manualTag,
        rating: dto.averageRating ?? 0,
        reviewsCount: dto.reviewsCount ?? 0,
        thumbnailUrls: dto.images && dto.images.length > 0 
            ? dto.images
            : ['https://placehold.co/400x300?text=SOBU']
    };
};
