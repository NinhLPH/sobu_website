import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
import { LocationTreeResponse } from '../interface/location.model';

export const LocationService = {
    getLocations: (): Promise<ApiResponseDTO<LocationTreeResponse>> => {
        return apiClient.get('/api/public/locations');
    }
};
