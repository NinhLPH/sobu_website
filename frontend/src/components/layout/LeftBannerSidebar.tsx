import BannerPlaceholder from './BannerPlaceholder';
import BannerMedia from '../common/BannerMedia';
import {getBannersForPlacement, usePublicUiStore} from '../../store/usePublicUiStore';

interface LeftBannerSidebarProps {
    stickyClassName?: string;
}

export default function LeftBannerSidebar({stickyClassName = 'top-28'}: LeftBannerSidebarProps) {
    const banners = usePublicUiStore((state) => state.banners);
    const banner = getBannersForPlacement(banners, 'site_left_sidebar_banner', 'WEB')[0];

    return (
        <aside className="hidden min-[1440px]:block" aria-label="Quảng cáo bên trái">
            <div className={`sticky ${stickyClassName}`}>
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
