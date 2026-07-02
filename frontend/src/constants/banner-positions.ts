import {BannerPosition, DeviceType} from '../enum/union-types';

export interface BannerPositionOption {
    value: BannerPosition;
    label: string;
    description: string;
    defaultLinkUrl: string;
    defaultDeviceType: DeviceType;
}

export const BANNER_POSITION_OPTIONS: BannerPositionOption[] = [
    {
        value: 'home_hero_carousel',
        label: 'home_hero_carousel',
        description: 'Carousel lon dau trang Home.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'site_left_sidebar_banner',
        label: 'site_left_sidebar_banner',
        description: 'Banner doc sticky ben trai layout public tren desktop lon.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'WEB',
    },
    {
        value: 'site_right_sidebar_banner',
        label: 'site_right_sidebar_banner',
        description: 'Banner doc sticky ben phai layout public tren desktop lon.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'WEB',
    },
    {
        value: 'home_section_01_banner',
        label: 'home_section_01_banner',
        description: 'Banner cua section BAN CHAY.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_custom_service_image_primary',
        label: 'home_custom_service_image_primary',
        description: 'Anh lon thu nhat trong section dich vu custom.',
        defaultLinkUrl: '/services',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_custom_service_image_secondary',
        label: 'home_custom_service_image_secondary',
        description: 'Anh lon thu hai trong section dich vu custom.',
        defaultLinkUrl: '/services',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_custom_service_image_tertiary',
        label: 'home_custom_service_image_tertiary',
        description: 'Anh nho thu ba trong section dich vu custom.',
        defaultLinkUrl: '/services',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_category_card_01',
        label: 'home_category_card_01',
        description: 'Anh nen category card thu nhat tren Home.',
        defaultLinkUrl: '/category/marvel',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_category_card_02',
        label: 'home_category_card_02',
        description: 'Anh nen category card thu hai tren Home.',
        defaultLinkUrl: '/category/dc',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_category_card_03',
        label: 'home_category_card_03',
        description: 'Anh nen category card thu ba tren Home.',
        defaultLinkUrl: '/category/hot wheels',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_category_card_04',
        label: 'home_category_card_04',
        description: 'Anh nen category card thu tu tren Home.',
        defaultLinkUrl: '/category/transformer',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_category_card_05',
        label: 'home_category_card_05',
        description: 'Anh nen category card thu nam tren Home.',
        defaultLinkUrl: '/category/naruto',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_category_card_06',
        label: 'home_category_card_06',
        description: 'Anh nen category card thu sau tren Home.',
        defaultLinkUrl: '/category/pacific rim',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_section_02_banner',
        label: 'home_section_02_banner',
        description: 'Banner cua section Dung Cu.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_promo_grid_top_left',
        label: 'home_promo_grid_top_left',
        description: 'O promo 2x2 cot trai hang tren.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_promo_grid_bottom_left',
        label: 'home_promo_grid_bottom_left',
        description: 'O promo 2x2 cot trai hang duoi.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_promo_grid_top_right',
        label: 'home_promo_grid_top_right',
        description: 'O promo 2x2 cot phai hang tren.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_promo_grid_bottom_right',
        label: 'home_promo_grid_bottom_right',
        description: 'O promo 2x2 cot phai hang duoi.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_section_03_banner',
        label: 'home_section_03_banner',
        description: 'Banner cua section Hotwheels.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
    {
        value: 'home_section_04_banner',
        label: 'home_section_04_banner',
        description: 'Banner cua section giam gia.',
        defaultLinkUrl: '/products',
        defaultDeviceType: 'ALL',
    },
];

export const findBannerPositionOption = (position: BannerPosition) =>
    BANNER_POSITION_OPTIONS.find((option) => option.value === position);
