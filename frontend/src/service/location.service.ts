import axios from 'axios';
import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import {
    AddressDatasetResponse,
    ExternalLocationProvince,
    ExternalLocationWard,
    LocationProvince,
    LocationWard,
    ProvinceListResponse,
    WardListResponse
} from '../interface/location.model';

const EXTERNAL_DATASET_VERSION = 'api-v2';
const externalLocationClient = axios.create({
    baseURL: process.env.REACT_APP_LOCATION_API_BASE_URL?.trim()
        || 'https://provinces.open-api.vn/api/v2',
    headers: { Accept: 'application/json' },
    timeout: 15_000
});

const isPositiveCode = (value: unknown): value is number =>
    typeof value === 'number' && Number.isInteger(value) && value > 0;

const isLocationName = (value: unknown): value is string =>
    typeof value === 'string' && value.trim().length > 0;

const normalizeProvinces = (payload: unknown): LocationProvince[] => {
    if (!Array.isArray(payload) || payload.length === 0) {
        throw new Error('External location API returned an empty province list.');
    }
    const provinces = payload.map((item: ExternalLocationProvince) => {
        if (!item || !isPositiveCode(item.code) || !isLocationName(item.name)) {
            throw new Error('External location API returned an invalid province.');
        }
        return { id: item.code, name: item.name.trim() };
    });
    return provinces.sort((left, right) => left.name.localeCompare(right.name, 'vi'));
};

const normalizeWards = (payload: unknown, provinceId: number): LocationWard[] => {
    if (!Array.isArray(payload) || payload.length === 0) {
        throw new Error('External location API returned an empty ward list.');
    }
    const wards = payload.map((item: ExternalLocationWard) => {
        if (!item || !isPositiveCode(item.code) || !isLocationName(item.name)
            || item.province_code !== provinceId) {
            throw new Error('External location API returned a ward outside the selected province.');
        }
        return { id: item.code, name: item.name.trim() };
    });
    return wards.sort((left, right) => left.name.localeCompare(right.name, 'vi'));
};

export const LocationService = {
    getBackendProvinces: (
        signal?: AbortSignal
    ): Promise<ApiResponseDTO<ProvinceListResponse>> => {
        return apiClient.get('/api/public/address/provinces', { signal });
    },
    getBackendWards: (
        provinceId: number,
        signal?: AbortSignal
    ): Promise<ApiResponseDTO<WardListResponse>> => {
        return apiClient.get('/api/public/address/wards', {
            params: { provinceId },
            signal
        });
    },
    getExternalProvinces: async (
        signal?: AbortSignal
    ): Promise<ProvinceListResponse> => {
        const response = await externalLocationClient.get<ExternalLocationProvince[]>('/p/', { signal });
        return {
            datasetVersion: EXTERNAL_DATASET_VERSION,
            provinces: normalizeProvinces(response.data)
        };
    },
    getExternalWards: async (
        provinceId: number,
        signal?: AbortSignal
    ): Promise<WardListResponse> => {
        const response = await externalLocationClient.get<ExternalLocationWard[]>('/w/', {
            params: { province: provinceId },
            signal
        });
        return {
            datasetVersion: EXTERNAL_DATASET_VERSION,
            wards: normalizeWards(response.data, provinceId)
        };
    },
    getCurrentDataset: (): Promise<ApiResponseDTO<AddressDatasetResponse>> => {
        return apiClient.get('/api/public/address/datasets/current');
    }
};
