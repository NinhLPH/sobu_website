import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminBanners from './Banners';
import {AdminUiService} from '../../service/admin-ui.service';

jest.mock('../../service/admin-ui.service');
jest.mock('../../service/toast.service');
const mockInvalidateBanners = jest.fn();
const mockFetchBanners = jest.fn(async () => undefined);
jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({invalidateBanners: mockInvalidateBanners, fetchBanners: mockFetchBanners}),
}));

const mockedService = jest.mocked(AdminUiService);

describe('AdminBanners', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedService.searchBanners.mockResolvedValue({
            content: [], pageNumber: 1, pageSize: 10, totalElements: 0, totalPages: 0,
            first: true, last: true, hasNext: false, hasPrevious: false,
        });
    });

    it('requires an image or image URL when creating a banner', async () => {
        render(<AdminBanners/>);
        await waitFor(() => expect(mockedService.searchBanners).toHaveBeenCalled());
        fireEvent.click(screen.getByRole('button', {name: /Thêm banner/i}));
        fireEvent.change(screen.getByLabelText('Tiêu đề'), {target: {value: 'Hero mới'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu banner/i}));

        expect(await screen.findByText('Hãy chọn ảnh hoặc nhập URL ảnh.')).toBeTruthy();
        expect(mockedService.createBanner).not.toHaveBeenCalled();
    });
});
