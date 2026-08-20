import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import {
    AddressDatasetResponse,
    ProvinceListResponse,
    WardListResponse
} from '../interface/location.model';

export const LocationService = {
    getProvinces: (): Promise<ApiResponseDTO<ProvinceListResponse>> => {
        return apiClient.get('/api/public/address/provinces');
    },
    getWards: (provinceId: number): Promise<ApiResponseDTO<WardListResponse>> => {
        return apiClient.get('/api/public/address/wards', {
            params: { provinceId }
        });
    },
    getCurrentDataset: (): Promise<ApiResponseDTO<AddressDatasetResponse>> => {
        return apiClient.get('/api/public/address/datasets/current');
    }
};
