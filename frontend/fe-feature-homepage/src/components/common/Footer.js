import { FaMapMarkerAlt, FaPhoneAlt, FaRegCopyright, FaFacebook,FaInstagramSquare,FaYoutube  } from "react-icons/fa";
import { IoMdMail } from "react-icons/io";

const Footer = () => {
    return (
        <footer className="pt-20 pb-10">
            <div className="container mx-auto px-8">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-16">
                    <div className="space-y-6">
                        <div className="text-3xl font-black text-primary tracking-tightest">SOBU</div>
                        <p className="text-sm text-outline leading-relaxed">
                            Nơi hội tụ của những tâm hồn say mê mô hình chính xác. Chúng tôi không chỉ bán đồ chơi, chúng tôi bán những kiệt tác nghệ thuật thu nhỏ.
                        </p>
                        <div className="flex gap-4">
                            {[<FaFacebook/>, <FaInstagramSquare/>, <FaYoutube/>].map(social => (
                                <div key={social} className="w-10 h-10 rounded-full bg-primary-container/20 flex items-center justify-center text-primary font-bold text-xl hover:bg-primary-container hover:text-white transition-all cursor-pointer">
                                    {social}
                                </div>
                            ))}
                        </div>
                    </div>

                    <div>
                        <h4 className="text-sm font-bold tracking-blueprint uppercase text-on-surface mb-6">Menu Chính</h4>
                        <ul className="space-y-4 text-sm text-outline">
                            <li><a href="#" className="hover:text-primary transition-colors">Sản phẩm</a></li>
                            <li><a href="#" className="hover:text-primary transition-colors">Hàng mới</a></li>
                            <li><a href="#" className="hover:text-primary transition-colors">Hướng dẫn</a></li>
                            <li><a href="#" className="hover:text-primary transition-colors">Mô hình Custom</a></li>
                        </ul>
                    </div>

                    <div>
                        <h4 className="text-sm font-bold tracking-blueprint uppercase text-on-surface mb-6">Dịch vụ</h4>
                        <ul className="space-y-4 text-sm text-outline">
                            <li><a href="#" className="hover:text-primary transition-colors">Dịch vụ sơn</a></li>
                            <li><a href="#" className="hover:text-primary transition-colors">Thu mua</a></li>
                            <li><a href="#" className="hover:text-primary transition-colors">Pre-order</a></li>
                            <li><a href="#" className="hover:text-primary transition-colors">Custom theo yêu cầu</a></li>
                        </ul>
                    </div>

                    <div>
                        <h4 className="text-sm font-bold tracking-blueprint uppercase text-on-surface mb-6">Liên hệ</h4>
                        <ul className="space-y-4 text-sm text-outline">
                            <li className="flex gap-3"><FaMapMarkerAlt/><span>Đức Thắng, Quận Bắc Từ Liêm, Hà Nội</span></li>
                            <li className="flex gap-3"><FaPhoneAlt/><span>1900 1234 (8:00 - 21:00)</span></li>
                            <li className="flex gap-3"><IoMdMail/><span>support@sobu.vn</span></li>
                        </ul>
                    </div>
                </div>

                <div className="pt-8 border-t border-outline/10 flex flex-col md:flex-row justify-between items-center gap-4 text-[10px] font-bold tracking-blueprint uppercase text-outline">
                    <p><FaRegCopyright/>2025 SOBU. All rights reserved. The Precision Playground.</p>
                    <div className="flex gap-8">
                        <a href="#" className="hover:text-primary">Chính sách bảo mật</a>
                        <a href="#" className="hover:text-primary">Điều khoản dịch vụ</a>
                    </div>
                </div>
            </div>
        </footer>
    )
}

export default Footer;