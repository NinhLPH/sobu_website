import {BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom';

import LoginPage from './pages/LoginPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
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
import AdminOrderDetail from "./pages/admin/OrderDetails";
import BlogList from "./pages/BlogList";
import BlogDetail from "./pages/BlogDetail";
import ServicesPage from "./pages/ServicesPage";
import Membership from "./pages/Membership";
import CreateRequest from "./pages/CreateRequest";
import MyRequests from "./pages/MyRequests";
import RequestDetail from "./pages/RequestDetail";
import AdminSync from "./pages/admin/Sync";
import AdminNhanhCallback from "./pages/admin/NhanhCallback";
import OrderTracking from "./pages/OrderTracking";
import ProtectedRoute from "./components/common/ProtectedRoute";

export default function App() {
    return (
        <Router>
            <div className="flex flex-col min-h-screen">
                <Header/>
                <Routes>
                    <Route path="/" element={<HomePage/>}/>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/verify-email" element={<VerifyEmailPage/>}/>
                    <Route path="/products" element={<ProductList/>}/>
                    <Route path="/product/:id" element={<ProductDetail/>}/>
                    <Route path="/category/:category" element={<ProductList/>}/>
                    <Route path="/blog" element={<BlogList/>}/>
                    <Route path="/blog/:id" element={<BlogDetail/>}/>
                    <Route path="/services" element={<ServicesPage/>}/>
                    <Route path="/membership" element={<Membership/>}/>

                    {/* CUSTOMER ORDER TRACKING */}
                    <Route path="/tracking" element={<OrderTracking/>}/>

                    {/* CUSTOMER LOGGED IN PRIVATE ROUTES */}
                    <Route element={<ProtectedRoute allowedRoles={['USER', 'ADMIN']} />}>
                        <Route path="/cart" element={<Cart/>}/>
                        <Route path="/requests" element={<MyRequests/>}/>
                        <Route path="/requests/new" element={<CreateRequest/>}/>
                        <Route path="/requests/:id" element={<RequestDetail/>}/>
                    </Route>

                    {/* ADMIN WORKFLOWS */}
                    <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                        <Route path="/admin" element={<AdminLayout/>}>
                            <Route index element={<Navigate to="/admin/products" replace/>}/>
                            <Route path="products" element={<AdminProducts/>}/>
                            <Route path="categories" element={<AdminCategories/>}/>
                            <Route path="brands" element={<AdminBrands/>}/>
                            <Route path="orders" element={<AdminOrders/>}/>
                            <Route path="orders/:id" element={<AdminOrderDetail/>}/>
                            <Route path="requests" element={<AdminRequests/>}/>
                            <Route path="sync" element={<AdminSync/>}/>
                            <Route path="nhanh/callback" element={<AdminNhanhCallback/>}/>
                        </Route>
                    </Route>
                </Routes>
                <Footer/>
            </div>
        </Router>
    );
}
