import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminConfigs from './Configs';
import {AdminUiService} from '../../service/admin-ui.service';

jest.mock('../../service/admin-ui.service');
const mockUploadFile = jest.fn();
jest.mock('../../service/file.service', () => ({
    FileService: {uploadFile: (...args: any[]) => mockUploadFile(...args)},
}));
jest.mock('../../service/toast.service');

const mockInvalidateConfigs = jest.fn();
const mockFetchConfigs = jest.fn(async () => undefined);
jest.mock('../../store/usePublicUiStore', () => ({
    usePublicUiStore: (selector: any) => selector({
        invalidateConfigs: mockInvalidateConfigs,
        fetchConfigs: mockFetchConfigs,
    }),
}));

const mockedService = jest.mocked(AdminUiService);

const pageWith = (content: any[]) => ({
    content,
    pageNumber: 1,
    pageSize: 500,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false,
});

const configs = [
    {id: 1, key: 'primary_color', value: '#00618e', type: 'color', groupName: 'THEME', description: 'Primary', isPublic: true},
    {id: 2, key: 'site_name', value: 'SOBU', type: 'text', groupName: 'GENERAL', description: 'Site name', isPublic: true},
    {id: 3, key: 'free_shipping_threshold', value: '500000', type: 'number', groupName: 'CHECKOUT', description: 'Free shipping', isPublic: true},
    {id: 11, key: 'business_phone', value: '0900000000', type: 'text', groupName: 'BUSINESS', description: 'Business phone', isPublic: true},
    {id: 4, key: 'website_logo', value: 'https://placehold.co/logo.png', type: 'image', groupName: 'THEME', description: 'Logo', isPublic: true},
    {id: 5, key: 'maintenance_mode_enabled', value: 'false', type: 'boolean_type', groupName: 'GENERAL', description: 'Maintenance', isPublic: false},
    {id: 6, key: 'social_links', value: '{"facebook":""}', type: 'json', groupName: 'SOCIAL', description: 'Social', isPublic: true},
    {id: 7, key: 'home_section_01_title', value: 'BAN CHAY', type: 'text', groupName: 'HOME_SECTION', description: 'Home section', isPublic: true},
    {id: 8, key: 'home_promo_grid_top_left_title', value: 'HOT WHEELS', type: 'text', groupName: 'HOME_PROMO', description: 'Home promo', isPublic: true},
    {id: 9, key: 'home_partners_title', value: 'Doi tac', type: 'text', groupName: 'HOME_PARTNER', description: 'Partners', isPublic: true},
    {id: 10, key: 'footer_company_links', value: '[]', type: 'json', groupName: 'FOOTER', description: 'Footer links', isPublic: true},
];

describe('AdminConfigs', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedService.searchConfigs.mockResolvedValue(pageWith(configs));
        mockedService.bulkUpdateConfigs.mockResolvedValue([]);
    });

    it('renders visible tabs from groupName and hides temporarily disabled groups', async () => {
        render(<AdminConfigs/>);

        expect(await screen.findByRole('button', {name: /Giao/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /chung/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /Trang chủ/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /Home promo/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /Đối tác/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /Footer/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /xã hội/i})).toBeTruthy();
        expect(screen.queryByRole('button', {name: /CHECKOUT/i})).toBeNull();
        expect(screen.queryByRole('button', {name: /BUSINESS/i})).toBeNull();
        expect(screen.queryByLabelText(/free_shipping_threshold/)).toBeNull();
        expect(screen.queryByLabelText(/business_phone/)).toBeNull();
        expect(screen.queryByText(/Thêm cấu hình/i)).toBeNull();
        expect(screen.queryByText(/^Xóa$/i)).toBeNull();
    });

    it('renders dynamic controls for supported config types', async () => {
        render(<AdminConfigs/>);

        expect((await screen.findAllByLabelText(/primary_color/)).length).toBeGreaterThan(1);
        expect(screen.getByLabelText(/website_logo/)).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: /chung/i}));
        expect(screen.getByLabelText(/site_name/)).toBeTruthy();
        expect(screen.getByRole('switch')).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: /xã hội/i}));
        expect(await screen.findByText(/social_links/)).toBeTruthy();
        fireEvent.click(screen.getByRole('button', {name: /Code/i}));
        expect(screen.getByLabelText(/social_links/)).toBeTruthy();
    });

    it('validates invalid JSON before calling bulk update', async () => {
        render(<AdminConfigs/>);

        fireEvent.click(await screen.findByRole('button', {name: /xã hội/i}));
        expect(await screen.findByText(/social_links/)).toBeTruthy();
        fireEvent.click(screen.getByRole('button', {name: /Code/i}));
        fireEvent.change(screen.getByLabelText(/social_links/), {target: {value: '{invalid'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu/i}));

        expect(await screen.findByText(/social_links.*JSON/i)).toBeTruthy();
        expect(mockedService.bulkUpdateConfigs).not.toHaveBeenCalled();
    });

    it('saves only the active group with key-value payloads', async () => {
        render(<AdminConfigs/>);

        const primaryColorInputs = await screen.findAllByLabelText(/primary_color/);
        fireEvent.change(primaryColorInputs[1], {target: {value: '#111111'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu/i}));

        await waitFor(() => expect(mockedService.bulkUpdateConfigs).toHaveBeenCalledWith([
            {key: 'primary_color', value: '#111111'},
            {key: 'website_logo', value: 'https://placehold.co/logo.png'},
        ]));
        expect(mockInvalidateConfigs).toHaveBeenCalled();
        expect(mockFetchConfigs).toHaveBeenCalledWith(true);
    });
});
