import {OrderStatus} from '../enum/union-types';

export interface AdminOrderStatusTransition {
    status: OrderStatus;
    label: string;
}

const NEXT_FULFILMENT_STATUS: Partial<Record<OrderStatus, AdminOrderStatusTransition>> = {
    NEW: {status: 'PROCESSING', label: 'Đang xử lý'},
    PROCESSING: {status: 'SHIPPED', label: 'Đang giao'},
    SHIPPED: {status: 'DELIVERED', label: 'Đã giao'}
};

export const getNextAdminOrderStatus = (status?: OrderStatus): AdminOrderStatusTransition | null =>
    status ? NEXT_FULFILMENT_STATUS[status] ?? null : null;
