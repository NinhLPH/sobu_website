import apiClient from '../api/api-client';
import {ApiResponseDTO, PageResponse} from '../interface/api-response';
import {
    ProcessRequestDto,
    RequestResponseDto,
    UpdateRequestDto
} from '../interface/customer-request.model';
import {
    AdminOrderQueryParams,
    OrderPaymentResponseDto,
    OrderResponseDto,
    OrderSyncResultDto
} from '../interface/order.model';
import {OrderStatus} from '../enum/union-types';

export const AdminWorkflowService = {
    getAdminRequests: (
        params?: Record<string, unknown>
    ): Promise<ApiResponseDTO<PageResponse<RequestResponseDto>>> => {
        return apiClient.get('/api/admin/requests', {params});
    },

    getAdminRequestDetail: (
        requestId: string | number
    ): Promise<ApiResponseDTO<RequestResponseDto>> => {
        return apiClient.get(`/api/admin/requests/${requestId}`);
    },

    updateAdminRequest: (
        requestId: string | number,
        data: UpdateRequestDto
    ): Promise<ApiResponseDTO<RequestResponseDto>> => {
        return apiClient.put(`/api/admin/requests/${requestId}`, data);
    },

    processRequest: (
        requestId: string | number,
        data: ProcessRequestDto
    ): Promise<ApiResponseDTO<RequestResponseDto>> => {
        return apiClient.post(`/api/admin/requests/${requestId}/process`, data);
    },

    getAdminOrders: (
        params?: AdminOrderQueryParams,
        signal?: AbortSignal
    ): Promise<ApiResponseDTO<PageResponse<OrderResponseDto>>> => {
        return apiClient.get('/api/admin/orders', {params, signal});
    },

    getAdminOrderDetail: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderResponseDto>> => {
        return apiClient.get(`/api/admin/orders/${orderId}`);
    },

    getAdminOrderPayments: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderPaymentResponseDto[]>> => {
        return apiClient.get(`/v1/api/admin/payments/orders/${orderId}`);
    },

    retryOrderSync: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderSyncResultDto>> => {
        return apiClient.post(`/api/admin/orders/${orderId}/sync/retry`);
    },

    updateAdminOrderStatus: (
        orderId: string | number,
        status: OrderStatus
    ): Promise<ApiResponseDTO<OrderResponseDto>> => {
        return apiClient.patch(`/api/admin/orders/${orderId}/status`, {status});
    },

    createPreorderFinalPayment: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderPaymentResponseDto>> => {
        return apiClient.post(`/v1/api/admin/payments/orders/${orderId}/final`);
    },

    confirmMockPayment: (
        paymentCode: string
    ): Promise<ApiResponseDTO<OrderPaymentResponseDto>> => {
        return apiClient.post(
            `/v1/api/admin/payments/${encodeURIComponent(paymentCode)}/mock/confirm`
        );
    },

    exportSpxOrders: (
        data?: { ids?: number[]; status?: string; query?: string }
    ): Promise<Blob> => {
        return apiClient.post('/api/admin/orders/export/spx', data, {
            responseType: 'blob'
        });
    }
};
