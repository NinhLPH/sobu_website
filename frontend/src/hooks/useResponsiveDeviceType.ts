import {useEffect, useState} from 'react';

const MOBILE_QUERY = '(max-width: 767px)';

export const useResponsiveDeviceType = (): 'WEB' | 'MOBILE' => {
    const [deviceType, setDeviceType] = useState<'WEB' | 'MOBILE'>(() =>
        typeof window !== 'undefined' && window.matchMedia(MOBILE_QUERY).matches ? 'MOBILE' : 'WEB'
    );

    useEffect(() => {
        const media = window.matchMedia(MOBILE_QUERY);
        const update = () => setDeviceType(media.matches ? 'MOBILE' : 'WEB');
        update();
        media.addEventListener('change', update);
        return () => media.removeEventListener('change', update);
    }, []);

    return deviceType;
};
