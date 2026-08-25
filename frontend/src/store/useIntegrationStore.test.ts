import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {IntegrationService} from '../service/integration.service';
import {useIntegrationStore} from './useIntegrationStore';

jest.mock('../service/integration.service');

const mockedIntegrationService = jest.mocked(IntegrationService);

describe('useIntegrationStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useIntegrationStore.setState({
            nhanhEnabled: false,
            loaded: false,
            loading: false
        });
    });

    it('loads the enabled flag once and deduplicates concurrent calls', async () => {
        let resolveStatus!: (status: {enabled: boolean}) => void;
        mockedIntegrationService.getNhanhEnabled.mockReturnValue(
            new Promise(resolve => {
                resolveStatus = resolve;
            })
        );

        const first = useIntegrationStore.getState().ensureLoaded();
        const second = useIntegrationStore.getState().ensureLoaded();
        expect(mockedIntegrationService.getNhanhEnabled).toHaveBeenCalledTimes(1);

        resolveStatus({enabled: true});
        await Promise.all([first, second]);

        expect(useIntegrationStore.getState()).toMatchObject({
            nhanhEnabled: true,
            loaded: true,
            loading: false
        });
    });

    it('falls back to local mode when the public status request fails', async () => {
        mockedIntegrationService.getNhanhEnabled.mockRejectedValue(new Error('Network unavailable'));

        await useIntegrationStore.getState().ensureLoaded();

        expect(useIntegrationStore.getState()).toMatchObject({
            nhanhEnabled: false,
            loaded: true,
            loading: false
        });
    });
});
