import { Navigate, useLocation } from 'react-router-dom';

export default function LegacyTrackingRedirect() {
    const { search } = useLocation();
    const params = new URLSearchParams(search);
    const orderId = params.get('orderId');
    const nhanhOrderId = params.get('nhanhOrderId');
    const paymentSetup = params.get('paymentSetup');
    const paymentQuery = paymentSetup ? `?paymentSetup=${encodeURIComponent(paymentSetup)}` : '';

    if (orderId) {
        return <Navigate to={`/orders/${encodeURIComponent(orderId)}${paymentQuery}`} replace />;
    }
    if (nhanhOrderId) {
        const query = new URLSearchParams({ nhanhOrderId });
        if (paymentSetup) {
            query.set('paymentSetup', paymentSetup);
        }
        return <Navigate to={`/orders/lookup?${query.toString()}`} replace />;
    }
    return <Navigate to="/orders" replace />;
}
