import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import Cart from './Cart';
import { useCartStore } from '../store/useCartStore';
import { useAuthStore } from '../store/useAuthStore';
import { useLocationStore } from '../store/useLocationStore';
import { usePaymentStore } from '../store/usePaymentStore';
import { redirectToPaymentCheckout } from '../utils/payment-session';

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
jest.mock('../utils/payment-session', () => ({
    redirectToPaymentCheckout: require('@jest/globals').jest.fn()
}));

const mockedUseCartStore = jest.mocked(useCartStore);
const mockedUseAuthStore = jest.mocked(useAuthStore);
const mockedUseLocationStore = jest.mocked(useLocationStore);
const mockedUsePaymentStore = jest.mocked(usePaymentStore);
const mockedRedirectToPaymentCheckout = jest.mocked(redirectToPaymentCheckout);
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

describe('Cart payment selection', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockSubmitOrder.mockResolvedValue({ id: 12 });
        mockedUseCartStore.mockReturnValue({
            items: [{ product, quantity: 1 }],
            removeFromCart: jest.fn(),
            updateQuantity: jest.fn(),
            getTotals: () => ({ subtotal: 350000, totalDiscount: 0, total: 350000, itemCount: 1 }),
            submitOrder: mockSubmitOrder,
            isSubmitting: false,
            checkoutError: null,
            clearCheckoutError: jest.fn()
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
    });

    const selectShippingLocation = () => {
        const selects = screen.getAllByRole('combobox');
        fireEvent.change(selects[0], { target: { value: '1' } });
        fireEvent.change(selects[1], { target: { value: '2' } });
        fireEvent.change(selects[2], { target: { value: '3' } });
    };

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
        fireEvent.change(screen.getByLabelText('Phương thức thanh toán'), {
            target: { value: 'COD' }
        });
        fireEvent.click(screen.getByRole('button', { name: 'Đặt hàng với COD' }));

        await waitFor(() => expect(mockCreatePayment).toHaveBeenCalledWith(12, {
            type: 'FULL',
            paymentMethod: 'COD'
        }));
        expect(mockNavigate).toHaveBeenCalledWith(
            '/tracking?orderId=12&paymentSetup=cod',
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
        fireEvent.click(screen.getByRole('button', { name: 'Đặt hàng và thanh toán' }));

        await waitFor(() => expect(mockCreatePayment).toHaveBeenCalledWith(12, {
            type: 'FULL',
            paymentMethod: 'ONLINE'
        }));
        expect(mockedRedirectToPaymentCheckout).toHaveBeenCalledWith(onlinePayment);
    });

    it('does not recreate the order when payment initialization fails', async () => {
        mockCreatePayment.mockRejectedValue(new Error('PayOS unavailable'));
        render(<Cart />);
        selectShippingLocation();
        fireEvent.click(screen.getByRole('button', { name: 'Đặt hàng và thanh toán' }));

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith(
            '/tracking?orderId=12&paymentSetup=failed',
            { replace: true }
        ));
        expect(mockSubmitOrder).toHaveBeenCalledTimes(1);
        expect(mockCreatePayment).toHaveBeenCalledTimes(1);
    });
});
