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
    {id: 4, key: 'website_logo', value: 'https://placehold.co/logo.png', type: 'image', groupName: 'THEME', description: 'Logo', isPublic: true},
    {id: 5, key: 'maintenance_mode_enabled', value: 'false', type: 'boolean_type', groupName: 'GENERAL', description: 'Maintenance', isPublic: false},
    {id: 6, key: 'social_links', value: '{"facebook":""}', type: 'json', groupName: 'SOCIAL', description: 'Social', isPublic: true},
];

describe('AdminConfigs', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedService.searchConfigs.mockResolvedValue(pageWith(configs));
        mockedService.bulkUpdateConfigs.mockResolvedValue([]);
    });

    it('renders tabs from groupName and does not expose create/delete actions', async () => {
        render(<AdminConfigs/>);

        expect(await screen.findByRole('button', {name: /Giao diện/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /Thông tin chung/i})).toBeTruthy();
        expect(screen.getByRole('button', {name: /Mạng xã hội/i})).toBeTruthy();
        expect(screen.queryByText(/Thêm cấu hình/i)).toBeNull();
        expect(screen.queryByText(/^Xóa$/i)).toBeNull();
    });

    it('renders dynamic controls for supported config types', async () => {
        render(<AdminConfigs/>);

        expect(await screen.findByLabelText('Giá trị primary_color')).toBeTruthy();
        expect(screen.getByLabelText('Giá trị website_logo')).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: /Thông tin chung/i}));
        expect(screen.getByLabelText('Giá trị site_name')).toBeTruthy();
        expect(screen.getByRole('switch')).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: /Thanh toán/i}));
        expect(screen.getByLabelText('Giá trị free_shipping_threshold')).toBeTruthy();

        fireEvent.click(screen.getByRole('button', {name: /Mạng xã hội/i}));
        expect(screen.getByLabelText('Giá trị social_links')).toBeTruthy();
    });

    it('validates invalid JSON before calling bulk update', async () => {
        render(<AdminConfigs/>);

        fireEvent.click(await screen.findByRole('button', {name: /Mạng xã hội/i}));
        fireEvent.change(screen.getByLabelText('Giá trị social_links'), {target: {value: '{invalid'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu thay đổi/i}));

        expect(await screen.findByText('social_links phải là JSON hợp lệ.')).toBeTruthy();
        expect(mockedService.bulkUpdateConfigs).not.toHaveBeenCalled();
    });

    it('saves only the active group with key-value payloads', async () => {
        render(<AdminConfigs/>);

        fireEvent.change(await screen.findByLabelText('Giá trị primary_color'), {target: {value: '#111111'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu thay đổi/i}));

        await waitFor(() => expect(mockedService.bulkUpdateConfigs).toHaveBeenCalledWith([
            {key: 'primary_color', value: '#111111'},
            {key: 'website_logo', value: 'https://placehold.co/logo.png'},
        ]));
        expect(mockInvalidateConfigs).toHaveBeenCalled();
        expect(mockFetchConfigs).toHaveBeenCalledWith(true);
    });
});
