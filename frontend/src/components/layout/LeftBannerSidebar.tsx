import BannerPlaceholder from './BannerPlaceholder';
import BannerMedia from '../common/BannerMedia';
import {getBannersForPlacement, usePublicUiStore} from '../../store/usePublicUiStore';

export default function LeftBannerSidebar() {
    const banners = usePublicUiStore((state) => state.banners);
    const banner = getBannersForPlacement(banners, 'PRODUCT_SIDEBAR', 'WEB')[0];

    return (
        <aside className="hidden min-[1440px]:block" aria-label="Quảng cáo bên trái">
            <div className="sticky top-28">
                {banner ? (
                    <BannerMedia
                        banner={banner}
                        className="h-[min(560px,calc(100vh-8rem))] min-h-[360px] w-full rounded-2xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm"
                        fallback={<BannerPlaceholder label="Quảng cáo trái"/>}
                    >
                    </BannerMedia>
                ) : <BannerPlaceholder label="Quảng cáo trái"/>}
            </div>
        </aside>
    );
}
