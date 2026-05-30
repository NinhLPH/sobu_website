export interface ProductModel {
    id: string;
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
    thumbnailUrls?: string[];
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

export interface ProductImageDTO {
    id?: number;
    url: string;
}

export interface ProductListItemDTO {
    id: number;
    name: string;
    code: string;
    price: number;
    oldPrice?: number;
    avatarImage?: string;
    brandName?: string;
    categoryName?: string;
    stockAvailable?: number;
    status?: string;
}

export interface ProductDetailDTO {
    id: number;
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
    images: ProductImageDTO[];
    updatedAt: string;
}

export const mapListItemToProductModel = (dto: ProductListItemDTO): ProductModel => {
    return {
        id: String(dto.id),
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
        rating: 4.8,
        reviewsCount: 10,
        thumbnailUrls: dto.avatarImage ? [dto.avatarImage] : []
    };
};

export const mapDetailToProductModel = (dto: ProductDetailDTO): ProductModel => {
    return {
        id: String(dto.id),
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
        rating: 4.8,
        reviewsCount: 15,
        thumbnailUrls: dto.images && dto.images.length > 0 
            ? dto.images.map(img => img.url) 
            : [dto.avatarImage || 'https://placehold.co/400x300?text=SOBU']
    };
};