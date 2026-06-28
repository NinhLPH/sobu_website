import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {PublicUiService} from '../service/public-ui.service';
import {
    getBannersForPlacement,
    getPublicConfigValue,
    resetPublicUiRequestsForTests,
    usePublicUiStore,
} from './usePublicUiStore';

jest.mock('../service/public-ui.service');
const mockedPublicUiService = jest.mocked(PublicUiService);

const banners = [
    {id: 3, title: 'All second', imageUrl: 'three.jpg', position: 'HOME_TOP', deviceType: 'ALL', isActive: true, displayOrder: 2},
    {id: 1, title: 'Web first', imageUrl: 'one.jpg', position: 'HOME_TOP', deviceType: 'WEB', isActive: true, displayOrder: 1},
    {id: 2, title: 'Mobile', imageUrl: 'two.jpg', position: 'HOME_TOP', deviceType: 'MOBILE', isActive: true, displayOrder: 0},
    {id: 4, title: 'Inactive', imageUrl: 'four.jpg', position: 'HOME_TOP', deviceType: 'ALL', isActive: false, displayOrder: 0},
] as any;

describe('usePublicUiStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        resetPublicUiRequestsForTests();
        usePublicUiStore.setState({
            banners: [], configs: [], configMap: {}, bannersLoaded: false, configsLoaded: false,
            isBannersLoading: false, isConfigsLoading: false, bannersError: null, configsError: null,
        });
    });

    it('deduplicates banner requests and stores the public result', async () => {
        mockedPublicUiService.getBanners.mockResolvedValue(banners);
        await Promise.all([
            usePublicUiStore.getState().fetchBanners(),
            usePublicUiStore.getState().fetchBanners(),
        ]);

        expect(mockedPublicUiService.getBanners).toHaveBeenCalledTimes(1);
        expect(usePublicUiStore.getState().banners).toHaveLength(4);
        expect(usePublicUiStore.getState().bannersLoaded).toBe(true);
    });

    it('filters by placement/device and sorts by displayOrder', () => {
        expect(getBannersForPlacement(banners, 'HOME_TOP', 'WEB').map((item) => item.id)).toEqual([1, 3]);
        expect(getBannersForPlacement(banners, 'HOME_TOP', 'MOBILE').map((item) => item.id)).toEqual([2, 3]);
    });

    it('reads public config values and falls back for missing/private keys', () => {
        const configs = [
            {id: 1, key: 'site_name', value: 'SOBU API', type: 'text', isPublic: true},
            {id: 2, key: 'secret', value: 'hidden', type: 'text', isPublic: false},
        ] as any;
        expect(getPublicConfigValue(configs, 'site_name', 'SOBU')).toBe('SOBU API');
        expect(getPublicConfigValue(configs, 'secret', 'fallback')).toBe('fallback');
        expect(getPublicConfigValue(configs, 'missing', 'fallback')).toBe('fallback');
    });

    it('stores public configs as both an array and flat key-value map', async () => {
        mockedPublicUiService.getConfigs.mockResolvedValue([
            {id: 1, key: 'site_name', value: 'SOBU API', type: 'text', isPublic: true},
            {id: 2, key: 'secret', value: 'hidden', type: 'text', isPublic: false},
        ] as any);

        await usePublicUiStore.getState().fetchConfigs();

        expect(mockedPublicUiService.getConfigs).toHaveBeenCalledTimes(1);
        expect(usePublicUiStore.getState().configs).toHaveLength(2);
        expect(usePublicUiStore.getState().configMap).toEqual({site_name: 'SOBU API'});
    });
});
