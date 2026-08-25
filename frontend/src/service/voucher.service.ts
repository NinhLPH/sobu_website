import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import {
    ActiveVoucher,
    ProductVoucherQuery,
    ProductVoucherSummary,
    VoucherApplyRequest,
    VoucherApplyResponse
} from '../interface/voucher.model';

export const VoucherService = {
    getActive: (): Promise<ApiResponseDTO<ActiveVoucher[]>> =>
        apiClient.get('/api/public/vouchers/active'),

    apply: (
        payload: VoucherApplyRequest,
        signal?: AbortSignal
    ): Promise<ApiResponseDTO<VoucherApplyResponse>> =>
        apiClient.post('/api/public/vouchers/apply', payload, { signal }),
    getForProduct: (
        productId: string | number,
        params: ProductVoucherQuery,
        signal?: AbortSignal
    ): Promise<ApiResponseDTO<ProductVoucherSummary[]>> =>
        apiClient.get(`/api/public/vouchers/product/${encodeURIComponent(String(productId))}`, {
            params,
            signal
        })
};
