import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import Cart from './pages/Cart';
import Header from "./components/common/Header";
import Footer from "./components/common/Footer";
import HomePage from "./pages/HomePage";
import ProductDetail from "./pages/ProductDetails";
import ProductList from "./pages/ProductList";
import AdminLayout from "./components/admin/AdminLayout";
import AdminProducts from "./pages/admin/Products";
import AdminCategories from "./pages/admin/Categories";
import AdminOrders from "./pages/admin/Orders";
import AdminRequests from "./pages/admin/Requests";
import AdminOrderDetail from "./pages/admin/OrderDetails";

export default function App() {
    return (
        <Router>
            <div className="flex flex-col min-h-screen">
                <Header />
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/products" element={<ProductList />} />
                    <Route path="/product/:id" element={<ProductDetail />} />
                    <Route path="/cart" element={<Cart />} />
                    <Route path="/category/:category" element={<ProductList />} />

                    <Route path="/admin" element={<AdminLayout />}>
                        <Route index element={<Navigate to="/admin/products" replace />} />
                        <Route path="products" element={<AdminProducts />} />
                        <Route path="categories" element={<AdminCategories />} />
                        <Route path="orders" element={<AdminOrders />} />
                        <Route path="orders/:id" element={<AdminOrderDetail />} />
                        <Route path="requests" element={<AdminRequests />} />
                    </Route>
                </Routes>
                <Footer />
            </div>
        </Router>
    );
}