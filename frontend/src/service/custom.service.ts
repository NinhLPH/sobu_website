import apiClient from '../api/api-client';
import { ApiResponseDTO, PageResponse } from '../interface/api-response';
import {
    CreateRequestDto,
    RequestResponseDto,
    UpdateRequestDto
} from '../interface/customer-request.model';
import {
    CreateNormalOrderDto,
    CreateOrderPaymentDto,
    OrderPaymentResponseDto,
    OrderResponseDto
} from '../interface/order.model';
import { createIdempotencyKey } from '../utils/idempotency';
import { CartDto } from '../interface/cart.dto';

export const CustomerService = {
    getMyRequests: (
        params?: Record<string, unknown>
    ): Promise<ApiResponseDTO<PageResponse<RequestResponseDto>>> => {
        return apiClient.get('/api/requests/me', { params });
    },

    getRequestDetail: (
        requestId: string | number
    ): Promise<ApiResponseDTO<RequestResponseDto>> => {
        return apiClient.get(`/api/requests/me/${requestId}`);
    },

    createRequest: (
        data: CreateRequestDto,
        idempotencyKey?: string
    ): Promise<ApiResponseDTO<RequestResponseDto>> => {
        return apiClient.post(
            '/api/requests',
            data,
            idempotencyKey
                ? { headers: { 'Idempotency-Key': idempotencyKey } }
                : undefined
        );
    },

    updateRequest: (
        requestId: string | number,
        data: UpdateRequestDto
    ): Promise<ApiResponseDTO<RequestResponseDto>> => {
        return apiClient.put(`/api/requests/${requestId}`, data);
    },

    createOrder: (
        data: CreateNormalOrderDto,
        idempotencyKey?: string
    ): Promise<ApiResponseDTO<OrderResponseDto>> => {
        return apiClient.post('/api/orders', data, {
            headers: {
                'Idempotency-Key': idempotencyKey || createIdempotencyKey()
            }
        });
    },

    getMyOrder: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderResponseDto>> => {
        return apiClient.get(`/api/orders/me/${orderId}`);
    },

    getOrderByNhanhId: (
        nhanhOrderId: string
    ): Promise<ApiResponseDTO<OrderResponseDto>> => {
        return apiClient.get(`/api/orders/me/by-nhanh/${encodeURIComponent(nhanhOrderId)}`);
    },

    getOrderPayments: (
        orderId: string | number
    ): Promise<ApiResponseDTO<OrderPaymentResponseDto[]>> => {
        return apiClient.get(`/api/orders/${orderId}/payments`);
    },

    createOrderPayment: (
        orderId: string | number,
        data: CreateOrderPaymentDto,
        idempotencyKey?: string
    ): Promise<ApiResponseDTO<OrderPaymentResponseDto>> => {
        return apiClient.post(`/api/orders/${orderId}/payments`, data, {
            headers: {
                'Idempotency-Key': idempotencyKey || createIdempotencyKey()
            }
        });
    },

    getCart: (): Promise<ApiResponseDTO<CartDto>> => {
        return apiClient.get('/api/cart');
    },

    addCartItem: (data: {
        productId: string;
        nhanhProductId?: string;
        name: string;
        price: number;
        imageUrl?: string;
        quantity: number;
    }): Promise<ApiResponseDTO<CartDto>> => {
        return apiClient.post('/api/cart/items', data);
    },

    updateCartItem: (
        productId: string,
        quantity: number
    ): Promise<ApiResponseDTO<CartDto>> => {
        return apiClient.put(`/api/cart/items/${encodeURIComponent(productId)}`, { quantity });
    },

    removeCartItem: (productId: string): Promise<ApiResponseDTO<void>> => {
        return apiClient.delete(`/api/cart/items/${encodeURIComponent(productId)}`);
    },

    clearCart: (): Promise<ApiResponseDTO<void>> => {
        return apiClient.delete('/api/cart');
    }
};
