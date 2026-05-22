export interface OrderItem {
    productId: string;
    productName: string;
    quantity: number;
    price: number;
}

export interface Order {
    id: string;
    customerName: string;
    customerPhone: string;
    customerEmail: string;
    shippingAddress: string;
    items: OrderItem[];
    subtotal: number;
    tax: number;
    shippingFee: number;
    total: number;
    status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
    createdAt: string;
}