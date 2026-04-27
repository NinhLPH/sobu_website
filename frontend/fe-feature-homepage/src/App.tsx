import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

import Cart from './pages/Cart';
import Header from "./components/common/Header";
import Footer from "./components/common/Footer";
import HomePage from "./pages/HomePage";
import ProductDetail from "./pages/ProductDetails";
import ProductList from "./pages/ProductList";

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
                </Routes>
                <Footer />
            </div>
        </Router>
    );
}