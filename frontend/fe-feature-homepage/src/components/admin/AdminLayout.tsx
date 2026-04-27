import { Outlet, Link, useLocation } from 'react-router-dom';
import { Package, ListTree, ShoppingCart, MessageSquare, LayoutDashboard, Settings } from 'lucide-react';

export default function AdminLayout() {
    const location = useLocation();

    const menu = [
        { name: 'Sản phẩm', path: '/admin/products', icon: Package },
        { name: 'Danh mục', path: '/admin/categories', icon: ListTree },
        { name: 'Đơn hàng', path: '/admin/orders', icon: ShoppingCart },
        { name: 'Yêu cầu', path: '/admin/requests', icon: MessageSquare },
    ];

    return (
        <div className="flex min-h-[calc(100vh-80px)] mt-20 max-w-7xl mx-auto w-full border-t border-outline-variant/30">
            <aside className="w-64 flex-shrink-0 border-r border-outline-variant/30 bg-white">
                <div className="p-6 pt-16">
                    <h2 className="text-sm font-bold text-outline uppercase tracking-widest mb-6">Admin Panel</h2>
                    <nav className="space-y-2">
                        {menu.map((item) => {
                            const isActive = location.pathname.startsWith(item.path);
                            return (
                                <Link
                                    key={item.path}
                                    to={item.path}
                                    className={`flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-colors ${
                                        isActive
                                            ? 'bg-primary text-white'
                                            : 'text-on-surface hover:bg-surface-variant hover:text-primary'
                                    }`}
                                >
                                    <item.icon className="w-5 h-5" />
                                    {item.name}
                                </Link>
                            );
                        })}
                    </nav>
                </div>
            </aside>
            <main className="flex-1 p-8 bg-surface-container-lowest min-h-full">
                <Outlet />
            </main>
        </div>
    );
}
