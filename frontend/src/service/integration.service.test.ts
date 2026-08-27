import {describe, expect, it, jest} from '@jest/globals';
import {IntegrationService} from './integration.service';

const mockGet = jest.fn<Promise<{enabled: boolean}>, [string]>();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {get: (url: string) => mockGet(url)}
}));

describe('IntegrationService', () => {
    it('requests and returns the unwrapped public Nhanh status', async () => {
        mockGet.mockResolvedValue({enabled: false});

        await expect(IntegrationService.getNhanhEnabled()).resolves.toEqual({enabled: false});
        expect(mockGet).toHaveBeenCalledWith('/api/public/integrations/nhanh');
    });
});
