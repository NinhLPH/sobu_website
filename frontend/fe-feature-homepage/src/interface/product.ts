export interface Product {
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
    product: Product;
    quantity: number;
}
