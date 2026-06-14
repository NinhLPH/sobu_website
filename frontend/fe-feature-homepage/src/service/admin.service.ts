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
        params?: AdminOrderQueryParams
    ): Promise<ApiResponseDTO<PageResponse<OrderResponseDto>>> => {
        return apiClient.get('/api/admin/orders', {params});
    },

    getAdminOrderDetail: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderResponseDto>> => {
        return apiClient.get(`/api/admin/orders/${orderId}`);
    },

    retryOrderSync: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderSyncResultDto>> => {
        return apiClient.post(`/api/admin/orders/${orderId}/sync/retry`);
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
    }
};
