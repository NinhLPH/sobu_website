import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import {
    ActiveVoucher,
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
        apiClient.post('/api/public/vouchers/apply', payload, { signal })
};
