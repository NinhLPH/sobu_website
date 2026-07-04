import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import {
    ShippingQuoteDto,
    ShippingQuoteRequestDto
} from '../interface/shipping.model';

export const ShippingService = {
    getQuotes: (
        data: ShippingQuoteRequestDto
    ): Promise<ApiResponseDTO<ShippingQuoteDto[]>> => {
        return apiClient.post('/api/public/shipping/quotes', data);
    }
};
