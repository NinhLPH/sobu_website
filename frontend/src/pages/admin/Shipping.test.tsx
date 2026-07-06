import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AdminShipping from './Shipping';
import { ShippingService } from '../../service/shipping.service';
import { ToastService } from '../../service/toast.service';

jest.mock('../../service/shipping.service');
jest.mock('../../service/toast.service');
jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    )
}), { virtual: true });

const mockedShippingService = jest.mocked(ShippingService);
const mockedToastService = jest.mocked(ToastService);

const configResponse = {
    success: true,
    message: 'Carrier configuration retrieved successfully',
    data: {
        carrierId: 22151,
        standardService: 'VCN',
        expressService: 'VHT',
        expressFallbackId: 22384
    }
};

describe('AdminShipping', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedShippingService.getAdminCarriers.mockResolvedValue({
            success: true,
            message: 'Carriers retrieved successfully from Nhanh',
            data: {
                '22151': {
                    name: 'Nhanh Express',
                    service: 'VCN'
                }
            }
        });
        mockedShippingService.getCarrierConfig.mockResolvedValue(configResponse);
        mockedShippingService.updateCarrierConfig.mockResolvedValue({
            success: true,
            message: 'Carrier configuration updated successfully',
            data: null
        });
        mockedToastService.success.mockImplementation(() => undefined);
    });

    it('loads carrier config and parsed carrier list', async () => {
        render(<AdminShipping />);

        expect(await screen.findByText('Nhanh Express')).toBeTruthy();
        expect(screen.getByText('VCN')).toBeTruthy();
        expect((screen.getByLabelText('Carrier ID') as HTMLInputElement).value).toBe('22151');
        expect((screen.getByLabelText('Standard service') as HTMLInputElement).value).toBe('VCN');
    });

    it('renders raw JSON when carrier payload cannot be normalized', async () => {
        mockedShippingService.getAdminCarriers.mockResolvedValueOnce({
            success: true,
            message: 'Carriers retrieved successfully from Nhanh',
            data: { meta: 'unexpected-shape' }
        });

        render(<AdminShipping />);

        expect(await screen.findByText(/Chua parse duoc danh sach carrier/i)).toBeTruthy();
        expect(screen.getByText(/unexpected-shape/i)).toBeTruthy();
    });

    it('allows blank optional config fields and trims service codes before saving', async () => {
        render(<AdminShipping />);

        await screen.findByText('Nhanh Express');

        fireEvent.change(screen.getByLabelText('Carrier ID'), { target: { value: '' } });
        fireEvent.change(screen.getByLabelText('Standard service'), { target: { value: ' VCN ' } });
        fireEvent.change(screen.getByLabelText('Express service'), { target: { value: '' } });
        fireEvent.change(screen.getByLabelText('Express fallback ID'), { target: { value: '' } });
        fireEvent.click(screen.getByRole('button', { name: /Luu cau hinh/i }));

        await waitFor(() => expect(mockedShippingService.updateCarrierConfig).toHaveBeenCalledWith({
            carrierId: undefined,
            standardService: 'VCN',
            expressService: undefined,
            expressFallbackId: undefined
        }));
        expect(mockedToastService.success).toHaveBeenCalled();
    });

    it('blocks invalid carrier ids before saving', async () => {
        render(<AdminShipping />);

        await screen.findByText('Nhanh Express');

        fireEvent.change(screen.getByLabelText('Carrier ID'), { target: { value: 'abc' } });
        fireEvent.click(screen.getByRole('button', { name: /Luu cau hinh/i }));

        expect(await screen.findByText(/Carrier ID phai la so nguyen duong/i)).toBeTruthy();
        expect(mockedShippingService.updateCarrierConfig).not.toHaveBeenCalled();
    });
});
