import {ReactNode, useEffect, useState} from 'react';
import {ChevronLeft, ChevronRight} from 'lucide-react';
import {BannerDTO} from '../../interface/public-ui-config.model';
import BannerMedia from './BannerMedia';

interface BannerCarouselProps {
    banners: BannerDTO[];
    fallback: ReactNode;
    className?: string;
    intervalMs?: number;
    imageFallback?: ReactNode;
}

export default function BannerCarousel({banners, fallback, className = '', intervalMs = 5000, imageFallback}: BannerCarouselProps) {
    const [current, setCurrent] = useState(0);

    useEffect(() => setCurrent(0), [banners]);
    useEffect(() => {
        if (banners.length < 2) return;
        const timer = window.setInterval(() => setCurrent((value) => (value + 1) % banners.length), intervalMs);
        return () => window.clearInterval(timer);
    }, [banners.length, intervalMs]);

    if (!banners.length) return <>{fallback}</>;

    return (
        <div className={`relative overflow-hidden ${className}`}>
            {banners.map((banner, index) => (
                <div key={banner.id} className={`absolute inset-0 transition-opacity duration-700 ${index === current ? 'opacity-100' : 'pointer-events-none opacity-0'}`}>
                    <BannerMedia banner={banner} className="h-full w-full" fallback={imageFallback || <div className="h-full w-full bg-surface-container"/>}>
                        <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/10 to-transparent"/>
                        <p className="absolute bottom-5 left-5 right-5 text-lg font-black text-white drop-shadow sm:text-2xl">
                            {banner.title}
                        </p>
                    </BannerMedia>
                </div>
            ))}
            {banners.length > 1 && (
                <>
                    <button type="button" onClick={() => setCurrent((current - 1 + banners.length) % banners.length)} aria-label="Banner trước" className="absolute left-3 top-1/2 z-10 -translate-y-1/2 rounded-full bg-black/45 p-2 text-white">
                        <ChevronLeft className="h-5 w-5"/>
                    </button>
                    <button type="button" onClick={() => setCurrent((current + 1) % banners.length)} aria-label="Banner sau" className="absolute right-3 top-1/2 z-10 -translate-y-1/2 rounded-full bg-black/45 p-2 text-white">
                        <ChevronRight className="h-5 w-5"/>
                    </button>
                    <div className="absolute bottom-3 left-1/2 z-10 flex -translate-x-1/2 gap-1.5">
                        {banners.map((banner, index) => <button key={banner.id} type="button" onClick={() => setCurrent(index)} aria-label={`Banner ${index + 1}`} className={`h-2 rounded-full ${index === current ? 'w-6 bg-white' : 'w-2 bg-white/50'}`}/>) }
                    </div>
                </>
            )}
        </div>
    );
}
