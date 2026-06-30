import {describe, expect, it, jest} from '@jest/globals';
import {PublicUiService} from './public-ui.service';

const mockApiGet = jest.fn();
jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {get: (...args: any[]) => mockApiGet(...args)},
}));

describe('PublicUiService', () => {
    it('loads all banners once when no filter is supplied', () => {
        PublicUiService.getBanners();
        expect(mockApiGet).toHaveBeenCalledWith('/api/public/ui/banners', {params: undefined});
    });

    it('passes supported banner filters and loads configs', () => {
        PublicUiService.getBanners({deviceType: 'WEB', position: 'home_hero_carousel'});
        PublicUiService.getConfigs();
        expect(mockApiGet).toHaveBeenCalledWith('/api/public/ui/banners', {params: {deviceType: 'WEB', position: 'home_hero_carousel'}});
        expect(mockApiGet).toHaveBeenCalledWith('/api/public/ui/configs');
    });
});
