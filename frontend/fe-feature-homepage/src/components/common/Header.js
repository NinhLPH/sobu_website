import { FaShoppingCart, FaSearch } from "react-icons/fa";
import { IoPersonSharp } from "react-icons/io5";
import logo from '../../assets/logo.png';

const Header = () => {
    return (
        <header className="sticky top-0 z-50 w-full">
            <div className="bg-white/80 backdrop-blur-[20px] h-20 flex items-center shadow-sm">
                <div className="container mx-auto px-8 flex justify-between items-center gap-8">
                    <a href="/" className="flex-shrink-0 transition-transform hover:scale-105">
                        <img src={logo} alt="SOBU Logo" className="h-10 w-auto object-contain"/>
                    </a>
                    <div className="flex-grow max-w-xl relative">
                        <input
                            type="text"
                            placeholder="Tìm kiếm mô hình..."
                            className="w-full bg-surface-container-low px-6 py-3 rounded-md text-sm outline-none focus:ring-2 focus:ring-primary/40 transition-all"
                        />
                        <span className="absolute right-4 top-3 text-outline"><FaSearch/></span>
                    </div>
                    <div className="flex items-center gap-6">
                        <button className="relative p-2 hover:bg-primary-container/20 rounded-full transition-colors">
                        <FaShoppingCart/>
                            <span className="absolute top-0 right-0 bg-primary text-white text-[10px] w-4 h-4 rounded-full flex items-center justify-center">3</span>
                        </button>
                        <button className="p-2 hover:bg-primary-container/20 rounded-full transition-colors"><IoPersonSharp/></button>
                    </div>
                </div>
            </div>
            <div className="bg-surface border-t border-outline/10 h-12 flex items-center">
                <div className="container mx-auto px-8 flex gap-10">
                    {['Sản phẩm', 'Hàng mới', 'Hướng dẫn', 'Dịch vụ', 'Mô hình Custom'].map((item) => (
                        <a
                            key={item}
                            href={`#${item}`}
                            className="text-[11px] font-bold tracking-blueprint uppercase text-on-surface hover:text-primary transition-colors"
                        >
                            {item}
                        </a>
                    ))}
                </div>
            </div>
        </header>
    )
}

export default Header;