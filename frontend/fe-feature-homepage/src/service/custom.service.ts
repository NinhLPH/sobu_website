import apiClient from "../api/api-client";
import {PageResponse} from "../interface/api-response";
import {CreateRequestDto, RequestResponseDto, UpdateRequestDto} from "../interface/customer-request.model";
import {OrderResponseDto} from "../interface/order.model";
import { createIdempotencyKey } from "../utils/idempotency";


export const CustomerService = {
    // --- REQUESTS (Yêu cầu tìm hàng, custom, pre-order) ---
    getMyRequests: (params?: any): Promise<PageResponse<RequestResponseDto>> => {
        return apiClient.get('/api/requests/me', { params });
    },

    getMyRequestDetail: (requestId: string | number): Promise<RequestResponseDto> => {
        return apiClient.get(`/api/requests/me/${requestId}`);
    },

    createRequest: (data: CreateRequestDto): Promise<RequestResponseDto> => {
        return apiClient.post('/api/requests', data, {
            headers: {
                'Idempotency-Key': createIdempotencyKey(),
            },
        });
    },

    updateRequest: (requestId: string | number, data: UpdateRequestDto): Promise<RequestResponseDto> => {
        return apiClient.put(`/api/requests/${requestId}`, data);
    },

    // --- ORDERS (Đơn hàng) ---
    // Lưu ý từ doc: Chưa có API list orders cho customer, chỉ có API xem detail
    getOrderDetail: (orderId: string | number): Promise<OrderResponseDto> => {
        return apiClient.get(`/api/orders/me/${orderId}`);
    },

    getOrderByNhanhId: (nhanhOrderId: string): Promise<OrderResponseDto> => {
        return apiClient.get(`/api/orders/me/by-nhanh/${nhanhOrderId}`);
    },
};
