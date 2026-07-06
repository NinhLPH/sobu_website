import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import {
    AdminShippingCarriersPayload,
    CarrierConfigDto,
    CarrierConfigRequestDto,
    ShippingQuoteDto,
    ShippingQuoteRequestDto
} from '../interface/shipping.model';

export const ShippingService = {
    getQuotes: (
        data: ShippingQuoteRequestDto
    ): Promise<ApiResponseDTO<ShippingQuoteDto[]>> => {
        return apiClient.post('/api/public/shipping/quotes', data);
    },

    getAdminCarriers: (): Promise<ApiResponseDTO<AdminShippingCarriersPayload>> => {
        return apiClient.get('/api/admin/shipping/carriers');
    },

    getCarrierConfig: (): Promise<ApiResponseDTO<CarrierConfigDto>> => {
        return apiClient.get('/api/admin/shipping/carrier-config');
    },

    updateCarrierConfig: (
        data: CarrierConfigRequestDto
    ): Promise<ApiResponseDTO<null>> => {
        return apiClient.put('/api/admin/shipping/carrier-config', data);
    }
};
