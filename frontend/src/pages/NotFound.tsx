import {Home, SearchX} from 'lucide-react';
import {Link} from 'react-router-dom';
import SeoHead from '../components/common/SeoHead';

export default function NotFound() {
    return <main
        className="flex min-h-[70vh] flex-col items-center justify-center bg-surface px-4 pb-24 pt-32 text-center">
        <SeoHead title="Không tìm thấy trang | SOBU" noIndex/>
        <SearchX className="h-14 w-14 text-outline/40"/><p
        className="mt-5 text-xs font-black uppercase tracking-[0.25em] text-primary">404</p>
        <h1 className="mt-2 text-3xl font-black text-on-surface sm:text-4xl">Trang không tồn tại</h1>
        <p className="mt-3 max-w-md text-sm font-semibold text-on-surface-variant">Đường dẫn có thể đã thay đổi hoặc nội
            dung không còn khả dụng.</p>
        <Link to="/"
              className="mt-7 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-xs font-black uppercase text-on-primary"><Home
            className="h-4 w-4"/>Về trang chủ</Link>
    </main>;
}
