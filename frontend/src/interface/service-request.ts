export interface ServiceRequest {
    id: string;
    type: 'PRE_ORDER' | 'CUSTOM';
    customerName: string;
    customerPhone: string;
    customerEmail: string;
    productName?: string;
    description: string;
    budget?: number;
    status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'COMPLETED';
    adminNotes?: string;
    createdAt: string;
}