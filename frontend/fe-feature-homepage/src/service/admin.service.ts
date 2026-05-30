import apiClient from "../api/api-client";
import {PageResponse} from "../interface/api-response";
import {ProcessRequestDto, RequestResponseDto, UpdateRequestDto} from "../interface/customer-request.model";
import {OrderResponseDto, OrderSyncResultDto} from "../interface/order.model";


export const AdminWorkflowService = {
    // --- ADMIN REQUESTS ---
    getRequests: (params?: any): Promise<PageResponse<RequestResponseDto>> => {
        return apiClient.get('/api/admin/requests', { params });
    },

    getRequestDetail: (requestId: string | number): Promise<RequestResponseDto> => {
        return apiClient.get(`/api/admin/requests/${requestId}`);
    },

    updateRequest: (requestId: string | number, data: UpdateRequestDto): Promise<RequestResponseDto> => {
        return apiClient.put(`/api/admin/requests/${requestId}`, data);
    },

    processRequest: (requestId: string | number, data: ProcessRequestDto): Promise<RequestResponseDto> => {
        return apiClient.post(`/api/admin/requests/${requestId}/process`, data);
    },

    // --- ADMIN ORDERS ---
    getOrders: (params?: any): Promise<PageResponse<OrderResponseDto>> => {
        return apiClient.get('/api/admin/orders', { params });
    },

    getOrderDetail: (orderId: string | number): Promise<OrderResponseDto> => {
        return apiClient.get(`/api/admin/orders/${orderId}`);
    },

    retryOrderSyncNhanh: (orderId: string | number): Promise<OrderSyncResultDto> => {
        return apiClient.post(`/api/admin/orders/${orderId}/sync/retry`);
    },
};