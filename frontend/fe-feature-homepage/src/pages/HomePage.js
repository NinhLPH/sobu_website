import Footer from "../components/common/Footer";
import Header from "../components/common/Header";
import { PrimaryButton, TertiaryButton } from "../components/ui/Button"
import { ProductCard } from "../components/ui/ProductCard";
import { HeroCarousel } from "../components/homepage/HeroCarousel";
import { HotCategories } from "../components/homepage/HotCategories";
import { FaPalette } from "react-icons/fa6";
import { FaTools } from "react-icons/fa";
import { FaExternalLinkAlt } from "react-icons/fa";

// ảnh ví dụ thoi
import pic1 from "../assets/IMG_9827.JPG";
import pic2 from "../assets/IMG_9918.JPG";
import pic3 from "../assets/IMG_9863.JPG";
const Homepage = () => {
    return (
        <div className="min-h-screen font-sans antialiased">
            <Header />

            <main>

                <HeroCarousel />

                <section className="py-24 bg-surface">
                    <div className="container mx-auto px-8">
                        <div className="flex justify-between items-end mb-12">
                            <div>
                                <h2 className="text-4xl font-bold tracking-tightest mb-2">Hàng mới về</h2>
                                <p className="text-outline">Những siêu phẩm vừa cập bến tại showroom SOBU</p>
                            </div>
                            <TertiaryButton>Xem tất cả</TertiaryButton>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition", "Lắp ráp", "Hot Fig"]} tag={"HOT"} image={pic1} />
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition"]} tag={"HOT"} image={pic1} />
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition"]} tag={"HOT"} image={pic1} />
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition"]} tag={"HOT"} image={pic1} />
                            {/* sau này có data thì cho hiển thị đúng 4 sản phẩm mới nhất
                            {products.slice(0, 4).map(product => (
                             <ProductCard key={product.id} {...product} />
                             ))} */}
                        </div>
                    </div>
                </section>

                <HotCategories />

                <section className="py-24 bg-surface-container-low relative">
                    <div className="container mx-auto px-8 flex flex-col md:flex-row items-center gap-16">
                        <div className="w-full md:w-1/2">
                            <span className="w-fit p-4 mb-10 rounded-full bg-primary-container/15 flex items-center justify-center text-primary font-bold text-sm">SOBU RESPONSE</span>
                            <h2 className="text-5xl font-black tracking-tightest leading-[1.1] mb-6">
                                <span className="text-on_surface">Dịch vụ</span> <br />
                                <span className="text-primary">Custom Mô Hình</span> <br />
                                <span className="text-on_surface">Chuyên Nghiệp</span>
                            </h2>
                            <p className="text-lg text-on-surface/70 mb-8 leading-relaxed">Chúng tôi nhận sơn, mod và lắp ráp mô hình theo yêu cầu cá nhân hóa hoàn hảo.</p>
                            <div className="grid grid-cols-2 gap-6 mb-8 text-sm font-bold">
                                <div className="flex items-center gap-3"><div className="w-10 h-10 rounded-full bg-primary-container/20 flex items-center justify-center text-primary font-bold text-xl"><FaPalette /></div>Airbrushing chuyên nghiệp</div>
                                <div className="flex items-center gap-3"><div className="w-10 h-10 rounded-full bg-primary-container/20 flex items-center justify-center text-primary font-bold text-xl"><FaTools /></div>Weathering giả cổ</div>
                            </div>
                            <PrimaryButton>Báo giá ngay <FaExternalLinkAlt /></PrimaryButton>
                        </div>
                        <div className="w-full md:w-1/2 relative group">
                            <div className="relative rounded-lg overflow-hidden shadow-ambient z-10">
                                <img src={pic1} className="w-full h-[500px] object-cover transition-transform duration-700 group-hover:scale-105" alt="Custom service" />
                            </div>
                            <div className="absolute -bottom-10 -left-12 bg-white/80 backdrop-blur-[20px] p-8 rounded-[2rem] shadow-ambient max-w-sm z-20 border border-white/20">
                                <p className="text-sm italic leading-relaxed text-on_surface/90 mb-4">
                                    "Độ chi tiết vượt xa mong đợi. SOBU đã hiện thực hóa giấc mơ của tôi."
                                </p>
                                <div className="flex items-center gap-3">
                                    <div className="w-8 h-[1px] bg-primary/30"></div>
                                    <p className="text-[10px] font-bold uppercase tracking-blueprint text-outline">
                                        — Anh Tuấn, Collector
                                    </p>
                                </div>
                                <div className="absolute top-0 right-10 w-6 h-6 bg-white/80 backdrop-blur-[20px] rotate-45 -translate-y-1/2 border-tl border-white/20 hidden md:block"></div>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="py-16 bg-white">
                    <div className="container mx-auto px-8">
                        <p className="text-center text-[12px] font-bold text-outline uppercase tracking-blueprint mb-10">
                            Đối tác & Thương hiệu
                        </p>
                        <div className="flex justify-between items-center opacity-50 grayscale hover:grayscale-0 transition-all gap-8">
                            {['Bandai', 'Tamiya', 'Hot Toys', 'Good Smile', 'Kotobukiya', 'Revell'].map(brand => (
                                <span key={brand} className="text-xl font-black tracking-widest uppercase">{brand}</span>
                            ))}
                        </div>
                    </div>
                </section>

                <section className="py-24">
                    <div className="container mx-auto px-8">
                        <div className="flex justify-between items-center mb-12">
                            <h2 className="text-4xl font-bold tracking-tightest">Cẩm nang hướng dẫn</h2>
                            <div className="flex gap-2"> {/* Navigation buttons */} </div>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                            <div className="bg-white rounded-lg overflow-hidden shadow-ambient group cursor-pointer">
                                <div className="h-64 overflow-hidden">
                                    <img src={pic2} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                </div>
                                <div className="p-8">
                                    <span className="text-[10px] font-bold text-primary uppercase tracking-blueprint">Kỹ thuật cơ bản</span>
                                    <h3 className="text-xl font-bold mt-2 leading-tight">Cách dán decal nước siêu mịn không để lại vết</h3>
                                </div>
                            </div>
                            <div className="bg-white rounded-lg overflow-hidden shadow-ambient group cursor-pointer">
                                <div className="h-64 overflow-hidden">
                                    <img src={pic2} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                </div>
                                <div className="p-8">
                                    <span className="text-[10px] font-bold text-primary uppercase tracking-blueprint">Kỹ thuật cơ bản</span>
                                    <h3 className="text-xl font-bold mt-2 leading-tight">Cách dán decal nước siêu mịn không để lại vết</h3>
                                </div>
                            </div>
                            <div className="bg-white rounded-lg overflow-hidden shadow-ambient group cursor-pointer">
                                <div className="h-64 overflow-hidden">
                                    <img src={pic2} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                </div>
                                <div className="p-8">
                                    <span className="text-[10px] font-bold text-primary uppercase tracking-blueprint">Kỹ thuật cơ bản</span>
                                    <h3 className="text-xl font-bold mt-2 leading-tight">Cách dán decal nước siêu mịn không để lại vết</h3>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="py-24 bg-surface">
                    <div className="container mx-auto px-8 grid grid-cols-12 gap-16">
                        <div className="col-span-12 md:col-span-5">
                            <h2 className="text-4xl font-bold tracking-tightest mb-12">Blog của shop</h2>
                            <div className="space-y-10">
                                {[2, 6].map(i => (
                                    <div key={i} className="flex gap-6 items-start group cursor-pointer">
                                        <div className="w-24 h-24 rounded-md overflow-hidden flex-shrink-0">
                                            {/* <img src={`/blog${i}.jpg`} className="w-full h-full object-cover" /> */}
                                            <img src={pic3} className="w-full h-full object-cover" />
                                        </div>
                                        <div>
                                            <h4 className="font-bold text-lg group-hover:text-primary transition-colors leading-snug">Phóng sự: Triển lãm Gunpla Expo 2023 tại Sài Gòn</h4>
                                            <p className="text-outline text-sm mt-2 uppercase tracking-blueprint">12 Tháng 10, 2023</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className="col-span-12 md:col-span-7 grid grid-cols-2 gap-4">
                            {/* <div className="rounded-lg bg-surface_container h-80 overflow-hidden"><img src="/art1.jpg" className="w-full h-full object-cover" /></div>
                            <div className="rounded-lg bg-surface_container h-64 overflow-hidden mt-12"><img src="/art2.jpg" className="w-full h-full object-cover" /></div> */}
                            <div className="rounded-lg bg-surface_container h-80 overflow-hidden"><img src={pic3} className="w-full h-full object-cover" /></div>
                            <div className="rounded-lg bg-surface_container h-64 overflow-hidden mt-12"><img src={pic3} className="w-full h-full object-cover" /></div>
                        </div>
                    </div>
                </section>
            </main>

            <Footer />
        </div>
    );
}

export default Homepage;