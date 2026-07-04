import {useState, useMemo, useEffect} from 'react';
import {Link, useSearchParams, useNavigate} from 'react-router-dom';
import {ChevronRight, ChevronDown, SlidersHorizontal, X} from 'lucide-react';

import ProductCard from "../components/common/ProductCard";
import {useProductStore} from '../store/useProductStore';
import {mapListItemToProductModel} from '../interface/product.model';

export default function ProductList() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const searchQuery = searchParams.get('search') || '';
    const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
    const [selectedBrands, setSelectedBrands] = useState<string[]>([]);
    const [selectedScales, setSelectedScales] = useState<string[]>([]);
    const [inStockOnly, setInStockOnly] = useState<boolean>(false);
    const [priceRange, setPriceRange] = useState<number>(10000000);
    const [expandedParents, setExpandedParents] = useState<number[]>([]);
    const [isFilterOpen, setIsFilterOpen] = useState(false);

    const { products, categories: apiCategories, brands: apiBrands, fetchProducts, fetchCategories, fetchBrands } = useProductStore();

    useEffect(() => {
        fetchProducts();
        fetchCategories();
        fetchBrands();
    }, [fetchProducts, fetchCategories, fetchBrands]);

    const mappedProducts = useMemo(() => {
        return products.map(mapListItemToProductModel);
    }, [products]);

    const categoryTree = useMemo(() => {
        if (!apiCategories || apiCategories.length === 0) return [];
        
        // Find parents
        const parents = apiCategories.filter(cat => {
            const pId = cat.parentId !== undefined ? cat.parentId : (cat as any).parentID;
            return pId === null || pId === undefined;
        });

        // Map parents with their children
        return parents.map(parent => {
            // Find children from flat list
            let children = apiCategories.filter(cat => {
                const pId = cat.parentId !== undefined ? cat.parentId : (cat as any).parentID;
                return pId !== null && pId !== undefined && String(pId) === String(parent.id);
            });

            // If no children in flat list, fallback to nested children list
            if (children.length === 0 && parent.children && parent.children.length > 0) {
                children = parent.children;
            }

            return {
                ...parent,
                children
            };
        });
    }, [apiCategories]);

    const brands = useMemo(() => {
        if (!apiBrands || apiBrands.length === 0) return [];
        return Array.from(new Set(apiBrands.map(b => b.name).filter(b => b && b !== 'N/A'))) as string[];
    }, [apiBrands]);

    const scales = useMemo(() => {
        return Array.from(new Set(mappedProducts.map(p => p.scale).filter(Boolean))) as string[];
    }, [mappedProducts]);

    const toggleParentExpand = (parentId: number) => {
        setExpandedParents(prev => 
            prev.includes(parentId) ? prev.filter(id => id !== parentId) : [...prev, parentId]
        );
    };

    // Parse URL query params and pre-select categories/brands
    useEffect(() => {
        const catId = searchParams.get('category');
        const brandId = searchParams.get('brand');
        
        if (catId && apiCategories.length > 0) {
            const cat = apiCategories.find(c => String(c.id) === String(catId));
            if (cat) {
                setSelectedCategories(prev => prev.includes(cat.name) ? prev : [...prev, cat.name]);
                
                // Automatically expand parent if the selected category is a child
                const pId = cat.parentId !== undefined ? cat.parentId : (cat as any).parentID;
                if (pId !== null && pId !== undefined) {
                    setExpandedParents(prev => prev.includes(Number(pId)) ? prev : [...prev, Number(pId)]);
                }
            }
        }
        
        if (brandId && apiBrands.length > 0) {
            const br = apiBrands.find(b => String(b.id) === String(brandId));
            if (br) {
                setSelectedBrands(prev => prev.includes(br.name) ? prev : [...prev, br.name]);
            }
        }
    }, [searchParams, apiCategories, apiBrands]);

    const toggleFilter = (item: string, list: string[], setList: React.Dispatch<React.SetStateAction<string[]>>) => {
        setList(prev => prev.includes(item) ? prev.filter(i => i !== item) : [...prev, item]);
    };

    const filteredProducts = useMemo(() => {
        // Build a set of all category names that match the selected categories (including their children if parents are selected)
        const activeCategoryNames = new Set<string>();
        selectedCategories.forEach(selectedName => {
            activeCategoryNames.add(selectedName);
            
            // Find if this is a parent in our tree, and if so, add all its children names
            const parentNode = categoryTree.find(p => p.name === selectedName);
            if (parentNode && parentNode.children) {
                parentNode.children.forEach(child => {
                    if (child.name) {
                        activeCategoryNames.add(child.name);
                    }
                });
            }
        });

        return mappedProducts.filter(p => {
            const catMatch = selectedCategories.length === 0 || (p.category && activeCategoryNames.has(p.category));
            const brandMatch = selectedBrands.length === 0 || selectedBrands.includes(p.brand);
            const scaleMatch = selectedScales.length === 0 || (p.scale && selectedScales.includes(p.scale));
            const stockMatch = !inStockOnly || p.stock > 0;
            const priceMatch = p.price <= priceRange;
            const searchMatch = !searchQuery || 
                p.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                (p.description && p.description.toLowerCase().includes(searchQuery.toLowerCase()));

            return catMatch && brandMatch && scaleMatch && stockMatch && priceMatch && searchMatch;
        });
    }, [mappedProducts, selectedCategories, selectedBrands, selectedScales, inStockOnly, priceRange, categoryTree, searchQuery]);

    const clearFilters = () => {
        setSelectedCategories([]);
        setSelectedBrands([]);
        setSelectedScales([]);
        setInStockOnly(false);
        setPriceRange(10000000);
        setExpandedParents([]);
        navigate('/products');
    };

    return (
        <main className="flex min-h-screen w-full min-w-0 flex-col bg-surface px-3 pb-20 pt-24 sm:px-6 sm:pb-24 sm:pt-32">
            {/* Header / Breadcrumb */}
            <nav className="mb-6 flex items-center gap-2 text-xs font-bold text-on-surface-variant sm:mb-8 sm:text-sm">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-4 h-4"/>
                <span className="text-primary">Cửa hàng</span>
            </nav>
            <header className="mb-7 sm:mb-12">
                <h1 className="mb-4 text-2xl font-black uppercase tracking-tight text-on-surface sm:text-4xl lg:text-5xl">Tất cả sản phẩm</h1>
            </header>

            <button
                type="button"
                onClick={() => setIsFilterOpen((open) => !open)}
                className="mb-4 flex w-full items-center justify-between rounded-xl border border-outline-variant/20 bg-surface-container-lowest px-4 py-3 text-xs font-black uppercase text-on-surface shadow-sm lg:hidden"
                aria-expanded={isFilterOpen}
                aria-controls="product-filters"
            >
                <span className="flex items-center gap-2"><SlidersHorizontal className="h-5 w-5 text-primary"/> Bộ lọc</span>
                <ChevronDown className={`h-5 w-5 transition-transform ${isFilterOpen ? 'rotate-180' : ''}`}/>
            </button>

            <div className="flex flex-col items-start gap-6 lg:flex-row lg:gap-10">
                {/* ---------------- SIDEBAR BỘ LỌC ---------------- */}
                <aside id="product-filters" className={`${isFilterOpen ? 'block' : 'hidden'} w-full flex-shrink-0 lg:sticky lg:top-28 lg:block lg:w-72`}>
                    <div
                        className="rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-5 shadow-[0_20px_40px_-15px_rgba(14,48,78,0.1)] sm:rounded-[2rem] sm:p-8">
                        <div className="relative mb-6 flex items-center justify-between pb-4 sm:mb-8">
                            <div className="flex items-center gap-3">
                                <SlidersHorizontal className="w-5 h-5 text-primary"/>
                                <h2 className="text-lg font-black uppercase tracking-widest text-on-surface">Bộ lọc</h2>
                            </div>
                            <div className="absolute bottom-0 left-0 w-full h-[2px] bg-surface-container"></div>
                        </div>

                        <div className="mb-6 sm:mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Danh mục</h3>
                            <div className="space-y-3">
                                {categoryTree.map(parent => {
                                    const isExpanded = expandedParents.includes(parent.id);
                                    const hasChildren = parent.children && parent.children.length > 0;
                                    const isParentSelected = selectedCategories.includes(parent.name);

                                    return (
                                        <div key={parent.id} className="space-y-2">
                                            {/* Parent Category Row */}
                                            <div className="flex items-center justify-between group">
                                                <div 
                                                    onClick={() => toggleFilter(parent.name, selectedCategories, setSelectedCategories)} 
                                                    className="flex items-center gap-3 cursor-pointer select-none flex-1"
                                                >
                                                    <div
                                                        className={`w-5 h-5 rounded-md flex items-center justify-center transition-colors ${isParentSelected ? 'bg-primary' : 'bg-surface-container group-hover:bg-outline-variant'}`}
                                                    >
                                                        {isParentSelected &&
                                                            <div className="w-2 h-2 rounded-sm bg-on-primary"></div>}
                                                    </div>
                                                    <span
                                                        className={`text-sm font-medium transition-colors ${isParentSelected ? 'text-primary font-bold' : 'text-on-surface group-hover:text-primary'}`}
                                                    >
                                                        {parent.name}
                                                    </span>
                                                </div>

                                                {hasChildren && (
                                                    <button 
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            toggleParentExpand(parent.id);
                                                        }}
                                                        className="p-1.5 hover:bg-surface-container rounded-lg text-outline/60 hover:text-primary transition-colors cursor-pointer border-none bg-transparent flex items-center justify-center"
                                                        title={isExpanded ? "Thu gọn" : "Mở rộng"}
                                                    >
                                                        {isExpanded ? (
                                                            <ChevronDown className="w-4 h-4" />
                                                        ) : (
                                                            <ChevronRight className="w-4 h-4" />
                                                        )}
                                                    </button>
                                                )}
                                            </div>

                                            {/* Child Categories (if expanded) */}
                                            {hasChildren && isExpanded && (
                                                <div className="pl-6 space-y-2.5 border-l-2 border-surface-container ml-2.5 py-1.5 animate-in fade-in slide-in-from-top-1 duration-200">
                                                    {parent.children.map(child => {
                                                        const isChildSelected = selectedCategories.includes(child.name);
                                                        return (
                                                            <div 
                                                                key={child.id}
                                                                onClick={() => toggleFilter(child.name, selectedCategories, setSelectedCategories)}
                                                                className="flex items-center gap-3 cursor-pointer group/child select-none"
                                                            >
                                                                <div
                                                                    className={`w-4 h-4 rounded-md flex items-center justify-center transition-colors ${isChildSelected ? 'bg-primary' : 'bg-surface-container group-hover:bg-outline-variant'}`}
                                                                >
                                                                    {isChildSelected &&
                                                                        <div className="h-1.5 w-1.5 rounded-sm bg-on-primary"></div>}
                                                                </div>
                                                                <span
                                                                    className={`text-xs font-medium transition-colors ${isChildSelected ? 'text-primary font-bold' : 'text-on-surface group-hover/child:text-primary'}`}
                                                                >
                                                                    {child.name}
                                                                </span>
                                                            </div>
                                                        );
                                                    })}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        <div className="mb-6 sm:mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Thương
                                hiệu</h3>
                            <div className="max-h-[160px] overflow-y-auto custom-scrollbar pr-2 space-y-3">
                                {brands.map(brand => (
                                    <label key={brand} onClick={() => toggleFilter(brand, selectedBrands, setSelectedBrands)} className="flex items-center gap-3 cursor-pointer group select-none">
                                        <div
                                            className={`w-5 h-5 rounded-md flex items-center justify-center transition-colors ${selectedBrands.includes(brand) ? 'bg-primary' : 'bg-surface-container group-hover:bg-outline-variant'}`}>
                                            {selectedBrands.includes(brand) &&
                                                <div className="w-2 h-2 rounded-sm bg-on-primary"></div>}
                                        </div>
                                        <span
                                            className={`text-sm font-medium transition-colors ${selectedBrands.includes(brand) ? 'text-primary font-bold' : 'text-on-surface group-hover:text-primary'}`}>{brand}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div className="mb-6 sm:mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Tỉ lệ mô
                                hình</h3>
                            <div className="flex flex-wrap gap-2">
                                {scales.map(scale => (
                                    <button
                                        key={scale}
                                        onClick={() => toggleFilter(scale, selectedScales, setSelectedScales)}
                                        className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${selectedScales.includes(scale) ? 'bg-primary text-on-primary shadow-md' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'}`}
                                    >
                                        {scale}
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div className="mb-6 sm:mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Tình
                                trạng</h3>
                            <label className="flex items-center gap-3 cursor-pointer group">
                                <div
                                    className={`w-10 h-6 rounded-full p-1 transition-colors ${inStockOnly ? 'bg-primary' : 'bg-surface-container'}`}>
                                    <div
                                        className={`h-4 w-4 rounded-full bg-on-primary transition-transform ${inStockOnly ? 'translate-x-4' : 'translate-x-0'}`}></div>
                                </div>
                                <span className="text-sm font-medium text-on-surface">Chỉ hiện hàng có sẵn</span>
                            </label>
                        </div>
                        <div>
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Khoảng
                                giá</h3>
                            <input type="range" min="500000" max="20000000" step="500000" value={priceRange}
                                   onChange={(e) => setPriceRange(Number(e.target.value))}
                                   className="w-full h-2 bg-surface-container rounded-full appearance-none cursor-pointer accent-primary mb-3"/>
                            <div
                                className="text-center font-black text-primary text-lg">{new Intl.NumberFormat('vi-VN').format(priceRange)}đ
                            </div>
                        </div>
                    </div>
                </aside>

                {/* ---------------- PRODUCT GRID ---------------- */}
                <div className="flex-1 w-full min-h-[600px] flex flex-col">
                    <div
                        className="mb-5 flex flex-col items-stretch justify-between gap-3 rounded-xl border border-outline-variant/20 bg-surface-container-lowest px-4 py-3 shadow-[0_10px_30px_-15px_rgba(14,48,78,0.12)] sm:mb-8 sm:flex-row sm:items-center sm:gap-4 sm:rounded-2xl sm:px-6 sm:py-4">
                        <div className="flex flex-wrap items-center gap-2">
                            <p className="text-sm text-on-surface-variant font-bold">
                                Hiển thị <span className="text-base font-black leading-none text-primary">{filteredProducts.length}</span> kết quả
                                {searchQuery && (
                                    <> cho từ khóa <span className="text-primary">"{searchQuery}"</span></>
                                )}
                            </p>
                            {(selectedCategories.length > 0 || selectedBrands.length > 0 || selectedScales.length > 0 || searchQuery) && (
                                <button onClick={clearFilters}
                                        className="flex cursor-pointer items-center gap-1 rounded-full bg-error/10 px-3 py-1 text-xs font-bold text-error hover:bg-error/20 sm:ml-4">
                                    <X className="w-3 h-3"/> Xóa lọc
                                </button>
                            )}
                        </div>
                        <div className="flex items-center justify-between gap-3 sm:justify-start sm:gap-4">
                            <span className="text-xs font-bold uppercase tracking-widest text-outline">Sắp xếp:</span>
                            <select
                                className="cursor-pointer rounded-full bg-surface-container px-4 py-2 text-xs font-bold text-on-surface outline-none transition-colors focus-visible:ring-2 focus-visible:ring-primary/30 sm:px-5 sm:text-sm">
                                <option>Hàng Mới Nhất</option>
                                <option>Giá tăng dần</option>
                                <option>Giá giảm dần</option>
                            </select>
                        </div>
                    </div>
                    {filteredProducts.length > 0 ? (
                        <div className="grid grid-cols-2 gap-3 sm:gap-6 min-[1536px]:grid-cols-3">
                            {filteredProducts.map(product => (
                                <ProductCard key={product.id} product={product}/>
                            ))}
                        </div>
                    ) : (
                        <div
                            className="my-auto flex w-full flex-1 flex-col items-center justify-center rounded-2xl border-2 border-dashed border-outline-variant/30 bg-surface-container-lowest p-8 text-center sm:rounded-[2rem] sm:p-16">
                            <h3 className="mb-4 text-xl font-black text-on-surface sm:text-2xl">Không tìm thấy sản phẩm</h3>
                            <p className="text-on-surface-variant font-medium">Không có mô hình nào khớp với bộ lọc của
                                bạn.</p>
                            <button onClick={clearFilters}
                                    className="mt-8 cursor-pointer rounded-full bg-primary px-7 py-2.5 text-sm font-bold text-on-primary shadow-md transition-transform hover:scale-105 sm:px-8 sm:py-3">Xóa
                                tất cả bộ lọc
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </main>
    );
}
