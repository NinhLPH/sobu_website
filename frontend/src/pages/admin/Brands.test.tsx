import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import AdminBrands from './Brands';
import {AdminCatalogService} from '../../service/admin-catalog.service';

jest.mock('../../service/admin-catalog.service');
jest.mock('../../service/toast.service');

const brands = [{id: 1, name: 'Sodu', code: 'SODU', status: 1}, {id: 2, name: 'Melia', code: 'MELIA', status: 1}, {id: 3, name: 'Bandai', code: 'BANDAI', status: 0}];
describe('AdminBrands API catalog', () => {
    beforeEach(() => { jest.clearAllMocks(); (AdminCatalogService.getBrands as any).mockResolvedValue(brands); });
    it('renders and filters brands loaded from the admin API', async () => {
        render(<AdminBrands/>);
        expect(await screen.findByText('Bandai')).toBeTruthy();
        fireEvent.change(screen.getByLabelText('Tìm kiếm thương hiệu quản trị'), {target: {value: 'ban'}});
        expect(screen.getByText('Bandai')).toBeTruthy();
        expect(screen.queryByText('Sodu')).toBeNull();
        expect(screen.queryByText('Melia')).toBeNull();
    });
});
