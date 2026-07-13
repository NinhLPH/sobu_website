import {useState, useEffect, useRef, useMemo, type FormEvent} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {
    ShoppingCart, User, Search, Shield, ChevronDown, ChevronRight,
    Car, Box, Wrench, Diamond, Layers, Trash2, LogOut, FileText, Sun, Moon, Home, Package, Newspaper
} from 'lucide-react';
import {useCartStore} from "../../store/useCartStore";
import {useProductStore} from "../../store/useProductStore";
import {useAuthStore} from "../../store/useAuthStore";
import {formatCurrency} from "../../utils/format";
import {usePublicUiStore} from '../../store/usePublicUiStore';
import {getPublicImageUrl} from '../../utils/file-url';
import SearchSuggestInput, {SearchSuggestion} from './SearchSuggestInput';

type ThemeMode = 'light' | 'dark';

const THEME_STORAGE_KEY = 'sobu-theme';

const getInitialTheme = (): ThemeMode => {
    if (typeof window === 'undefined') {
        return 'light';
    }

    const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
    if (storedTheme === 'light' || storedTheme === 'dark') {
        return storedTheme;
    }

    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

const applyTheme = (theme: ThemeMode) => {
    if (typeof document === 'undefined') {
        return;
    }

    const root = document.documentElement;
    root.classList.toggle('dark', theme === 'dark');
    root.classList.toggle('light', theme === 'light');
    root.dataset.theme = theme;
};


const getCategoryIcon = (catCode: string) => {
    switch (catCode?.toUpperCase()) {
        case 'VEHICLE':
        case 'CAT_VEHICLE':
            return Car;
        case 'MECHA':
        case 'CAT_MECHA':
            return Box;
        case 'FIGURE':
        case 'CAT_FIGURE':
            return User;
        case 'ACCESSORY':
        case 'CAT_ACCESSORY':
            return Wrench;
        case 'LEGO':
        case 'CAT_LEGO':
            return Layers;
        default:
            return Diamond;
    }
};

export default function Header() {
    const navigate = useNavigate();
    const location = useLocation();
    const configMap = usePublicUiStore((state) => state.configMap);
    const siteName = configMap?.site_name || 'SOBU';
    const logoUrl = configMap?.website_logo;

    const {items, removeFromCart, getTotals} = useCartStore();
    const {subtotal} = getTotals();
    const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

    // Fetch categories and brands from useProductStore to avoid mockData
    const {
        categories,
        brands,
        allProducts,
        allProductsLoaded,
        categoriesLoaded,
        brandsLoaded,
        fetchAllProducts,
        fetchCategories,
        fetchBrands
    } = useProductStore();

    const [activeParentId, setActiveParentId] = useState<number | null>(null);
    const [isMiniCartOpen, setIsMiniCartOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [theme, setTheme] = useState<ThemeMode>(getInitialTheme);
    const cartRef = useRef<HTMLDivElement>(null);

    // Auth States & Refs
    const {isAuthenticated, user, logoutAction} = useAuthStore();
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
    const userMenuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (cartRef.current && !cartRef.current.contains(event.target as Node)) {
                setIsMiniCartOpen(false);
            }
            if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
                setIsUserMenuOpen(false);
            }
        }

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    useEffect(() => {
        if (!categoriesLoaded) {
            fetchCategories();
        }
        if (!brandsLoaded) {
            fetchBrands();
        }
        if (!allProductsLoaded && typeof fetchAllProducts === 'function') {
            fetchAllProducts();
        }
    }, [allProductsLoaded, categoriesLoaded, brandsLoaded, fetchAllProducts, fetchCategories, fetchBrands]);

    const mainCategories = useMemo(() => {
        return categories?.filter(cat => {
            const pId = cat.parentId !== undefined ? cat.parentId : (cat as any).parentID;
            return pId === null || pId === undefined;
        }) || [];
    }, [categories]);

    const activeChildren = useMemo(() => {
        if (activeParentId === null) return [];

        // Find matching subcategories by parent ID (check both parentId and parentID)
        const subCats = categories.filter(cat => {
            const pId = cat.parentId !== undefined ? cat.parentId : (cat as any).parentID;
            return pId !== null && pId !== undefined && String(pId) === String(activeParentId);
        });

        if (subCats.length > 0) {
            return subCats;
        }

        // Fallback to .children array if it exists inside the parent object
        const parent = categories.find(cat => String(cat.id) === String(activeParentId));
        if (parent && parent.children && parent.children.length > 0) {
            return parent.children;
        }

        return [];
    }, [categories, activeParentId]);

    const productSearchSuggestions = useMemo<SearchSuggestion[]>(() => {
        return (allProducts || []).map((product) => ({
            id: product.id,
            label: product.name,
            description: [product.code, product.categoryName, product.brandName].filter(Boolean).join(' • '),
            searchValue: product.name,
        }));
    }, [allProducts]);

    useEffect(() => {
        if (mainCategories && mainCategories.length > 0 && activeParentId === null) {
            setActiveParentId(mainCategories[0].id);
        }
    }, [mainCategories, activeParentId]);

    useEffect(() => {
        applyTheme(theme);
        window.localStorage.setItem(THEME_STORAGE_KEY, theme);
    }, [theme]);

    const submitSearch = (query: string) => {
        if (query.trim()) {
            navigate(`/products?search=${encodeURIComponent(query.trim())}`);
        } else {
            navigate('/products');
        }
    };

    const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        submitSearch(searchQuery);
    };

    const toggleTheme = () => {
        setTheme((currentTheme) => currentTheme === 'dark' ? 'light' : 'dark');
    };

    const mobileNavItems = [
        {label: 'Trang chủ', path: '/', icon: Home},
        {label: 'Sản phẩm', path: '/products', icon: Package},
        {label: 'Dịch vụ', path: '/services', icon: Wrench},
        {label: 'Thành viên', path: '/membership', icon: Diamond},
        {label: 'Tin tức', path: '/blog', icon: Newspaper},
    ];
    const isMobileNavActive = (path: string) => {
        if (path === '/') {
            return location.pathname === '/';
        }

        if (path === '/products') {
            return location.pathname.startsWith('/products') || location.pathname.startsWith('/product/');
        }

        return location.pathname.startsWith(path);
    };

    return (
        <>
        <header className="fixed top-0 z-50 w-full bg-surface/90 backdrop-blur-xl shadow-[0_4px_30px_rgba(0,0,0,0.03)]">
            {/* HÀNG 1: CHUNG CHO CẢ MOBILE & DESKTOP */}
            <div className="mx-auto flex max-w-[1504px] items-center justify-between px-4 pb-3 pt-3 sm:px-6 lg:py-4">

                {/* TRÁI: Logo & Desktop Nav */}
                <div className="flex min-w-0 flex-1 items-center gap-4 xl:gap-6">
                    <Link
                        to="/"
                        className={`flex lg:h-22 h-16 items-center text-xl font-black tracking-widest transition-transform hover:scale-105 focus-visible:ring-2 focus-visible:ring-primary/40 ${
                            logoUrl
                                ? 'bg-transparent px-3 text-on-surface'
                                : 'bg-primary px-6 text-on-primary'
                        }`}
                    >
                        {logoUrl ? (
                            <img
                                src={getPublicImageUrl(logoUrl)}
                                alt={siteName}
                                className="max-h-12 max-w-[200px] lg:max-h-14 lg:max-w-[240px] object-contain"
                            />
                        ) : siteName}
                    </Link>

                    {/* Navigation (Chỉ hiện trên Desktop) */}
                    <nav aria-label="Điều hướng chính" className="hidden min-w-0 flex-1 items-center gap-4 text-sm font-bold text-on-surface-variant lg:flex xl:gap-5">
                        <div className="relative group py-2">
                            <Link to="/products"
                                  className="flex items-center gap-1 whitespace-nowrap text-on-surface transition-colors hover:text-primary">
                                Danh mục <ChevronDown className="w-4 h-4 transition-transform group-hover:rotate-180"/>
                            </Link>
                            <div
                                className="fixed left-4 right-4 top-[76px] z-50 flex cursor-default flex-col gap-6 rounded-[2rem] bg-surface-container-lowest p-8 opacity-0 invisible shadow-[0_30px_60px_-15px_rgba(14,48,78,0.15)] transition-all duration-300 group-hover:visible group-hover:opacity-100 xl:absolute xl:-left-20 xl:right-auto xl:top-full xl:w-[950px]">
                                <div className="grid grid-cols-12 gap-6 min-h-[300px]">
                                    <div
                                        className="col-span-4 border-r border-surface-container pr-4 flex flex-col gap-1.5">
                                        <span
                                            className="text-[11px] font-black uppercase tracking-wider text-outline mb-2 block px-2">Danh mục chính</span>
                                        {mainCategories?.map((parent) => {
                                            const IconComponent = getCategoryIcon(parent.code);
                                            const isActive = activeParentId === parent.id;
                                            const hasChildren = (parent.children && parent.children.length > 0) ||
                                                categories.some(c => {
                                                    const pId = c.parentId !== undefined ? c.parentId : (c as any).parentID;
                                                    return pId !== null && pId !== undefined && String(pId) === String(parent.id);
                                                });
                                            return (
                                                <div
                                                    key={parent.id}
                                                    onMouseEnter={() => setActiveParentId(parent.id)}
                                                    className={`flex items-center justify-between px-4 py-3 rounded-2xl transition-all duration-200 cursor-pointer ${
                                                        isActive ? 'bg-primary text-on-primary shadow-md shadow-primary/20 scale-[1.02]' : 'hover:bg-surface-container text-on-surface'
                                                    }`}
                                                >
                                                    <div className="flex items-center gap-3">
                                                        <IconComponent className="w-5 h-5"/>
                                                        <span className="font-bold text-sm">{parent.name}</span>
                                                    </div>
                                                    {hasChildren && (
                                                        <ChevronRight
                                                            className={`w-4 h-4 ${isActive ? 'text-on-primary' : 'text-outline/60'}`}/>
                                                    )}
                                                </div>
                                            );
                                        })}
                                    </div>

                                    <div className="col-span-8 pl-4 flex flex-col">
                                        <span
                                            className="text-[11px] font-black uppercase tracking-wider text-outline mb-4 block">Dòng sản phẩm chi tiết</span>
                                        {activeChildren && activeChildren.length > 0 ? (
                                            <div className="grid grid-cols-2 gap-3">
                                                {activeChildren.map((child) => (
                                                    <Link key={child.id} to={`/products?category=${child.id}`}
                                                          className="flex items-center justify-between px-4 py-3.5 rounded-xl bg-surface-container-low hover:bg-primary-container/20 border border-transparent hover:border-primary/20 transition-all group/child">
                                                        <span
                                                            className="text-sm font-bold text-on-surface group-hover/child:text-primary transition-colors">{child.name}</span>
                                                        <span
                                                            className="rounded-md bg-surface-container px-2 py-0.5 text-[10px] font-medium text-outline transition-colors group-hover/child:bg-primary group-hover/child:text-on-primary">Xem</span>
                                                    </Link>
                                                ))}
                                            </div>
                                        ) : (
                                            <div
                                                className="flex flex-col items-center justify-center flex-1 text-outline/60 border border-dashed border-surface-container rounded-2xl p-6">
                                                <Layers className="w-8 h-8 mb-2 stroke-[1.5]"/>
                                                <p className="text-xs font-medium">Danh mục này hiện tại không phân
                                                    nhánh nhỏ hơn</p>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <div className="border-t border-surface-container pt-4">
                                    <span
                                        className="text-[11px] font-black uppercase tracking-wider text-outline mb-3 block">Thương hiệu phân phối</span>
                                    <div className="grid grid-cols-6 gap-x-4 gap-y-2 font-semibold">
                                        {brands?.map((brand) => (
                                            <Link key={brand.id} to={`/products?brand=${brand.id}`}
                                                  className="text-xs text-on-surface-variant hover:text-primary transition-colors truncate">• {brand.name}</Link>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <form onSubmit={handleSearchSubmit} className="relative min-w-[260px] flex-1 lg:max-w-[360px] xl:max-w-[480px] 2xl:max-w-[560px]">
                            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/>
                            <SearchSuggestInput
                                value={searchQuery}
                                onChange={setSearchQuery}
                                onSubmit={submitSearch}
                                suggestions={productSearchSuggestions}
                                className="w-full rounded-full border border-outline-variant/20 bg-surface-container py-3 pl-10 pr-4 text-sm font-semibold text-on-surface outline-none transition-all placeholder:text-outline/70 focus:border-primary/30 focus:ring-2 focus:ring-primary/30"
                                placeholder="Tìm kiếm mô hình..."
                                ariaLabel="Tìm kiếm mô hình"
                            />
                        </form>

                        <Link to="/services" className="whitespace-nowrap transition-colors hover:text-primary">Dịch vụ</Link>
                        <Link to="/membership" className="whitespace-nowrap transition-colors hover:text-primary">Thẻ thành viên</Link>
                        <Link to="/blog" className="whitespace-nowrap transition-colors hover:text-primary">Tin tức</Link>
                    </nav>
                </div>

                {/* Right Actions */}
                <div className="flex shrink-0 items-center gap-1 sm:gap-3 lg:gap-3 xl:gap-4">
                    <div className="flex items-center gap-3">

                        <div className="relative" ref={cartRef}>
                            <button
                                onClick={() => setIsMiniCartOpen(!isMiniCartOpen)}
                                className={`w-10 h-10 rounded-full flex items-center justify-center transition-all relative group ${isMiniCartOpen ? 'bg-primary/10 text-primary' : 'text-on-surface hover:bg-surface-container'}`}
                                title="Giỏ hàng"
                            >
                                <ShoppingCart className="w-5 h-5 transition-transform group-hover:scale-105"/>
                                {itemCount > 0 && (
                                    <span
                                        className="absolute right-0 top-0 flex h-4 w-4 items-center justify-center rounded-full bg-error text-[9px] font-black leading-none text-on-error shadow-sm">
                                        {itemCount}
                                    </span>
                                )}
                            </button>

                            {/* MINI CART DROPDOWN*/}
                            {isMiniCartOpen && (
                                <div
                                    className="fixed left-3 right-3 top-[112px] z-50 rounded-2xl border border-surface-container/60 bg-surface-container-lowest p-4 shadow-[0_15px_45px_-10px_rgba(14,48,78,0.12)] animate-in fade-in slide-in-from-top-3 duration-200 sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-2 sm:w-[340px]">
                                    <div
                                        className="flex items-center justify-between pb-3 border-b border-surface-container-high/60 mb-3">
                                        <span className="text-xs font-black uppercase tracking-wider text-on-surface">Giỏ hàng sản phẩm ({itemCount})</span>
                                    </div>

                                    {items.length === 0 ? (
                                        // Empty State của Mini Cart
                                        <div className="py-8 flex flex-col items-center justify-center text-center">
                                            <ShoppingCart className="w-8 h-8 text-outline/40 mb-2 stroke-[1.5]"/>
                                            <p className="text-[11px] text-outline font-bold">Chưa có sản phẩm nào!</p>
                                        </div>
                                    ) : (
                                        // List Items State
                                        <>
                                            <div
                                                className="space-y-3.5 max-h-[240px] overflow-y-auto pr-1 scrollbar-hide">
                                                {items.map(({product, quantity}) => (
                                                    <div key={product.id}
                                                         className="flex gap-3 items-center group/item text-xs font-bold">
                                                        {/* Thumbnail ảnh sản phẩm */}
                                                        <div
                                                            className="w-12 h-12 rounded-lg bg-surface-container-low p-1.5 shrink-0 flex items-center justify-center">
                                                            <img src={product.imageUrl} alt={product.name}
                                                                 className="w-full h-full object-contain"/>
                                                        </div>
                                                        {/* Tên và Số lượng */}
                                                        <div className="flex-1 min-w-0">
                                                            <h4 className="text-on-surface font-bold truncate pr-2 leading-tight">{product.name}</h4>
                                                            <p className="text-[10px] text-outline mt-0.5">Số
                                                                lượng: {quantity}</p>
                                                        </div>
                                                        {/* Giá thành & Nút xóa */}
                                                        <div
                                                            className="text-right shrink-0 flex flex-col items-end gap-0.5">
                                                            <span
                                                                className="text-primary font-black">{formatCurrency(product.price * quantity)}</span>
                                                            <button
                                                                onClick={() => removeFromCart(product.id)}
                                                                className="text-outline hover:text-error transition-colors p-0.5"
                                                                title="Xóa khỏi giỏ"
                                                            >
                                                                <Trash2 className="w-3.5 h-3.5"/>
                                                            </button>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>

                                            {/* Tổng tiền tạm tính & Nút điều hướng */}
                                            <div className="mt-4 pt-3 border-t border-surface-container-high/60">
                                                <div
                                                    className="flex justify-between items-center mb-3.5 text-xs font-bold">
                                                    <span className="text-outline">Tổng cộng:</span>
                                                    <span
                                                        className="text-base font-black text-on-surface">{formatCurrency(subtotal)}</span>
                                                </div>

                                                <button
                                                    onClick={() => {
                                                        setIsMiniCartOpen(false);
                                                        if (!isAuthenticated) {
                                                            navigate('/login');
                                                        } else {
                                                            navigate('/cart');
                                                        }
                                                    }}
                                                    className="w-full cursor-pointer rounded-xl bg-gradient-to-br from-primary to-primary-container py-2.5 text-center text-xs font-black uppercase tracking-widest text-on-primary shadow-md shadow-primary/10 transition-transform hover:scale-[1.01] focus-visible:ring-2 focus-visible:ring-primary/40"
                                                >
                                                    Đi đến thanh toán
                                                </button>
                                            </div>
                                        </>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* USER PROFILE / AUTH BUTTON */}
                        <div className="relative" ref={userMenuRef}>
                            <button
                                onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                                className="flex items-center gap-2 pl-1 cursor-pointer transition-transform hover:scale-105 active:scale-95 outline-none lg:pl-2"
                                title={isAuthenticated ? "Tài khoản của bạn" : "Đăng nhập / Đăng ký"}
                            >
                                <div
                                    className={`w-10 h-10 rounded-full flex items-center justify-center transition-all ${
                                        isAuthenticated
                                            ? 'bg-primary text-on-primary shadow-md shadow-primary/20 font-black text-sm uppercase'
                                            : 'bg-surface-container-high text-primary hover:bg-surface-container-highest hover:scale-105'
                                    }`}
                                >
                                    {isAuthenticated && user?.fullName ? (
                                        <span>
                                            {user.fullName.charAt(0)}
                                        </span>
                                    ) : (
                                        <User className="w-5 h-5"/>
                                    )}
                                </div>
                            </button>

                            {/* USER PROFILE DROPDOWN */}
                            {isUserMenuOpen && (
                                <div
                                    className="fixed left-3 right-3 top-[112px] z-50 rounded-2xl border border-surface-container/60 bg-surface-container-lowest p-4 shadow-[0_15px_45px_-10px_rgba(14,48,78,0.12)] animate-in fade-in slide-in-from-top-3 duration-200 sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-2 sm:w-[260px]"
                                >
                                    {isAuthenticated ? (
                                        <div className="pb-3 border-b border-surface-container-high/60 mb-3 flex items-start justify-between gap-3">
                                            <div className="min-w-0 flex-1">
                                                <p className="text-xs font-black text-on-surface truncate">
                                                    {user?.fullName}
                                                </p>
                                                <p className="text-[10px] text-outline truncate mt-0.5">
                                                    {user?.email}
                                                </p>
                                                {user?.role && (
                                                    <span
                                                        className="inline-block mt-2 text-[9px] bg-primary/10 text-primary px-2.5 py-0.5 rounded-full font-black uppercase tracking-wider">
                                                        {user.role.name === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}
                                                    </span>
                                                )}
                                            </div>
                                            {/* Nút Darkmode icon-only (Chỉ hiện trên mobile) */}
                                            <button
                                                type="button"
                                                onClick={toggleTheme}
                                                title={theme === 'dark' ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}
                                                className="flex shrink-0 h-8 w-8 items-center justify-center rounded-full bg-surface-container-low hover:bg-surface-container-high text-primary transition-colors focus-visible:ring-2 focus-visible:ring-primary/40"
                                            >
                                                {theme === 'dark' ? <Moon className="h-4 w-4"/> : <Sun className="h-4 w-4"/>}
                                            </button>
                                        </div>
                                    ) : (
                                        <div className="pb-3 border-b border-surface-container-high/60 mb-3 flex items-start justify-between gap-3">
                                            <div className="min-w-0 flex-1">
                                                <p className="text-xs font-black text-on-surface">Tài khoản</p>
                                                <p className="mt-0.5 text-[10px] font-medium text-outline">Đăng nhập để theo dõi đơn hàng và yêu cầu của bạn</p>
                                            </div>
                                            {/* Nút Darkmode icon-only (Chỉ hiện trên mobile) */}
                                            <button
                                                type="button"
                                                onClick={toggleTheme}
                                                title={theme === 'dark' ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}
                                                className="flex shrink-0 h-8 w-8 items-center justify-center rounded-full bg-surface-container-low hover:bg-surface-container-high text-primary transition-colors focus-visible:ring-2 focus-visible:ring-primary/40"
                                            >
                                                {theme === 'dark' ? <Moon className="h-4 w-4"/> : <Sun className="h-4 w-4"/>}
                                            </button>
                                        </div>
                                    )}

                                    <div className="space-y-1">
                                        {/* Đã tháo renderThemeMenuButton() ở đây */}
                                        {isAuthenticated ? (
                                            <>
                                                {user?.role?.name === 'ADMIN' && (
                                                    <Link
                                                        to="/admin"
                                                        onClick={() => setIsUserMenuOpen(false)}
                                                        className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-on-surface hover:bg-surface-container transition-colors"
                                                    >
                                                        <Shield className="w-4 h-4 text-primary"/>
                                                        <span>Trang quản trị (Admin)</span>
                                                    </Link>
                                                )}
                                                <Link
                                                    to="/profile"
                                                    onClick={() => setIsUserMenuOpen(false)}
                                                    className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-on-surface hover:bg-surface-container transition-colors"
                                                >
                                                    <User className="w-4 h-4 text-primary"/>
                                                    <span>Hồ sơ cá nhân</span>
                                                </Link>
                                                <Link
                                                    to="/requests"
                                                    onClick={() => setIsUserMenuOpen(false)}
                                                    className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-on-surface hover:bg-surface-container transition-colors"
                                                >
                                                    <FileText className="w-4 h-4 text-primary"/>
                                                    <span>Yêu cầu của tôi</span>
                                                </Link>
                                                <Link
                                                    to="/orders"
                                                    onClick={() => setIsUserMenuOpen(false)}
                                                    className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-on-surface hover:bg-surface-container transition-colors"
                                                >
                                                    <Search className="w-4 h-4 text-primary"/>
                                                    <span>Đơn hàng của tôi</span>
                                                </Link>
                                                <button
                                                    onClick={async () => {
                                                        setIsUserMenuOpen(false);
                                                        await logoutAction();
                                                    }}
                                                    className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-error hover:bg-error/10 transition-colors text-left cursor-pointer outline-none"
                                                >
                                                    <LogOut className="w-4 h-4"/>
                                                    <span>Đăng xuất</span>
                                                </button>
                                            </>
                                        ) : (
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    setIsUserMenuOpen(false);
                                                    navigate('/login');
                                                }}
                                                className="flex w-full cursor-pointer items-center gap-2.5 rounded-xl bg-primary px-3 py-2.5 text-left text-xs font-black text-on-primary transition-colors hover:bg-primary-container focus-visible:ring-2 focus-visible:ring-primary/40"
                                            >
                                                <User className="h-4 w-4"/>
                                                <span>Đăng nhập / Đăng ký</span>
                                            </button>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <div className="mx-auto w-full max-w-[1504px] px-4 pb-3 sm:px-6 lg:hidden">
                <form onSubmit={handleSearchSubmit} className="relative">
                    <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/>
                    <SearchSuggestInput
                        value={searchQuery}
                        onChange={setSearchQuery}
                        onSubmit={submitSearch}
                        suggestions={productSearchSuggestions}
                        className="h-11 w-full rounded-full border border-outline-variant/20 bg-surface-container py-2.5 pl-10 pr-4 text-sm font-semibold text-on-surface outline-none transition-all placeholder:text-outline/70 focus:border-primary/30 focus:ring-2 focus:ring-primary/30"
                        placeholder="Tìm kiếm mô hình..."
                        ariaLabel="Tìm kiếm mô hình"
                    />
                </form>
            </div>

        </header>
        <nav
            aria-label="Thanh điều hướng mobile"
            className="fixed bottom-0 left-0 right-0 z-50 grid grid-cols-5 border-t border-outline-variant/20 bg-surface-container-lowest/95 px-2 pb-[calc(env(safe-area-inset-bottom)+0.35rem)] pt-1.5 shadow-[0_-10px_30px_rgba(14,48,78,0.08)] backdrop-blur-xl lg:hidden"
        >
            {mobileNavItems.map((item) => {
                const Icon = item.icon;
                const isActive = isMobileNavActive(item.path);

                return (
                    <Link
                        key={item.path}
                        to={item.path}
                        aria-current={isActive ? 'page' : undefined}
                        className={`flex min-h-[52px] flex-col items-center justify-center gap-1 rounded-xl px-1 text-[10px] font-black leading-tight transition-colors focus-visible:ring-2 focus-visible:ring-primary/40 ${
                            isActive ? 'bg-primary/10 text-primary' : 'text-outline hover:bg-surface-container hover:text-on-surface'
                        }`}
                    >
                        <Icon className="h-5 w-5"/>
                        <span className="truncate">{item.label}</span>
                    </Link>
                );
            })}
        </nav>
        </>
    );
}
