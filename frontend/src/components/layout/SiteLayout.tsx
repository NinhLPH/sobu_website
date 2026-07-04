import {Outlet, useLocation} from 'react-router-dom';
import LeftBannerSidebar from './LeftBannerSidebar';
import RightBannerSidebar from './RightBannerSidebar';
import {useEffect} from 'react';
import {usePublicUiStore} from '../../store/usePublicUiStore';
import ChatDock from '../common/ChatDock';

const STATIC_PAGE_PATHS = new Set(['/about', '/privacy', '/terms', '/policies/privacy', '/policies/terms']);

export default function SiteLayout() {
    const fetchBanners = usePublicUiStore((state) => state.fetchBanners);
    const location = useLocation();
    const sidebarStickyClassName = STATIC_PAGE_PATHS.has(location.pathname) ? 'top-40' : 'top-28';

    useEffect(() => {
        void fetchBanners();
    }, [fetchBanners]);

    return (
        <>
            <div className="mx-auto grid w-full max-w-[1504px] flex-1 grid-cols-1 gap-4 px-4 min-[1440px]:grid-cols-[160px_minmax(0,1120px)_160px]">
                <LeftBannerSidebar stickyClassName={sidebarStickyClassName}/>
                <div className="min-w-0 w-full">
                    <Outlet/>
                </div>
                <RightBannerSidebar stickyClassName={sidebarStickyClassName}/>
            </div>
            <ChatDock/>
        </>
    );
}
