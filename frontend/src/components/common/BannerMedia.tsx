import {ReactNode, useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {BannerDTO} from '../../interface/public-ui-config.model';
import {getPublicImageUrl} from '../../utils/file-url';

interface BannerMediaProps {
    banner: BannerDTO;
    className?: string;
    imageClassName?: string;
    fallback?: ReactNode;
    children?: ReactNode;
}

export default function BannerMedia({
    banner,
    className = '',
    imageClassName = '',
    fallback = null,
    children,
}: BannerMediaProps) {
    const [failed, setFailed] = useState(false);

    useEffect(() => setFailed(false), [banner.id, banner.imageUrl]);

    if (failed || !banner.imageUrl?.trim()) return <>{fallback}</>;

    const content = (
        <div className={`relative overflow-hidden ${className}`}>
            <img
                src={getPublicImageUrl(banner.imageUrl)}
                alt={banner.title}
                className={`h-full w-full object-cover ${imageClassName}`}
                onError={() => setFailed(true)}
            />
            {children}
        </div>
    );

    const linkUrl = banner.linkUrl?.trim();
    if (!linkUrl) return content;
    if (linkUrl.startsWith('/')) return <Link to={linkUrl}>{content}</Link>;
    if (/^https?:\/\//i.test(linkUrl)) {
        return <a href={linkUrl} target="_blank" rel="noreferrer">{content}</a>;
    }
    return content;
}
