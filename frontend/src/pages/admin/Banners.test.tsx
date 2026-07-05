import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminBanners from './Banners';
import {AdminUiService, buildBannerFormData} from '../../service/admin-ui.service';

jest.mock('../../service/admin-ui.service');
jest.mock('../../service/toast.service');
const mockInvalidateBanners = jest.fn();
const mockFetchBanners = jest.fn(async () => undefined);
jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({invalidateBanners: mockInvalidateBanners, fetchBanners: mockFetchBanners}),
}));

const mockedService = jest.mocked(AdminUiService);
const mockedBuildBannerFormData = jest.mocked(buildBannerFormData);

const banner = {
    id: 1,
    title: 'Hero seed',
    imageUrl: '/images/hero.jpg',
    linkUrl: '/products',
    displayOrder: 1,
    position: 'home_hero_carousel' as const,
    isActive: true,
    startDate: '2026-05-01T00:00:00',
    endDate: undefined,
    deviceType: 'ALL' as const,
};

describe('AdminBanners', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedBuildBannerFormData.mockImplementation((payload, image) => {
            const formData = new FormData();
            formData.append('banner', new Blob([JSON.stringify(payload)], {type: 'application/json'}));
            if (image) formData.append('image', image);
            return formData;
        });
        mockedService.updateBanner.mockResolvedValue(banner);
        mockedService.searchBanners.mockResolvedValue({
            content: [], pageNumber: 1, pageSize: 10, totalElements: 0, totalPages: 0,
            first: true, last: true, hasNext: false, hasPrevious: false,
        });
    });

    it('renders seeded banners as edit-only rows', async () => {
        mockedService.searchBanners.mockResolvedValue({
            content: [banner], pageNumber: 1, pageSize: 10, totalElements: 1, totalPages: 1,
            first: true, last: true, hasNext: false, hasPrevious: false,
        });

        render(<AdminBanners/>);
        expect(await screen.findByText('Hero seed')).toBeTruthy();

        expect(screen.queryByRole('button', {name: /Thêm banner/i})).toBeNull();
        expect(screen.queryByRole('button', {name: /Xóa banner/i})).toBeNull();
        expect(screen.queryByRole('button', {name: /Vô hiệu hóa banner/i})).toBeNull();
    });

    it('selects a banner suggestion and searches immediately', async () => {
        mockedService.searchBanners.mockResolvedValue({
            content: [banner], pageNumber: 1, pageSize: 10, totalElements: 1, totalPages: 1,
            first: true, last: true, hasNext: false, hasPrevious: false,
        });

        render(<AdminBanners/>);
        await screen.findByText('Hero seed');

        fireEvent.change(screen.getByLabelText('Tìm banner'), {target: {value: 'hero'}});
        fireEvent.mouseDown(screen.getByRole('option', {name: /Hero seed/i}));

        await waitFor(() => expect(mockedService.searchBanners).toHaveBeenLastCalledWith('Hero seed', {
            page: 1,
            pageSize: 10,
            sortBy: 'displayOrder',
            sortDirection: 'ASC',
        }));
    });

    it('keeps banner position read-only while updating editable fields', async () => {
        mockedService.searchBanners.mockResolvedValue({
            content: [banner], pageNumber: 1, pageSize: 10, totalElements: 1, totalPages: 1,
            first: true, last: true, hasNext: false, hasPrevious: false,
        });

        render(<AdminBanners/>);
        fireEvent.click(await screen.findByRole('button', {name: /Sửa banner Hero seed/i}));

        expect(screen.getAllByText('home_hero_carousel')).toHaveLength(2);
        expect(screen.queryByText('Home hero carousel')).toBeNull();
        expect(screen.queryByRole('option', {name: 'site_left_sidebar_banner'})).toBeNull();

        fireEvent.change(screen.getByLabelText('Tiêu đề'), {target: {value: 'Hero updated'}});
        fireEvent.change(screen.getByDisplayValue('/images/hero.jpg'), {target: {value: '/images/hero-updated.jpg'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu banner/i}));

        await waitFor(() => expect(mockedService.updateBanner).toHaveBeenCalled());
        const [id, formData] = mockedService.updateBanner.mock.calls[0];
        expect(id).toBe(1);
        const bannerBlob = (formData as FormData).get('banner') as Blob;
        const bannerJson = await new Promise<string>((resolve) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result));
            reader.readAsText(bannerBlob);
        });
        expect(JSON.parse(bannerJson)).toEqual(expect.objectContaining({
            title: 'Hero updated',
            imageUrl: '/images/hero-updated.jpg',
            position: 'home_hero_carousel',
        }));
        expect(mockInvalidateBanners).toHaveBeenCalled();
        expect(mockFetchBanners).toHaveBeenCalledWith(true);
    });
});
