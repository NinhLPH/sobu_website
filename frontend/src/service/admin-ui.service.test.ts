import {describe, expect, it, jest} from '@jest/globals';
import {AdminUiService, buildBannerFormData} from './admin-ui.service';

const mockApiGet = jest.fn();
const mockApiPost = jest.fn();
const mockApiPut = jest.fn();
const mockApiDelete = jest.fn();
jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        get: (...args: any[]) => mockApiGet(...args),
        post: (...args: any[]) => mockApiPost(...args),
        put: (...args: any[]) => mockApiPut(...args),
        delete: (...args: any[]) => mockApiDelete(...args),
    },
}));

describe('buildBannerFormData', () => {
    it('creates the multipart banner JSON part and optional image part', async () => {
        const file = new File(['banner'], 'banner.png', {type: 'image/png'});
        const formData = buildBannerFormData({
            title: 'Hero', imageUrl: '', linkUrl: '/products', displayOrder: 1,
            position: 'home_hero_carousel', isActive: true, deviceType: 'ALL',
        }, file);

        const bannerPart = formData.get('banner') as Blob;
        expect(bannerPart.type).toBe('application/json');
        const bannerJson = await new Promise<string>((resolve) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result));
            reader.readAsText(bannerPart);
        });
        expect(JSON.parse(bannerJson)).toEqual(expect.objectContaining({
            title: 'Hero', position: 'home_hero_carousel', deviceType: 'ALL',
        }));
        expect(formData.get('image')).toBe(file);
    });

    it('bulk updates configs with key-value items only', () => {
        const items = [{key: 'site_name', value: 'SOBU Studio'}];

        AdminUiService.bulkUpdateConfigs(items);

        expect(mockApiPut).toHaveBeenCalledWith('/api/admin/configs/bulk', items);
    });
});
