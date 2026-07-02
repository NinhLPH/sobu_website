export interface CartItemDto {
    productId: string;
    nhanhProductId?: string;
    name: string;
    price: number;
    imageUrl?: string;
    quantity: number;
}

export interface CartDto {
    items: CartItemDto[];
    updatedAt?: string;
}
