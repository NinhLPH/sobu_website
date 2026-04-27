import {Link} from 'react-router-dom';
import {ShoppingCart, User, Search, Rocket, Shield} from 'lucide-react';
import {useCartStore} from "../../store/useCartStore";

export default function Header() {
    const items = useCartStore(state => state.items);
    const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

    return (
        <header
            className="fixed top-0 z-50 w-full bg-slate-50/80 dark:bg-slate-200/80 backdrop-blur-xl shadow-sm border-b border-outline-variant/30">
            <div className="flex flex-col w-full px-6 py-4 max-w-screen-2xl mx-auto">
                <div className="flex items-center justify-between gap-8 mb-4">
                    <Link to="/"
                          className="flex items-center gap-2 text-3xl font-black tracking-tighter text-primary-container">
                        <Rocket className="w-8 h-8 fill-primary-container"/>
                        SOBU
                    </Link>
                    <div className="flex-1 max-w-2xl relative hidden md:block">
                        <input
                            className="w-full bg-surface-container-low border border-outline-variant/30 rounded-md px-6 py-2.5 pr-12 text-sm focus:ring-2 focus:ring-primary/40 transition-all outline-none"
                            placeholder="Tìm kiếm mô hình, thương hiệu..."
                            type="text"
                        />
                        <Search className="absolute right-4 top-1/2 -translate-y-1/2 text-outline w-5 h-5"/>
                    </div>
                    <div className="flex items-center gap-4">
                        <Link to="/admin" className="p-2 hover:bg-white/50 dark:hover:bg-slate-800/50 rounded-full transition-all duration-300 group" title="Trang Quản Trị">
                            <Shield className="text-secondary w-6 h-6 group-hover:text-primary transition-colors" />
                        </Link>
                        <Link to="/cart"
                              className="p-2 hover:bg-white/50 dark:hover:bg-slate-800/50 rounded-full transition-all duration-300 relative group">
                            <ShoppingCart
                                className="text-primary-container w-6 h-6 group-hover:scale-110 transition-transform"/>
                            {itemCount > 0 && (
                                <span
                                    className="absolute top-0 right-0 w-4 h-4 bg-error text-red-600 text-[10px] flex items-center justify-center rounded-full font-bold">
                                    {itemCount}
                                </span>
                            )}
                        </Link>
                        <button
                            className="p-2 hover:bg-white/50 dark:hover:bg-slate-800/50 rounded-full transition-all duration-300">
                            <User className="text-primary-container w-6 h-6"/>
                        </button>
                    </div>
                </div>

                <nav
                    className="flex items-center gap-8 font-['Be_Vietnam_Pro'] tracking-tight text-sm font-medium overflow-x-auto pb-2 md:pb-0">
                    <Link to="/" className="text-primary font-bold border-b-2 border-primary pb-1 whitespace-nowrap">Trang chủ</Link>
                    <Link to="#" className="text-slate-600 hover:text-primary transition-all duration-300 whitespace-nowrap">Hàng mới</Link>
                    <Link to="#" className="text-slate-600 hover:text-primary transition-all duration-300 whitespace-nowrap">Hướng dẫn</Link>
                    <Link to="#" className="text-slate-600 hover:text-primary transition-all duration-300 whitespace-nowrap">Dịch vụ</Link>
                    <Link to="#" className="text-slate-600 hover:text-primary transition-all duration-300 whitespace-nowrap">Mô hình Custom</Link>
                </nav>
            </div>
        </header>
    );
}