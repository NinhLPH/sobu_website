import { HatGlasses, MapPin, Phone, Mail } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Footer() {
    return (
        <footer className="bg-slate-100 w-full pt-12 mt-auto">
            <div className="max-w-screen-2xl mx-auto px-8">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-16">
                    <div className="col-span-1 md:col-span-1">
                        <div className="text-2xl font-black text-primary mb-4">SOBU</div>
                        <p className="text-sm leading-relaxed text-slate-500 mb-6">Nơi hội tụ của những tâm hồn say mê mô hình chính xác. Chúng tôi không chỉ bán đồ chơi, chúng tôi bán những kiệt tác nghệ thuật thu nhỏ.</p>
                        <div className="flex gap-4">
                            <a href="#" className="w-10 h-10 rounded-full bg-white flex items-center justify-center text-primary shadow-sm hover:scale-110 transition-transform">
                                <HatGlasses className="w-5 h-5" />
                            </a>
                            <a href="#" className="w-10 h-10 rounded-full bg-white flex items-center justify-center text-primary shadow-sm hover:scale-110 transition-transform">
                                <HatGlasses className="w-5 h-5" />
                            </a>
                            <a href="#" className="w-10 h-10 rounded-full bg-white flex items-center justify-center text-primary shadow-sm hover:scale-110 transition-transform">
                                <HatGlasses className="w-5 h-5" />
                            </a>
                        </div>
                    </div>
                    <div>
                        <h4 className="font-bold text-primary mb-6">Menu Chính</h4>
                        <ul className="space-y-3 text-sm">
                            <li><Link to="/" className="text-slate-500 hover:text-primary transition-colors">Sản phẩm</Link></li>
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">Hàng mới</Link></li>
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">Hướng dẫn</Link></li>
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">Mô hình Custom</Link></li>
                        </ul>
                    </div>
                    <div>
                        <h4 className="font-bold text-primary mb-6">Dịch vụ</h4>
                        <ul className="space-y-3 text-sm">
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">CSKH</Link></li>
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">Thu mua</Link></li>
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">Pre-order</Link></li>
                            <li><Link to="#" className="text-slate-500 hover:text-primary transition-colors">Custom theo yêu cầu</Link></li>
                        </ul>
                    </div>
                    <div>
                        <h4 className="font-bold text-primary mb-6">Liên hệ</h4>
                        <ul className="space-y-4 text-sm text-slate-500">
                            <li className="flex items-start gap-3">
                                <MapPin className="text-primary w-5 h-5 shrink-0" />
                                <span>Hà Nội</span>
                            </li>
                            <li className="flex items-center gap-3">
                                <Phone className="text-primary w-5 h-5 shrink-0" />
                                <span>1900 1234 (8:00 - 21:00)</span>
                            </li>
                            <li className="flex items-center gap-3">
                                <Mail className="text-primary w-5 h-5 shrink-0" />
                                <span>support@sobu.vn</span>
                            </li>
                        </ul>
                    </div>
                </div>

                <div className="py-8 border-t border-slate-200 flex flex-col md:flex-row justify-between items-center gap-4">
                    <p className="text-sm text-slate-500 text-center md:text-left">© 2026 SOBU. All rights reserved.</p>
                    <div className="flex gap-8 text-sm text-slate-500">
                        <Link to="#" className="hover:text-primary transition-colors">Chính sách bảo mật</Link>
                        <Link to="#" className="hover:text-primary transition-colors">Điều khoản dịch vụ</Link>
                    </div>
                </div>
            </div>
        </footer>
    );
}
