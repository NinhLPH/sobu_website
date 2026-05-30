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