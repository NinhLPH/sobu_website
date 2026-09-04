import apiClient from '../api/api-client';

export interface NhanhIntegrationStatus {
    enabled: boolean;
}

export const IntegrationService = {
    getNhanhEnabled: (): Promise<NhanhIntegrationStatus> => {
        return apiClient.get('/api/public/integrations/nhanh');
    }
};
