import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

import { mockProducts } from '../data/mockData';

import ProductCard from "../components/common/ProductCart";

export default function ProductList() {
    const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
    const [priceRange, setPriceRange] = useState<number>(5000000);

    const categories = Array.from(new Set(mockProducts.map(p => p.category).filter(Boolean))) as string[];

    const handleCategoryToggle = (category: string) => {
        setSelectedCategories(prev =>
            prev.includes(category)
                ? prev.filter(c => c !== category)
                : [...prev, category]
        );
    };

    const filteredProducts = useMemo(() => {
        return mockProducts.filter(p => {
            const categoryMatch = selectedCategories.length === 0 || (p.category !== undefined && selectedCategories.includes(p.category));
            const priceMatch = p.price <= priceRange;
            return categoryMatch && priceMatch;
        });
    }, [selectedCategories, priceRange]);

    return (
        <main className="max-w-7xl mx-auto px-6 pt-32 pb-20">
            {/* Breadcrumbs */}
            <nav className="flex items-center gap-2 text-xs text-on-surface-variant mb-6">
                <Link to="/" className="hover:text-primary">Trang chủ</Link>
                <ChevronRight className="w-3 h-3" />
                <span className="font-bold text-on-surface">Danh mục Sản phẩm</span>
            </nav>

            <header className="mb-10">
                <h1 className="text-4xl font-extrabold tracking-tight mb-3">Tất cả sản phẩm</h1>
                <p className="text-on-surface-variant max-w-2xl">
                    Khám phá bộ sưu tập mô hình đa dạng nhất. Từ những bộ lắp ráp Technic phức tạp đến những Figure chi tiết cao cho nhà sưu tầm chuyên nghiệp.
                </p>
            </header>

            <div className="flex flex-col lg:flex-row gap-8 items-start">
                {/* Sidebar Filters */}
                <aside className="w-full lg:w-64 space-y-8 lg:sticky lg:top-32 flex-shrink-0">
                    <div className="bg-white border border-outline-variant/30 rounded-lg p-6 shadow-sm">
                        {/* Categories */}
                        <div className="mb-8">
                            <h3 className="text-sm font-bold uppercase tracking-wider mb-4">Danh mục</h3>
                            <div className="space-y-3">
                                {categories.map((category) => (
                                    <label key={category} className="flex items-center gap-3 cursor-pointer group">
                                        <input
                                            type="checkbox"
                                            className="w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer"
                                            checked={selectedCategories.includes(category)}
                                            onChange={() => handleCategoryToggle(category)}
                                        />
                                        <span className={`text-sm transition-colors ${selectedCategories.includes(category) ? 'text-primary font-semibold' : 'group-hover:text-primary'}`}>
                      {category}
                    </span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        {/* Price Range */}
                        <div className="mb-8">
                            <h3 className="text-sm font-bold uppercase tracking-wider mb-4">Giá bán (Tối đa)</h3>
                            <div className="space-y-4">
                                <input
                                    type="range"
                                    min="500000"
                                    max="10000000"
                                    step="500000"
                                    value={priceRange}
                                    onChange={(e) => setPriceRange(Number(e.target.value))}
                                    className="w-full h-1.5 bg-surface-variant rounded-lg appearance-none cursor-pointer accent-primary"
                                />
                                <div className="flex items-center justify-between text-xs font-bold text-on-surface-variant">
                                    <span className="bg-surface-variant px-2 py-1 rounded">500k</span>
                                    <span className="bg-primary-container text-on-primary-container px-2 py-1 rounded">
                    {priceRange / 1000}k
                  </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </aside>

                {/* Product Grid */}
                <div className="flex-1 w-full">
                    {/* Grid Controls */}
                    <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8 bg-white p-4 border border-outline-variant/30 rounded-lg shadow-sm">
                        <p className="text-sm text-on-surface-variant font-medium">
                            Đang hiển thị <span className="text-on-surface font-bold">{filteredProducts.length}</span> kết quả phù hợp
                        </p>
                        <div className="flex items-center gap-3 w-full sm:w-auto">
                            <span className="text-xs font-bold text-on-surface-variant uppercase">Sắp xếp:</span>
                            <select className="bg-surface-container-low border border-outline-variant/30 rounded-md px-4 py-1.5 text-sm font-semibold focus:ring-2 focus:ring-primary w-full sm:w-48 outline-none">
                                <option>Mới nhất</option>
                                <option>Giá: Thấp đến Cao</option>
                                <option>Giá: Cao đến Thấp</option>
                                <option>Bán chạy nhất</option>
                            </select>
                        </div>
                    </div>

                    {filteredProducts.length > 0 ? (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                            {filteredProducts.map(product => (
                                <ProductCard key={product.id} product={product} />
                            ))}
                        </div>
                    ) : (
                        <div className="bg-white border border-outline-variant/30 rounded-xl p-12 text-center shadow-sm">
                            <h3 className="text-xl font-bold mb-2">Không tìm thấy sản phẩm</h3>
                            <p className="text-on-surface-variant">Vui lòng điều chỉnh bộ lọc để xem thêm các sản phẩm khác.</p>
                            <button
                                onClick={() => {
                                    setSelectedCategories([]);
                                    setPriceRange(10000000);
                                }}
                                className="mt-6 text-primary font-bold hover:underline"
                            >
                                Xóa tất cả bộ lọc
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </main>
    );
}
