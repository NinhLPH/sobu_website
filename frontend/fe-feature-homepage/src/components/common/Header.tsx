import {useState, useEffect, useRef, useMemo} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {
    ShoppingCart, User, Search, Shield, ChevronDown, ChevronRight,
    Car, Box, Wrench, Diamond, Layers, Trash2, LogOut, FileText
} from 'lucide-react';
import {useCartStore} from "../../store/useCartStore";
import {useProductStore} from "../../store/useProductStore";
import {useAuthStore} from "../../store/useAuthStore";
import {formatCurrency} from "../../utils/format";


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

    const {items, removeFromCart, getTotals} = useCartStore();
    const {subtotal} = getTotals();
    const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);
    
    // Fetch categories and brands from useProductStore to avoid mockData
    const {
        categories,
        brands,
        categoriesLoaded,
        brandsLoaded,
        fetchCategories,
        fetchBrands
    } = useProductStore();
    
    const [activeParentId, setActiveParentId] = useState<number | null>(null);
    const [isMiniCartOpen, setIsMiniCartOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
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
    }, [categoriesLoaded, brandsLoaded, fetchCategories, fetchBrands]);

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

    useEffect(() => {
        if (mainCategories && mainCategories.length > 0 && activeParentId === null) {
            setActiveParentId(mainCategories[0].id);
        }
    }, [mainCategories, activeParentId]);

    return (
        <header className="fixed top-0 z-50 w-full bg-surface/90 backdrop-blur-xl shadow-[0_4px_30px_rgba(0,0,0,0.03)]">
            <div className="flex items-center justify-between px-6 py-4 max-w-screen-2xl mx-auto">
                {/* Logo */}
                <div className="flex items-center gap-10">
                    <Link to="/"
                          className="bg-primary-container text-white px-6 py-2 rounded-xl font-black text-xl tracking-widest hover:scale-105 transition-transform shadow-md">
                        SOBU
                    </Link>

                    {/* Navigation */}
                    <nav className="hidden lg:flex items-center gap-8 font-bold text-sm text-on-surface-variant">
                        <Link to="/" className="text-on-surface hover:text-primary transition-colors">Trang chủ</Link>
                        <div className="relative group py-2">
                            <Link to="/products"
                                  className="flex items-center gap-1 hover:text-primary transition-colors text-on-surface">
                                Danh mục <ChevronDown className="w-4 h-4 transition-transform group-hover:rotate-180"/>
                            </Link>
                            <div
                                className="absolute top-full -left-20 w-[950px] bg-surface-container-lowest rounded-[2rem] shadow-[0_30px_60px_-15px_rgba(14,48,78,0.15)] opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 z-50 p-8 cursor-default flex flex-col gap-6">
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
                                                        isActive ? 'bg-primary text-white shadow-md shadow-primary/20 scale-[1.02]' : 'hover:bg-surface-container text-on-surface'
                                                    }`}
                                                >
                                                    <div className="flex items-center gap-3">
                                                        <IconComponent className="w-5 h-5"/>
                                                        <span className="font-bold text-sm">{parent.name}</span>
                                                    </div>
                                                    {hasChildren && (
                                                        <ChevronRight
                                                            className={`w-4 h-4 ${isActive ? 'text-white' : 'text-outline/60'}`}/>
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
                                                            className="text-[10px] bg-surface-container text-outline group-hover/child:bg-primary group-hover/child:text-white px-2 py-0.5 rounded-md font-medium transition-colors">Xem</span>
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

                        <Link to="/requests" className="hover:text-primary transition-colors">Dịch vụ</Link>
                        <Link to="/membership" className="hover:text-primary transition-colors">Thẻ thành viên</Link>
                        <Link to="/blog" className="hover:text-primary transition-colors">Tin tức</Link>
                    </nav>
                </div>

                {/* Right Actions */}
                <div className="flex items-center gap-6">
                    <form onSubmit={(e) => {
                        e.preventDefault();
                        if (searchQuery.trim()) {
                            navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
                        } else {
                            navigate('/products');
                        }
                    }} className="relative hidden md:block w-64">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-outline w-4 h-4"/>
                        <input
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-surface-container rounded-full pl-10 pr-4 py-2.5 text-sm focus:ring-2 focus:ring-primary/40 transition-all outline-none border-none placeholder:text-outline/70 font-medium"
                            placeholder="Tìm kiếm mô hình..." type="text"/>
                    </form>

                    <div className="flex items-center gap-3">
                        <Link to="/admin"
                              className="w-10 h-10 rounded-full flex items-center justify-center text-on-surface hover:bg-surface-container transition-colors"
                              title="Admin">
                            <Shield className="w-5 h-5"/>
                        </Link>

                        <div className="relative" ref={cartRef}>
                            <button
                                onClick={() => setIsMiniCartOpen(!isMiniCartOpen)}
                                className={`w-10 h-10 rounded-full flex items-center justify-center transition-all relative group ${isMiniCartOpen ? 'bg-primary/10 text-primary' : 'text-on-surface hover:bg-surface-container'}`}
                                title="Giỏ hàng"
                            >
                                <ShoppingCart className="w-5 h-5 transition-transform group-hover:scale-105"/>
                                {itemCount > 0 && (
                                    <span
                                        className="absolute top-0 right-0 w-4 h-4 bg-error text-red-600 text-[9px] flex items-center justify-center rounded-full font-black shadow-sm">
                                        {itemCount}
                                    </span>
                                )}
                            </button>

                            {/* MINI CART DROPDOWN*/}
                            {isMiniCartOpen && (
                                <div
                                    className="absolute top-full right-0 mt-2 w-[340px] bg-surface-container-lowest rounded-2xl shadow-[0_15px_45px_-10px_rgba(14,48,78,0.12)] border border-surface-container/60 p-4 z-50 animate-in fade-in slide-in-from-top-3 duration-200">
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
                                                    className="w-full py-2.5 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl text-xs font-black uppercase tracking-widest text-center shadow-md shadow-primary/10 hover:scale-[1.01] transition-transform cursor-pointer"
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
                                onClick={() => {
                                    if (isAuthenticated) {
                                        setIsUserMenuOpen(!isUserMenuOpen);
                                    } else {
                                        navigate('/login');
                                    }
                                }}
                                className="flex items-center gap-2 pl-2 cursor-pointer transition-transform hover:scale-105 active:scale-95 outline-none"
                                title={isAuthenticated ? "Tài khoản của bạn" : "Đăng nhập / Đăng ký"}
                            >
                                <div
                                    className={`w-10 h-10 rounded-full flex items-center justify-center transition-all ${
                                        isAuthenticated
                                            ? 'bg-primary text-white shadow-md shadow-primary/20 font-black text-sm uppercase'
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
                            {isAuthenticated && isUserMenuOpen && (
                                <div
                                    className="absolute top-full right-0 mt-2 w-[260px] bg-surface-container-lowest rounded-2xl shadow-[0_15px_45px_-10px_rgba(14,48,78,0.12)] border border-surface-container/60 p-4 z-50 animate-in fade-in slide-in-from-top-3 duration-200"
                                >
                                    <div className="pb-3 border-b border-surface-container-high/60 mb-3">
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

                                    <div className="space-y-1">
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
                                            to="/requests"
                                            onClick={() => setIsUserMenuOpen(false)}
                                            className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-on-surface hover:bg-surface-container transition-colors"
                                        >
                                            <FileText className="w-4 h-4 text-primary"/>
                                            <span>Yêu cầu của tôi</span>
                                        </Link>
                                        <Link
                                            to="/tracking"
                                            onClick={() => setIsUserMenuOpen(false)}
                                            className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-on-surface hover:bg-surface-container transition-colors"
                                        >
                                            <Search className="w-4 h-4 text-primary"/>
                                            <span>Tra cứu đơn hàng</span>
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
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </header>
    );
}
