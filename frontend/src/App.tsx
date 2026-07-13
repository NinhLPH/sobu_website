import {BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom';

import LoginPage from './pages/LoginPage';
import Cart from './pages/Cart';
import Header from "./components/common/Header";
import Footer from "./components/common/Footer";
import HomePage from "./pages/HomePage";
import ProductDetail from "./pages/ProductDetails";
import ProductList from "./pages/ProductList";
import AdminLayout from "./components/admin/AdminLayout";
import AdminProducts from "./pages/admin/Products";
import AdminCategories from "./pages/admin/Categories";
import AdminBrands from "./pages/admin/Brands";
import AdminOrders from "./pages/admin/Orders";
import AdminRequests from "./pages/admin/Requests";
import AdminRequestDetail from "./pages/admin/RequestDetails";
import AdminOrderDetail from "./pages/admin/OrderDetails";
import BlogList from "./pages/BlogList";
import BlogDetail from "./pages/BlogDetail";
import ServicesLandingPage from "./pages/ServicesLandingPage";
import Membership from "./pages/Membership";
import CreateRequest from "./pages/CreateRequest";
import MyRequests from "./pages/MyRequests";
import RequestDetail from "./pages/RequestDetail";
import AdminSync from "./pages/admin/Sync";
import AdminNhanhCallback from "./pages/admin/NhanhCallback";
import OrderTracking from "./pages/OrderTracking";
import MyOrders from "./pages/MyOrders";
import LegacyTrackingRedirect from "./pages/LegacyTrackingRedirect";
import PaymentResult from "./pages/PaymentResult";
import StaticPage from "./pages/StaticPage";
import UserProfile from "./pages/UserProfile";
import ProtectedRoute from "./components/common/ProtectedRoute";
import Toast from "./components/common/Toast";
import SiteLayout from './components/layout/SiteLayout';
import {useEffect} from 'react';
import {usePublicUiStore} from './store/usePublicUiStore';
import AdminBanners from './pages/admin/Banners';
import ScrollToTop from './components/common/ScrollToTop';
import AdminConfigs from './pages/admin/Configs';
import AdminStaticPages from './pages/admin/StaticPages';
import AdminReviews from './pages/admin/Reviews';
import AdminShipping from './pages/admin/Shipping';
import AdminSupport from './pages/admin/Support';

export default function App() {
    const fetchConfigs = usePublicUiStore((state) => state.fetchConfigs);

    useEffect(() => {
        void fetchConfigs();
    }, [fetchConfigs]);

    return (
        <Router>
            <ScrollToTop/>
            <div className="flex flex-col min-h-screen">
                <Header/>
                <Routes>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/activate" element={<Navigate to="/login" replace/>}/>
                    <Route path="/verify-email" element={<Navigate to="/login" replace/>}/>

                    <Route element={<SiteLayout/>}>
                        <Route path="/" element={<HomePage/>}/>
                        <Route path="/products" element={<ProductList/>}/>
                        <Route path="/product/:id" element={<ProductDetail/>}/>
                        <Route path="/category/:category" element={<ProductList/>}/>
                        <Route path="/blog" element={<BlogList/>}/>
                        <Route path="/blog/:id" element={<BlogDetail/>}/>
                        <Route path="/services" element={<ServicesLandingPage/>}/>
                        <Route path="/membership" element={<Membership/>}/>
                        <Route path="/about" element={<StaticPage slug="about"/>}/>
                        <Route path="/privacy" element={<StaticPage slug="privacy-policy"/>}/>
                        <Route path="/terms" element={<StaticPage slug="terms"/>}/>
                        <Route path="/policies/privacy" element={<StaticPage slug="privacy-policy"/>}/>
                        <Route path="/policies/terms" element={<StaticPage slug="terms"/>}/>
                        <Route path="/payment-result" element={<PaymentResult/>}/>
                        <Route path="/payment/return" element={<PaymentResult/>}/>
                        <Route path="/payment/cancel" element={<PaymentResult/>}/>

                        {/* USER LOGGED IN PRIVATE ROUTES */}
                        <Route element={<ProtectedRoute allowedRoles={['USER', 'ADMIN']} />}>
                            <Route path="/cart" element={<Cart/>}/>
                            <Route path="/profile" element={<UserProfile/>}/>
                            <Route path="/orders" element={<MyOrders/>}/>
                            <Route path="/orders/lookup" element={<OrderTracking/>}/>
                            <Route path="/orders/:orderId" element={<OrderTracking/>}/>
                            <Route path="/tracking" element={<LegacyTrackingRedirect/>}/>
                            <Route path="/requests" element={<MyRequests/>}/>
                            <Route path="/requests/new" element={<CreateRequest/>}/>
                            <Route path="/requests/:id" element={<RequestDetail/>}/>
                        </Route>
                    </Route>

                    {/* ADMIN WORKFLOWS */}
                    <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'STAFF']} />}>
                        <Route path="/admin" element={<AdminLayout/>}>
                            <Route index element={<Navigate to="/admin/products" replace/>}/>
                            <Route path="products" element={<AdminProducts/>}/>
                            <Route path="categories" element={<AdminCategories/>}/>
                            <Route path="brands" element={<AdminBrands/>}/>
                            <Route path="orders" element={<AdminOrders/>}/>
                            <Route path="orders/:id" element={<AdminOrderDetail/>}/>
                            <Route path="requests" element={<AdminRequests/>}/>
                            <Route path="requests/:id" element={<AdminRequestDetail/>}/>
                            <Route path="reviews" element={<AdminReviews/>}/>
                            <Route path="support" element={<AdminSupport/>}/>
                            <Route path="shipping" element={<AdminShipping/>}/>
                            <Route path="banners" element={<AdminBanners/>}/>
                            <Route path="configs" element={<AdminConfigs/>}/>
                            <Route path="static-pages" element={<AdminStaticPages/>}/>
                            <Route path="sync" element={<AdminSync/>}/>
                            <Route path="nhanh/callback" element={<AdminNhanhCallback/>}/>
                        </Route>
                    </Route>
                </Routes>
                <Footer/>
                <Toast/>
            </div>
        </Router>
    );
}
