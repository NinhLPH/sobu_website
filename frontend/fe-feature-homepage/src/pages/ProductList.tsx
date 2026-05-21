import {useState, useMemo} from 'react';
import {Link} from 'react-router-dom';
import {ChevronRight, SlidersHorizontal, X} from 'lucide-react';

import {mockProducts} from '../data/mockData';
import ProductCard from "../components/common/ProductCard";

export default function ProductList() {
    const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
    const [selectedBrands, setSelectedBrands] = useState<string[]>([]);
    const [selectedScales, setSelectedScales] = useState<string[]>([]);
    const [inStockOnly, setInStockOnly] = useState<boolean>(false);
    const [priceRange, setPriceRange] = useState<number>(10000000);

    const categories = Array.from(new Set(mockProducts.map(p => p.category).filter(Boolean))) as string[];
    const brands = Array.from(new Set(mockProducts.map(p => p.brand).filter(b => b && b !== 'N/A'))) as string[];
    const scales = Array.from(new Set(mockProducts.map(p => p.scale).filter(Boolean))) as string[];

    const toggleFilter = (item: string, list: string[], setList: React.Dispatch<React.SetStateAction<string[]>>) => {
        setList(prev => prev.includes(item) ? prev.filter(i => i !== item) : [...prev, item]);
    };

    const filteredProducts = useMemo(() => {
        return mockProducts.filter(p => {
            const catMatch = selectedCategories.length === 0 || (p.category && selectedCategories.includes(p.category));
            const brandMatch = selectedBrands.length === 0 || selectedBrands.includes(p.brand);
            const scaleMatch = selectedScales.length === 0 || (p.scale && selectedScales.includes(p.scale));
            const stockMatch = !inStockOnly || p.stock > 0;
            const priceMatch = p.price <= priceRange;

            return catMatch && brandMatch && scaleMatch && stockMatch && priceMatch;
        });
    }, [selectedCategories, selectedBrands, selectedScales, inStockOnly, priceRange]);

    const clearFilters = () => {
        setSelectedCategories([]);
        setSelectedBrands([]);
        setSelectedScales([]);
        setInStockOnly(false);
        setPriceRange(10000000);
    };

    return (
        <main className="max-w-screen-2xl mx-auto px-6 pt-32 pb-24 bg-surface">
            {/* Header / Breadcrumb */}
            <nav className="flex items-center gap-2 text-sm font-bold text-on-surface-variant mb-8">
                <Link to="/" className="hover:text-primary transition-colors">Trang chủ</Link>
                <ChevronRight className="w-4 h-4"/>
                <span className="text-primary">Cửa hàng</span>
            </nav>
            <header className="mb-12">
                <h1 className="text-5xl font-black tracking-tighter mb-4 text-on-surface uppercase">Tất cả sản phẩm</h1>
            </header>

            <div className="flex flex-col lg:flex-row gap-10 items-start">
                {/* ---------------- SIDEBAR BỘ LỌC ---------------- */}
                <aside className="w-full lg:w-72 flex-shrink-0 lg:sticky lg:top-28">
                    <div
                        className="bg-surface-container-lowest rounded-[2rem] p-8 shadow-[0_20px_40px_-15px_rgba(14,48,78,0.05)]">
                        <div className="flex items-center justify-between mb-8 pb-4 relative">
                            <div className="flex items-center gap-3">
                                <SlidersHorizontal className="w-5 h-5 text-primary"/>
                                <h2 className="text-lg font-black uppercase tracking-widest text-on-surface">Bộ lọc</h2>
                            </div>
                            <div className="absolute bottom-0 left-0 w-full h-[2px] bg-surface-container"></div>
                        </div>

                        <div className="mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Danh mục</h3>
                            <div className="space-y-3">
                                {categories.map(cat => (
                                    <label key={cat} className="flex items-center gap-3 cursor-pointer group">
                                        <div
                                            className={`w-5 h-5 rounded-md flex items-center justify-center transition-colors ${selectedCategories.includes(cat) ? 'bg-primary' : 'bg-surface-container group-hover:bg-outline-variant'}`}>
                                            {selectedCategories.includes(cat) &&
                                                <div className="w-2 h-2 bg-white rounded-sm"></div>}
                                        </div>
                                        <span
                                            className={`text-sm font-medium transition-colors ${selectedCategories.includes(cat) ? 'text-primary font-bold' : 'text-on-surface group-hover:text-primary'}`}>{cat}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div className="mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Thương
                                hiệu</h3>
                            <div className="max-h-[160px] overflow-y-auto custom-scrollbar pr-2 space-y-3">
                                {brands.map(brand => (
                                    <label key={brand} className="flex items-center gap-3 cursor-pointer group">
                                        <div
                                            className={`w-5 h-5 rounded-md flex items-center justify-center transition-colors ${selectedBrands.includes(brand) ? 'bg-primary' : 'bg-surface-container group-hover:bg-outline-variant'}`}>
                                            {selectedBrands.includes(brand) &&
                                                <div className="w-2 h-2 bg-white rounded-sm"></div>}
                                        </div>
                                        <span
                                            className={`text-sm font-medium transition-colors ${selectedBrands.includes(brand) ? 'text-primary font-bold' : 'text-on-surface group-hover:text-primary'}`}>{brand}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div className="mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Tỉ lệ mô
                                hình</h3>
                            <div className="flex flex-wrap gap-2">
                                {scales.map(scale => (
                                    <button
                                        key={scale}
                                        onClick={() => toggleFilter(scale, selectedScales, setSelectedScales)}
                                        className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${selectedScales.includes(scale) ? 'bg-primary text-white shadow-md' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'}`}
                                    >
                                        {scale}
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div className="mb-8">
                            <h3 className="text-xs font-black uppercase tracking-widest text-outline mb-4">Tình
                                trạng</h3>
                            <label className="flex items-center gap-3 cursor-pointer group">
                                <div
                                    className={`w-10 h-6 rounded-full p-1 transition-colors ${inStockOnly ? 'bg-primary' : 'bg-surface-container'}`}>
                                    <div
                                        className={`w-4 h-4 bg-white rounded-full transition-transform ${inStockOnly ? 'translate-x-4' : 'translate-x-0'}`}></div>
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
                <div className="flex-1 w-full">
                    <div
                        className="flex flex-col sm:flex-row justify-between items-center gap-4 mb-8 bg-surface-container-lowest px-6 py-4 rounded-2xl shadow-[0_10px_30px_-15px_rgba(14,48,78,0.05)]">
                        <div className="flex items-center gap-2">
                            <p className="text-sm text-on-surface-variant font-bold">Hiển thị <span
                                className="text-primary text-lg">{filteredProducts.length}</span> kết quả</p>
                            {(selectedCategories.length > 0 || selectedBrands.length > 0 || selectedScales.length > 0) && (
                                <button onClick={clearFilters}
                                        className="ml-4 flex items-center gap-1 text-xs font-bold text-error bg-error/10 px-3 py-1 rounded-full hover:bg-error/20">
                                    <X className="w-3 h-3"/> Xóa lọc
                                </button>
                            )}
                        </div>
                        <div className="flex items-center gap-4">
                            <span className="text-xs font-bold uppercase tracking-widest text-outline">Sắp xếp:</span>
                            <select
                                className="bg-surface-container rounded-full px-5 py-2 text-sm font-bold text-on-surface outline-none cursor-pointer">
                                <option>Hàng Mới Nhất</option>
                                <option>Giá tăng dần</option>
                                <option>Giá giảm dần</option>
                            </select>
                        </div>
                    </div>
                    {filteredProducts.length > 0 ? (
                        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-8">
                            {filteredProducts.map(product => (
                                <ProductCard key={product.id} product={product}/>
                            ))}
                        </div>
                    ) : (
                        <div
                            className="bg-surface-container-lowest rounded-[2rem] p-16 text-center border-2 border-dashed border-outline-variant/30">
                            <h3 className="text-2xl font-black mb-4 text-on-surface">Không tìm thấy sản phẩm</h3>
                            <p className="text-on-surface-variant font-medium">Không có mô hình nào khớp với bộ lọc của
                                bạn.</p>
                            <button onClick={clearFilters}
                                    className="mt-8 px-8 py-3 bg-primary text-white rounded-full font-bold shadow-md hover:scale-105 transition-transform">Xóa
                                tất cả bộ lọc
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </main>
    );
}