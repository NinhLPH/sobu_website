import React from "react";
import Sidebar from "../components/productlist/Sidebar";
import { ProductCard } from "../components/ui/ProductCard";
import { FaCartPlus } from "react-icons/fa";

// example pic thoi
import pic1 from "../../src/assets/IMG_9827.JPG";
import Header from "../components/common/Header";

const ProductList = () => {
    return (
        <div className="min-h-screen bg-surface font-sans antialiased">

            <Header />

            <div className="container mx-auto pt-4 px-8">

                {/* Breadcum-for more details để sau sửa thêm */}
                <nav className="text-[10px] font-bold text-outline uppercase tracking-blueprint mb-4">
                    Trang chủ <span className="mx-2">›</span> Danh mục sản phẩm
                </nav>

                <div className="max-w-3xl mb-12">
                    <h1 className="text-5xl font-black text-on_surface tracking-tightest mb-4">Tất cả sản phẩm</h1>
                    <p className="text-lg text-outline leading-relaxed font-light">
                        Khám phá bộ sưu tập mô hình đa dạng nhất. Từ những bộ lắp ráp Technic phức tạp đến những Figure chi tiết cao cho nhà sưu tầm chuyên nghiệp.
                    </p>
                </div>

                <div className="flex flex-col md:flex-row gap-12">
                    <Sidebar />

                    <div className="flex-1">
                        <div className="flex justify-between items-center mb-8 pb-4">
                            <p className="text-sm text-outline">Đang hiển thị <span className="font-bold text-on_surface">24</span> kết quả phù hợp</p>
                            <div className="flex items-center gap-4">
                                <span className="text-[10px] font-bold uppercase tracking-blueprint text-outline">Sắp xếp:</span>
                                <select className="bg-transparent font-bold text-sm text-on_surface border-none focus:ring-0 cursor-pointer">
                                    <option>Mới nhất</option>
                                    <option>Giá thấp đến cao</option>
                                    <option>Giá cao đến thấp</option>
                                </select>
                            </div>
                        </div>


                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">

                            {/* FEATURED CARD - cái này to nhất luôn chiếm diện tích như thế này, sau kết hợp be thì sửa một vài attribute để nhận api hoặc làm một cái riêng*/}
                            <div className="col-span-1 md:col-span-2 bg-white rounded-lg p-8 flex flex-col md:flex-row gap-8 shadow-ambient group">
                                <div className="w-full md:w-2/5 aspect-square bg-surface_container_low rounded-lg overflow-hidden">
                                    <img src="/ferrari-daytona.jpg" className="w-full h-full object-cover group-hover:scale-105 transition-all duration-700" alt="Featured" />
                                </div>
                                <div className="flex-1 flex flex-col justify-center">
                                    <span className="bg-error text-white text-[10px] font-bold px-3 py-1 rounded-full w-fit mb-4 tracking-blueprint">HOT TREND</span>
                                    <p className="text-[10px] font-bold text-primary uppercase tracking-blueprint mb-2">LEGO TECHNIC</p>
                                    <h2 className="text-3xl font-bold text-on_surface leading-tight mb-4">Ferrari Daytona SP3 - 1:8 Scale</h2>
                                    <div className="flex items-center gap-4 mb-8">
                                        <span className="text-2xl font-black text-on_surface">8.500.000đ</span>
                                        <span className="text-outline line-through text-sm italic">9.200.000đ</span>
                                    </div>
                                    <button className="w-fit px-8 py-4 rounded-full bg-gradient-to-br from-[#00618e] to-[#4bbafe] text-white font-bold text-sm flex items-center gap-3 hover:scale-105 transition-all">
                                        <FaCartPlus />Thêm vào giỏ hàng
                                    </button>
                                </div>
                            </div>

                            {/* Standard Cards - Sử dụng lại component ProductCard để tạo một l products, đã có tailwindcc lưới bên trên rồi */}
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition", "Lắp ráp", "Hot Fig"]} tag={"HOT"} image={pic1} />
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition"]} tag={"HOT"} image={pic1} />
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition"]} tag={"HOT"} image={pic1} />
                            <ProductCard title={"Asura Guardian V2"} price={"2.450.000"} categories={["Limited Edition"]} tag={"HOT"} image={pic1} />
                        </div>

                        {/*cái này là phần chuyển page, sau phải thêm UX đấy, pagging */}
                        <div className="mt-20 flex justify-center items-center gap-4">
                            <button className="w-10 h-10 rounded-md bg-surface_container_low flex items-center justify-center hover:bg-primary/10 transition-colors">‹</button>
                            <button className="w-10 h-10 rounded-md bg-primary text-white font-bold text-sm shadow-lg">1</button>
                            <button className="w-10 h-10 rounded-md text-on_surface font-bold text-sm hover:bg-surface_container_low transition-colors">2</button>
                            <button className="w-10 h-10 rounded-md text-on_surface font-bold text-sm hover:bg-surface_container_low transition-colors">3</button>
                            <span className="text-outline mx-2">...</span>
                            <button className="w-10 h-10 rounded-md text-on_surface font-bold text-sm hover:bg-surface_container_low transition-colors">12</button>
                            <button className="w-10 h-10 rounded-md bg-surface_container_low flex items-center justify-center hover:bg-primary/10 transition-colors">›</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default ProductList;