import {createElement} from 'react';
import {FaFacebookF, FaInstagram, FaTiktok, FaYoutube} from 'react-icons/fa';
import {SiZalo} from 'react-icons/si';
import {IconType} from 'react-icons';
import {usePublicUiStore} from '../../store/usePublicUiStore';
import {parseJsonConfig} from '../../utils/website-config';

type SocialLinks = Record<string, string>;

interface SocialChannel {
    key: 'facebook' | 'instagram' | 'zalo' | 'tiktok' | 'youtube';
    label: string;
    Icon: IconType;
    className: string;
}

const SOCIAL_CHANNELS: SocialChannel[] = [
    {
        key: 'facebook',
        label: 'Facebook',
        Icon: FaFacebookF,
        className: 'bg-[#1877F2] text-white shadow-[0_14px_35px_rgba(24,119,242,0.28)] hover:bg-[#166FE5]',
    },
    {
        key: 'instagram',
        label: 'Instagram',
        Icon: FaInstagram,
        className: 'bg-[#E4405F] text-white shadow-[0_14px_35px_rgba(228,64,95,0.28)] hover:bg-[#D93155]',
    },
    {
        key: 'zalo',
        label: 'Zalo',
        Icon: SiZalo,
        className: 'bg-[#0068FF] text-white shadow-[0_14px_35px_rgba(0,104,255,0.28)] hover:bg-[#005CE0]',
    },
    {
        key: 'tiktok',
        label: 'Tiktok',
        Icon: FaTiktok,
        className: 'bg-[#111111] text-white shadow-[0_14px_35px_rgba(17,17,17,0.24)] hover:bg-[#2A2A2A]',
    },
    {
        key: 'youtube',
        label: 'Youtube',
        Icon: FaYoutube,
        className: 'bg-[#FF0000] text-white shadow-[0_14px_35px_rgba(255,0,0,0.24)] hover:bg-[#CC0000]',
    },
];

export default function ZaloChatDock() {
    const configMap = usePublicUiStore((state) => state.configMap);
    const socialLinks = parseJsonConfig<SocialLinks>(configMap, 'social_links', {});
    const visibleChannels = SOCIAL_CHANNELS
        .map((channel) => ({
            ...channel,
            href: socialLinks[channel.key]?.trim() || '',
        }))
        .filter((channel) => Boolean(channel.href));

    if (visibleChannels.length === 0) {
        return null;
    }

    return (
        <div aria-label="Thanh lien ket social" className="flex flex-col items-end gap-3">
            {visibleChannels.map(({key, label, Icon, href, className}) => (
                <a
                    key={key}
                    href={href}
                    target="_blank"
                    rel="noreferrer"
                    aria-label={`Mo ${label}`}
                    title={label}
                    className={`inline-flex h-14 w-14 cursor-pointer items-center justify-center rounded-full text-xl transition-colors duration-200 focus-visible:ring-2 focus-visible:ring-primary/40 focus-visible:ring-offset-2 focus-visible:ring-offset-surface-container-lowest ${className}`}
                >
                    {createElement(Icon as any, {'aria-hidden': true})}
                </a>
            ))}
        </div>
    );
}
