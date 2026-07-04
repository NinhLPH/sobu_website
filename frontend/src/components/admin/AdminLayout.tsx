import { Outlet, Link, useLocation } from 'react-router-dom';
import { Package, ListTree, ShoppingCart, MessageSquare, Settings, Tag, Images, SlidersHorizontal, FileText, Star } from 'lucide-react';

export default function AdminLayout() {
    const location = useLocation();

    const menu = [
        { name: 'Sản phẩm', path: '/admin/products', icon: Package },
        { name: 'Danh mục', path: '/admin/categories', icon: ListTree },
        { name: 'Thương hiệu', path: '/admin/brands', icon: Tag },
        { name: 'Đơn hàng', path: '/admin/orders', icon: ShoppingCart },
        { name: 'Yêu cầu', path: '/admin/requests', icon: MessageSquare },
        { name: 'Đánh giá', path: '/admin/reviews', icon: Star },
        { name: 'Banner', path: '/admin/banners', icon: Images },
        { name: 'Cấu hình website', path: '/admin/configs', icon: SlidersHorizontal },
        { name: 'Trang tĩnh', path: '/admin/static-pages', icon: FileText },
        { name: 'Đồng bộ ERP', path: '/admin/sync', icon: Settings },
    ];

    return (
        <div className="admin-surface mx-auto mt-20 flex min-h-[calc(100vh-80px)] w-full max-w-7xl border-t border-outline-variant/30 bg-surface text-on-surface">
            <aside className="w-64 flex-shrink-0 border-r border-outline-variant/30 bg-surface-container-lowest">
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
                                            ? 'bg-primary text-on-primary'
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
            <main className="min-h-full flex-1 bg-surface-container-lowest p-8">
                <Outlet />
            </main>
        </div>
    );
}
