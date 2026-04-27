import {Link} from 'react-router-dom';
import {ArrowRight, ShoppingCart} from 'lucide-react';

import {mockProducts} from '../data/mockData';

import ProductCard from "../components/common/ProductCart";

export default function HomePage() {
    const newArrivals = mockProducts.slice(0, 4);

    return (
        <main className="pt-32 space-y-24 pb-24">
            {/* Hero Section */}
            <section className="px-6 max-w-screen-2xl mx-auto">
                <div className="relative h-[600px] md:h-[716px] rounded-xl overflow-hidden group">
                    <div className="absolute inset-0 flex transition-transform duration-700">
                        <div className="min-w-full relative h-full">
                            <img
                                className="w-full h-full object-cover"
                                src="https://4kwallpapers.com/images/wallpapers/iron-man-marvel-superheroes-marvel-comics-3440x1440-7871.jpg"
                                alt="Hero banner"
                            />
                            <div
                                className="absolute inset-0 bg-gradient-to-t md:bg-gradient-to-r from-inverse-surface/90 via-inverse-surface/50 to-transparent"></div>
                            <div className="absolute bottom-12 md:bottom-16 left-6 md:left-16 max-w-2xl z-10">
                <span
                    className="inline-block px-4 py-1 bg-primary-container text-on-primary-container text-xs font-bold tracking-widest uppercase rounded-full mb-4">
                  MỚI RA MẮT
                </span>
                                <h1 className="text-5xl md:text-7xl font-black text-white tracking-tighter mb-6 leading-none">
                                    The Precision <br className="hidden md:block"/>Masterpiece.
                                </h1>
                                <p className="text-base md:text-lg text-white/80 mb-8 font-medium max-w-lg">
                                    Khám phá bộ sưu tập mô hình cơ khí chính xác nhất với độ chi tiết đến từng milimet.
                                </p>
                                <Link to="/products"
                                      className="inline-flex bg-gradient-to-br from-primary to-primary-container text-white px-8 py-4 rounded-full font-bold shadow-lg shadow-primary/20 hover:scale-105 transition-transform">
                                    Khám phá ngay
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Hàng mới về Section */}
            <section className="px-6 max-w-screen-2xl mx-auto">
                <div className="flex items-end justify-between mb-12">
                    <div>
                        <h2 className="text-4xl font-black text-on-background tracking-tighter">Hàng mới về</h2>
                        <p className="text-outline mt-2 hidden md:block">Những siêu phẩm vừa cập bến tại showroom
                            SOBU</p>
                    </div>
                    <Link to="/products"
                          className="text-primary font-bold flex items-center gap-2 group whitespace-nowrap">
                        Xem tất cả <ArrowRight className="transition-transform group-hover:translate-x-1 w-5 h-5"/>
                    </Link>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
                    {newArrivals.map(product => (
                        <ProductCard key={product.id} product={product}/>
                    ))}
                </div>
            </section>

            {/* Danh mục HOT Section */}
            <section className="px-6 max-w-screen-2xl mx-auto">
                <h2 className="text-4xl font-black text-on-background tracking-tighter mb-12">Danh mục HOT</h2>
                <div
                    className="grid grid-cols-1 md:grid-cols-4 grid-rows-none md:grid-rows-2 gap-4 h-auto md:h-[600px]">
                    <Link to="/category/gunpla"
                          className="md:col-span-2 md:row-span-2 relative rounded-lg overflow-hidden group cursor-pointer min-h-[300px]">
                        <img
                            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                            src="https://i.ytimg.com/vi/OkKzIT8aRc8/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLA6qaS_Q-Lf8JQi_x6wGYClBGrZJw"
                            alt="Gunpla"/>
                        <div className="absolute inset-0 bg-gradient-to-t from-primary/90 to-transparent"></div>
                        <div className="absolute bottom-8 left-8">
                            <h3 className="text-3xl font-black text-white mb-2">Gunpla Master</h3>
                            <p className="text-white/80">Bộ sưu tập mô hình lắp ráp cao cấp</p>
                        </div>
                    </Link>
                    <Link to="/category/designer-toys"
                          className="md:col-span-2 relative rounded-lg overflow-hidden group cursor-pointer min-h-[200px]">
                        <img
                            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                            src="https://i.redd.it/3eybuqtnwqvc1.jpeg"
                            alt="Designer Toys"/>
                        <div className="absolute inset-0 bg-gradient-to-t from-tertiary/90 to-transparent"></div>
                        <div className="absolute bottom-6 left-6">
                            <h3 className="text-2xl font-black text-white mb-1">Designer Toys</h3>
                            <p className="text-white/80">Nghệ thuật trong từng sản phẩm</p>
                        </div>
                    </Link>
                    <Link to="/category/military-kits"
                          className="relative rounded-lg overflow-hidden group cursor-pointer min-h-[200px]">
                        <img
                            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                            src="https://m.media-amazon.com/images/I/81xuu4InSwL._AC_UF894,1000_QL80_.jpg"
                            alt="Military"/>
                        <div
                            className="absolute inset-0 bg-slate-900/50 group-hover:bg-slate-900/30 transition-colors"></div>
                        <div className="absolute inset-0 flex items-center justify-center p-4 text-center">
                            <h3 className="text-xl font-bold text-white">Military Kits</h3>
                        </div>
                    </Link>
                    <Link to="/category/supercars"
                          className="relative rounded-lg overflow-hidden group cursor-pointer min-h-[200px]">
                        <img
                            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                            src="https://images.unsplash.com/photo-1583121274602-3e2820c69888?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
                            alt="Supercars"/>
                        <div
                            className="absolute inset-0 bg-slate-900/50 group-hover:bg-slate-900/30 transition-colors"></div>
                        <div className="absolute inset-0 flex items-center justify-center p-4 text-center">
                            <h3 className="text-xl font-bold text-white">Supercars</h3>
                        </div>
                    </Link>
                </div>
            </section>

        </main>
    );
}