import {useEffect, useRef, useState} from 'react';
import {Link, Outlet, useLocation} from 'react-router-dom';
import {
    Boxes,
    ClipboardList,
    FileText,
    Headphones,
    Images,
    ListTree,
    Menu,
    Newspaper,
    Package,
    Settings,
    ShoppingCart,
    SlidersHorizontal,
    Star,
    Tags,
    TicketPercent,
    Warehouse,
    X
} from 'lucide-react';
import {useAuthStore} from '../../store/useAuthStore';
import {useIntegrationStore} from '../../store/useIntegrationStore';

type AdminNavItem = { name: string; path: string; icon: typeof Package; adminOnly?: boolean; requiresNhanh?: boolean };
type AdminNavGroup = { label: string; items: AdminNavItem[] };

const groups: AdminNavGroup[] = [
    {
        label: 'Danh mục bán hàng', items: [
            {name: 'Sản phẩm', path: '/admin/products', icon: Package}, {
                name: 'Danh mục',
                path: '/admin/categories',
                icon: ListTree
            },
            {name: 'Thương hiệu', path: '/admin/brands', icon: Tags}, {
                name: 'Tag sản phẩm',
                path: '/admin/badges',
                icon: Boxes
            },
            {name: 'Voucher', path: '/admin/vouchers', icon: TicketPercent}, {
                name: 'Tồn kho',
                path: '/admin/inventory',
                icon: Warehouse
            },
        ]
    },
    {
        label: 'Vận hành', items: [
            {name: 'Đơn hàng', path: '/admin/orders', icon: ShoppingCart}, {
                name: 'Yêu cầu',
                path: '/admin/requests',
                icon: ClipboardList
            },
            {name: 'Chat hỗ trợ', path: '/admin/support', icon: Headphones}, {
                name: 'Đánh giá',
                path: '/admin/reviews',
                icon: Star
            },
            {name: 'Đồng bộ ERP', path: '/admin/sync', icon: Settings, requiresNhanh: true},
        ]
    },
    {
        label: 'Nội dung & hệ thống', items: [
            {name: 'Banner', path: '/admin/banners', icon: Images, adminOnly: true},
            {name: 'Cấu hình website', path: '/admin/configs', icon: SlidersHorizontal, adminOnly: true},
            {name: 'Trang tĩnh', path: '/admin/static-pages', icon: FileText, adminOnly: true},
            {name: 'Bài viết', path: '/admin/articles', icon: Newspaper, adminOnly: true},
        ]
    },
];

export default function AdminLayout() {
    const location = useLocation();
    const [open, setOpen] = useState(false);
    const role = useAuthStore(state => state.user?.role?.name);
    const nhanhEnabled = useIntegrationStore(state => state.nhanhEnabled);
    const ensureIntegrationLoaded = useIntegrationStore(state => state.ensureLoaded);
    const mainRef = useRef<HTMLElement>(null);
    const drawerRef = useRef<HTMLElement>(null);
    const menuButtonRef = useRef<HTMLButtonElement>(null);

    useEffect(() => {
        if (mainRef.current) mainRef.current.scrollTop = 0;
    }, [location.pathname]);

    useEffect(() => {
        void ensureIntegrationLoaded();
    }, [ensureIntegrationLoaded]);

    useEffect(() => {
        if (!open) return;
        const originalOverflow = document.body.style.overflow;
        const menuButton = menuButtonRef.current;
        document.body.style.overflow = 'hidden';
        const drawer = drawerRef.current;
        const firstFocusable = drawer?.querySelector<HTMLElement>('a, button:not([disabled]), [tabindex]:not([tabindex="-1"])');
        window.setTimeout(() => firstFocusable?.focus(), 0);
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                event.preventDefault();
                setOpen(false);
                return;
            }
            if (event.key !== 'Tab' || !drawer) return;
            const focusable = Array.from(drawer.querySelectorAll<HTMLElement>('a, button:not([disabled]), [tabindex]:not([tabindex="-1"])'));
            if (!focusable.length) return;
            const first = focusable[0];
            const last = focusable[focusable.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        };
        document.addEventListener('keydown', onKeyDown);
        return () => {
            document.removeEventListener('keydown', onKeyDown);
            document.body.style.overflow = originalOverflow;
            menuButton?.focus();
        };
    }, [open]);
    const nav = <nav className="space-y-6" aria-label="Điều hướng quản trị">{groups.map(group => {
        const items = group.items.filter(item =>
            (!item.adminOnly || role === 'ADMIN')
            && (!item.requiresNhanh || nhanhEnabled)
        );
        if (!items.length) return null;
        return <div key={group.label}><p
            className="mb-2 px-3 text-[10px] font-black uppercase tracking-[0.16em] text-outline">{group.label}</p>
            <div className="space-y-1">{items.map(item => {
                const active = location.pathname.startsWith(item.path);
                return <Link key={item.path} to={item.path} onClick={() => setOpen(false)}
                             aria-current={active ? 'page' : undefined}
                             className={`flex min-h-10 items-center gap-3 rounded-lg px-3 text-sm font-bold transition ${active ? 'bg-primary text-on-primary shadow-sm' : 'text-on-surface-variant hover:bg-surface-container hover:text-primary'}`}>
                    <item.icon className="h-[18px] w-[18px]"/>
                    {item.name}</Link>;
            })}</div>
        </div>;
    })}</nav>;

    return <div className="admin-surface mx-auto w-full max-w-[1504px] bg-surface-container-lowest text-on-surface">
        <div
            className="sticky z-30 flex items-center justify-between border-b border-outline-variant/30 bg-surface/95 px-4 py-3 backdrop-blur lg:hidden"
            style={{top: 'var(--app-header-height)'}}>
            <button ref={menuButtonRef} onClick={() => setOpen(true)} aria-expanded={open}
                    aria-controls="admin-mobile-menu"
                    className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-outline-variant/40 px-3 text-sm font-bold focus-visible:ring-2 focus-visible:ring-primary/40">
                <Menu className="h-5 w-5"/>Menu quản trị
            </button>
            <span className="text-xs font-bold uppercase tracking-widest text-primary">{role}</span></div>
        <div className="admin-workspace flex">
            <aside
                className="custom-scrollbar hidden w-64 shrink-0 overscroll-contain border-r border-outline-variant/30 bg-surface px-4 py-7 lg:block lg:overflow-y-auto">
                <div className="mb-7 px-3"><p className="text-lg font-black tracking-tight">SOBU Admin</p><p
                    className="mt-1 text-xs text-outline">Không gian vận hành</p></div>
                {nav}</aside>
            {open && <div className="fixed inset-0 z-[70] bg-black/50 lg:hidden"
                          onMouseDown={e => e.target === e.currentTarget && setOpen(false)}>
                <aside id="admin-mobile-menu" ref={drawerRef} role="dialog" aria-modal="true" aria-label="Menu quản trị"
                       className="custom-scrollbar h-full w-[86%] max-w-80 overflow-y-auto bg-surface p-4 shadow-2xl">
                    <div className="mb-5 flex items-center justify-between"><strong>SOBU Admin</strong>
                        <button className="rounded-lg p-2 hover:bg-surface-container" onClick={() => setOpen(false)}
                                aria-label="Đóng menu"><X className="h-5 w-5"/></button>
                    </div>
                    {nav}</aside>
            </div>}
            <main ref={mainRef}
                  className="custom-scrollbar min-w-0 flex-1 overscroll-contain p-4 sm:p-6 lg:overflow-y-auto lg:p-8">
                <Outlet/></main>
        </div>
    </div>;
}
