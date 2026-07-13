import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import Cart from './Cart';
import { useCartStore } from '../store/useCartStore';
import { useAuthStore } from '../store/useAuthStore';
import { useLocationStore } from '../store/useLocationStore';
import { usePaymentStore } from '../store/usePaymentStore';
import { usePublicUiStore } from '../store/usePublicUiStore';
import { redirectToPaymentCheckout } from '../utils/payment-session';
import { onlineCartRecovery } from '../utils/online-cart-recovery';
import { ShippingService } from '../service/shipping.service';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    ),
    useNavigate: () => mockNavigate
}), { virtual: true });
jest.mock('../store/useCartStore');
jest.mock('../store/useAuthStore');
jest.mock('../store/useLocationStore');
jest.mock('../store/usePaymentStore');
jest.mock('../service/shipping.service');
jest.mock('../utils/payment-session', () => ({
    redirectToPaymentCheckout: require('@jest/globals').jest.fn()
}));
jest.mock('../utils/online-cart-recovery', () => ({
    onlineCartRecovery: {
        save: require('@jest/globals').jest.fn(),
        get: require('@jest/globals').jest.fn(),
        clear: require('@jest/globals').jest.fn()
    }
}));

const mockedUseCartStore = jest.mocked(useCartStore);
const mockedUseAuthStore = jest.mocked(useAuthStore);
const mockedUseLocationStore = jest.mocked(useLocationStore);
const mockedUsePaymentStore = jest.mocked(usePaymentStore);
const mockedRedirectToPaymentCheckout = jest.mocked(redirectToPaymentCheckout);
const mockedOnlineCartRecovery = jest.mocked(onlineCartRecovery);
const mockedShippingService = jest.mocked(ShippingService);
const mockSubmitOrder = jest.fn<Promise<any>, any[]>();
const mockCreatePayment = jest.fn<Promise<any>, any[]>();

const product = {
    id: '10',
    nhanhProductId: '10001',
    name: 'Áo hoodie',
    price: 350000,
    brand: 'SOBU',
    imageUrl: '/test-product.png',
    description: '',
    stock: 5
};

const locationTree = {
    stale: false,
    cities: [{
        cityId: 1,
        cityName: 'Hà Nội',
        districts: [{
            districtId: 2,
            districtName: 'Ba Đình',
            wards: [{ wardId: 3, wardName: 'Phúc Xá' }]
        }]
    }]
};

const shippingQuote = {
    carrierId: 29,
    carrierName: 'GHN',
    carrierServiceId: 186,
    carrierServiceName: 'Standard',
    shipFee: 32000,
    customerShipFee: 30000,
    deliveryTime: '2-3 days',
    description: 'Door delivery'
};

const expressShippingQuote = {
    carrierId: 29,
    carrierName: 'AhaMove',
    carrierServiceId: 186,
    carrierServiceName: 'Hoa toc',
    shipFee: 14000,
    customerShipFee: 14000,
    deliveryTime: null,
    description: null
};

const shippingQuotes = [shippingQuote, expressShippingQuote];

const stringCarrierShippingQuote = {
    carrierId: '29',
    carrierName: 'Viettel Post',
    carrierServiceId: '186',
    carrierServiceName: 'Nhanh',
    shipFee: '22000',
    customerShipFee: '21000',
    deliveryTime: null,
    description: null
};

const temporarySupportedShippingQuotes = [
    {
        carrierId: 29,
        carrierName: null,
        carrierServiceId: 186,
        carrierServiceName: null,
        shipFee: 14000,
        customerShipFee: 14000,
        deliveryTime: null,
        description: null
    },
    {
        carrierId: 29,
        carrierName: null,
        carrierServiceId: 186,
        carrierServiceName: 'Hỏa tốc (null)',
        shipFee: 14000,
        customerShipFee: 14000,
        deliveryTime: null,
        description: null
    }
];

const invalidShippingQuote = {
    carrierId: 11,
    carrierName: 'Broken carrier',
    carrierServiceId: null,
    carrierServiceName: 'Missing service',
    shipFee: null,
    customerShipFee: null,
    deliveryTime: null,
    description: null
};

describe('Cart payment selection', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockSubmitOrder.mockResolvedValue({ id: 12 });
        mockedShippingService.getQuotes.mockResolvedValue({
            success: true,
            statusCode: 200,
            message: 'Shipping quotes retrieved',
            data: shippingQuotes
        });
        mockedUseCartStore.mockReturnValue({
            items: [{ product, quantity: 1 }],
            removeFromCart: jest.fn(),
            updateQuantity: jest.fn(),
            getTotals: () => ({ subtotal: 350000, totalDiscount: 0, total: 350000, itemCount: 1 }),
            submitOrder: mockSubmitOrder,
            isSubmitting: false,
            checkoutError: null,
            clearCheckoutError: jest.fn(),
            fetchCart: jest.fn()
        } as unknown as ReturnType<typeof useCartStore>);
        mockedUseAuthStore.mockReturnValue({
            isAuthenticated: true,
            user: {
                fullName: 'Nguyễn Văn A',
                phone: '0901234567',
                email: 'customer@example.com'
            }
        } as ReturnType<typeof useAuthStore>);
        mockedUseLocationStore.mockReturnValue({
            locationTree,
            locationsLoaded: true,
            isLoading: false,
            error: null,
            fetchLocations: jest.fn(),
            cancelScheduledRetry: jest.fn()
        } as unknown as ReturnType<typeof useLocationStore>);
        mockedUsePaymentStore.mockReturnValue({
            createPayment: mockCreatePayment,
            isCreatingPayment: false
        } as ReturnType<typeof usePaymentStore>);
        usePublicUiStore.setState({
            configMap: {
                social_links: JSON.stringify({
                    facebook: 'https://facebook.com/sobu'
                })
            },
            configsLoaded: true
        });
    });

    const selectLocationOption = (label: string, name: string) => {
        fireEvent.click(screen.getByRole('button', { name: label }));
        fireEvent.click(screen.getByRole('option', { name: new RegExp(name) }));
    };

    const selectShippingLocation = () => {
        selectLocationOption('Tỉnh/Thành phố', locationTree.cities[0].cityName);
        selectLocationOption('Quận/Huyện', locationTree.cities[0].districts[0].districtName);
        selectLocationOption('Phường/Xã', locationTree.cities[0].districts[0].wards[0].wardName);
    };

    const getCheckoutButton = () =>
        screen.getByRole('button', { name: /Đặt hàng|thanh toán|COD|Dang xac nhan/i }) as HTMLButtonElement;

    const clickShippingQuote = async (name: RegExp = /Tiêu chuẩn/i) => {
        const option = await screen.findByRole('button', { name });
        fireEvent.click(option);
    };

    const selectShippingQuote = async () => {
        await clickShippingQuote();
        await waitFor(() => expect(getCheckoutButton().disabled).toBe(false));
    };

    it('requests shipping quotes after the customer selects a full location', async () => {
        render(<Cart />);
        selectShippingLocation();

        await waitFor(() => expect(mockedShippingService.getQuotes).toHaveBeenCalledWith({
            customerCityId: 1,
            customerDistrictId: 2,
            customerWardId: 3,
            cartSubtotal: 350000,
            codAmount: 0
        }));
        expect(await screen.findByRole('button', { name: /Tiêu chuẩn/i })).not.toBeNull();
        expect(screen.getByRole('button', { name: /Hỏa tốc/i })).not.toBeNull();
        const facebookSupportLink = screen.getByRole('link', { name: /Facebook hỗ trợ/i }) as HTMLAnchorElement;
        expect(facebookSupportLink.getAttribute('href')).toBe('https://facebook.com/sobu');
        expect(screen.getByText(/giao hàng nhanh trong vòng 4 giờ/i)).toBeTruthy();
        expect(screen.queryByText('Khong co tuy chon giao hang hop le. Vui long thu lai hoac chon dia chi khac.')).toBeNull();
        expect(screen.getAllByText(/30\.000/)).not.toHaveLength(0);
    });

    it('only renders the temporarily supported carrier service', async () => {
        mockedShippingService.getQuotes.mockResolvedValueOnce({
            success: true,
            statusCode: 200,
            message: 'Shipping quotes retrieved',
            data: [
                shippingQuote,
                {
                    ...expressShippingQuote,
                    carrierId: 30,
                    carrierServiceId: 40,
                    carrierName: 'Unsupported carrier'
                }
            ]
        });

        render(<Cart />);
        selectShippingLocation();

        expect(await screen.findByText(/GHN/)).toBeTruthy();
        expect(screen.queryByText('Unsupported carrier')).toBeNull();
    });

    it('keeps checkout disabled when the response has no temporarily supported quote', async () => {
        mockedShippingService.getQuotes.mockResolvedValueOnce({
            success: true,
            statusCode: 200,
            message: 'Shipping quotes retrieved',
            data: [{
                ...shippingQuote,
                carrierId: 30,
                carrierServiceId: 40,
                carrierName: 'Unsupported carrier'
            }]
        });

        render(<Cart />);
        selectShippingLocation();

        await waitFor(() => expect(mockedShippingService.getQuotes).toHaveBeenCalledTimes(1));
        await waitFor(() => expect(
            screen.getByText('Khong co tuy chon giao hang hop le. Vui long thu lai hoac chon dia chi khac.')
        ).toBeTruthy());
        expect(screen.queryByText('Unsupported carrier')).toBeNull();
        expect(getCheckoutButton().disabled).toBe(true);
    });

    it('does not request new shipping quotes when the detailed address changes', async () => {
        render(<Cart />);
        selectShippingLocation();

        await waitFor(() => expect(mockedShippingService.getQuotes).toHaveBeenCalledTimes(1));

        fireEvent.change(screen.getByPlaceholderText(/Địa chỉ giao hàng chi tiết/), {
            target: { value: '123 Nguyen Trai' }
        });

        expect(mockedShippingService.getQuotes).toHaveBeenCalledTimes(1);
    });

    it('confirms temporarily supported quotes before creating an order', async () => {
        mockedShippingService.getQuotes
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quotes retrieved',
                data: temporarySupportedShippingQuotes
            })
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quote confirmed',
                data: temporarySupportedShippingQuotes
            });

        render(<Cart />);
        selectShippingLocation();

        const standardOption = await screen.findByRole('button', { name: /Tiêu chuẩn/i });
        expect(screen.getByRole('button', { name: /Hỏa tốc/i })).not.toBeNull();
        expect(screen.queryByText('Khong co tuy chon giao hang hop le. Vui long thu lai hoac chon dia chi khac.')).toBeNull();

        fireEvent.click(standardOption);

        await waitFor(() => expect(getCheckoutButton().disabled).toBe(false));
        expect(mockedShippingService.getQuotes).toHaveBeenCalledTimes(2);

        fireEvent.click(getCheckoutButton());

        await waitFor(() => expect(mockSubmitOrder).toHaveBeenCalledWith(expect.objectContaining({
            carrierId: 29,
            carrierServiceId: 186,
            shippingFee: 14000
        }), {
            clearCartOnSuccess: false
        }));
    });

    it('does not render invalid shipping quotes or enable checkout', async () => {
        mockedShippingService.getQuotes.mockResolvedValueOnce({
            success: true,
            statusCode: 200,
            message: 'Shipping quotes retrieved',
            data: [invalidShippingQuote]
        });

        render(<Cart />);
        selectShippingLocation();

        await waitFor(() => expect(
            screen.getByText('Khong co tuy chon giao hang hop le. Vui long thu lai hoac chon dia chi khac.')
        ).toBeTruthy());
        expect(screen.queryByRole('button', { name: /Broken carrier/i })).toBeNull();
        expect(getCheckoutButton().disabled).toBe(true);
        expect(mockSubmitOrder).not.toHaveBeenCalled();
    });

    it('renders carrier quotes when ids and fees are returned as numeric strings', async () => {
        mockedShippingService.getQuotes
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quotes retrieved',
                data: [stringCarrierShippingQuote]
            })
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quote confirmed',
                data: [stringCarrierShippingQuote]
            });

        render(<Cart />);
        selectShippingLocation();

        await clickShippingQuote(/Tiêu chuẩn/i);
        await waitFor(() => expect(getCheckoutButton().disabled).toBe(false));

        fireEvent.click(getCheckoutButton());

        await waitFor(() => expect(mockSubmitOrder).toHaveBeenCalledWith(expect.objectContaining({
            carrierId: 29,
            carrierServiceId: 186,
            shippingFee: 21000
        }), {
            clearCartOnSuccess: false
        }));
    });

    it('confirms the selected carrier quote before enabling checkout', async () => {
        let resolveConfirmQuote: (value: any) => void = () => undefined;
        mockedShippingService.getQuotes
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quotes retrieved',
                data: [shippingQuote]
            })
            .mockImplementationOnce(() => new Promise((resolve) => {
                resolveConfirmQuote = resolve;
            }));

        render(<Cart />);
        selectShippingLocation();

        await clickShippingQuote(/Tiêu chuẩn/i);

        await waitFor(() => expect(mockedShippingService.getQuotes).toHaveBeenLastCalledWith({
            customerCityId: 1,
            customerDistrictId: 2,
            customerWardId: 3,
            cartSubtotal: 350000,
            codAmount: 0,
            carrierId: 29,
            carrierServiceId: 186
        }));

        expect(getCheckoutButton().disabled).toBe(true);
        expect(mockSubmitOrder).not.toHaveBeenCalled();

        resolveConfirmQuote({
            success: true,
            statusCode: 200,
            message: 'Shipping quote confirmed',
            data: [
                {
                    ...shippingQuote,
                    carrierId: 30,
                    carrierServiceId: 40,
                    carrierName: 'Unsupported carrier'
                },
                shippingQuote
            ]
        });

        await waitFor(() => expect(getCheckoutButton().disabled).toBe(false));
        fireEvent.click(getCheckoutButton());
        await waitFor(() => expect(mockSubmitOrder).toHaveBeenCalledWith(expect.objectContaining({
            carrierId: 29,
            carrierServiceId: 186
        }), {
            clearCartOnSuccess: false
        }));
    });

    it('keeps checkout disabled when quote confirmation no longer returns the selected option', async () => {
        mockedShippingService.getQuotes
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quotes retrieved',
                data: [shippingQuote]
            })
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Shipping quote confirmed',
                data: []
            });

        render(<Cart />);
        selectShippingLocation();
        await clickShippingQuote(/Tiêu chuẩn/i);

        await waitFor(() => expect(
            screen.getByText('Khong the xac nhan phi giao hang da chon. Vui long chon phuong thuc khac.')
        ).toBeTruthy());
        expect(getCheckoutButton().disabled).toBe(true);
        expect(mockSubmitOrder).not.toHaveBeenCalled();
    });

    it('resets the selected shipping quote when the location changes', async () => {
        render(<Cart />);
        selectShippingLocation();
        await selectShippingQuote();

        expect(getCheckoutButton().disabled).toBe(false);

        selectLocationOption('Tỉnh/Thành phố', 'Tỉnh/Thành phố');

        await waitFor(() => expect(getCheckoutButton().disabled).toBe(true));
    });

    it('creates COD payment immediately and navigates to tracking', async () => {
        mockCreatePayment.mockResolvedValue({
            id: 21,
            orderId: 12,
            paymentCode: 'SOBU-PAY-COD',
            type: 'FULL',
            paymentMethod: 'COD',
            status: 'PENDING',
            amount: 350000,
            createdAt: '2026-06-21T10:00:00',
            updatedAt: '2026-06-21T10:00:00'
        });
        render(<Cart />);
        selectShippingLocation();
        fireEvent.change(screen.getByPlaceholderText(/Địa chỉ giao hàng chi tiết/), {
            target: { value: '123 Nguyen Trai' }
        });
        fireEvent.change(screen.getByLabelText('Phương thức thanh toán'), {
            target: { value: 'COD' }
        });
        await clickShippingQuote(/Hỏa tốc/i);
        await waitFor(() => expect(getCheckoutButton().disabled).toBe(false));
        expect(screen.getAllByText(/364\.000/)).not.toHaveLength(0);
        fireEvent.click(screen.getByRole('button', { name: /COD/i }));

        await waitFor(() => expect(mockSubmitOrder).toHaveBeenCalledWith(expect.objectContaining({
            carrierId: 29,
            carrierServiceId: 186,
            shippingFee: 14000,
            customerAddress: '123 Nguyen Trai'
        }), {
            clearCartOnSuccess: true
        }));
        await waitFor(() => expect(mockCreatePayment).toHaveBeenCalledWith(12, {
            type: 'FULL',
            paymentMethod: 'COD'
        }));
        expect(mockedShippingService.getQuotes).toHaveBeenLastCalledWith(expect.objectContaining({
            codAmount: 350000
        }));
        expect(mockedOnlineCartRecovery.save).not.toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith(
            '/orders/12?paymentSetup=cod',
            { replace: true }
        );
    });

    it('redirects ONLINE payment through the shared checkout helper', async () => {
        const onlinePayment = {
            id: 22,
            orderId: 12,
            paymentCode: 'SOBU-PAY-ONLINE',
            type: 'FULL' as const,
            paymentMethod: 'ONLINE' as const,
            status: 'PENDING' as const,
            amount: 350000,
            checkoutUrl: 'https://pay.payos.vn/web/checkout',
            createdAt: '2026-06-21T10:00:00',
            updatedAt: '2026-06-21T10:00:00'
        };
        mockCreatePayment.mockResolvedValue(onlinePayment);
        render(<Cart />);
        selectShippingLocation();
        await selectShippingQuote();
        fireEvent.click(screen.getByRole('button', { name: 'Đặt hàng và thanh toán' }));

        await waitFor(() => expect(mockSubmitOrder).toHaveBeenCalledWith(expect.any(Object), {
            clearCartOnSuccess: false
        }));
        await waitFor(() => expect(mockCreatePayment).toHaveBeenCalledWith(12, {
            type: 'FULL',
            paymentMethod: 'ONLINE'
        }));
        expect(mockedOnlineCartRecovery.save).toHaveBeenCalledWith(onlinePayment, [{
            product,
            quantity: 1
        }]);
        expect(mockedRedirectToPaymentCheckout).toHaveBeenCalledWith(onlinePayment);
    });

    it('does not recreate the order when payment initialization fails', async () => {
        mockCreatePayment.mockRejectedValue(new Error('PayOS unavailable'));
        render(<Cart />);
        selectShippingLocation();
        await selectShippingQuote();
        fireEvent.click(screen.getByRole('button', { name: 'Đặt hàng và thanh toán' }));

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith(
            '/orders/12?paymentSetup=failed',
            { replace: true }
        ));
        expect(mockSubmitOrder).toHaveBeenCalledTimes(1);
        expect(mockCreatePayment).toHaveBeenCalledTimes(1);
        expect(mockedOnlineCartRecovery.save).not.toHaveBeenCalled();
        expect(mockedRedirectToPaymentCheckout).not.toHaveBeenCalled();
    });
});
