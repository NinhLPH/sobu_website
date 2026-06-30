import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {ChevronLeft, ChevronRight, Star} from 'lucide-react';
import {HERO_SLIDES, mockBlogs, placeholderImages} from '../data/mockData';
import ProductSlider from '../components/common/ProductSlider';
import {useProductStore} from '../store/useProductStore';
import {mapListItemToProductModel} from '../interface/product.model';
import {BannerDTO} from '../interface/public-ui-config.model';
import {BannerPosition} from '../enum/union-types';
import {getBannersForPlacement, usePublicUiStore} from '../store/usePublicUiStore';
import {useResponsiveDeviceType} from '../hooks/useResponsiveDeviceType';
import {parseJsonConfig, PublicConfigMap} from '../utils/website-config';
import BannerMedia from '../components/common/BannerMedia';
import BannerCarousel from '../components/common/BannerCarousel';

interface SectionHeaderProps {
    title: string;
    subtitle?: string;
}

interface CategoryCardConfig {
    label: string;
    href: string;
    bannerPosition?: BannerPosition;
}

interface PartnerBrandConfig {
    name: string;
    logoUrl: string;
}

interface PromoTileConfig {
    position: BannerPosition;
    fallbackImage: string;
    title: string;
    description: string;
    ctaLabel?: string;
    ctaUrl?: string;
    align?: 'center' | 'end';
    solidButton?: boolean;
}

const DEFAULT_BEST_SELLER_BANNER = 'https://i0.wp.com/www.comicbookrevolution.com/wp-content/uploads/2023/12/transformers-4-previw-banner.jpg';
const DEFAULT_TOOLS_BANNER = 'https://tooltechvietnam.com/wp-content/uploads/2023/03/handtools.jpg';
const DEFAULT_HOTWHEELS_BANNER = 'https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1600&auto=format&fit=crop';
const DEFAULT_SALE_BANNER = 'https://img.magnific.com/free-vector/modern-black-friday-holiday-sale-offer-banner-get-30-percent-price-drop-vector_1017-47794.jpg?semt=ais_hybrid&w=740&q=80';
const DEFAULT_PROMO_POSTER = 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg';
const DEFAULT_PROMO_CAR = 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg';
const DEFAULT_CUSTOM_TERTIARY = 'https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop';

const DEFAULT_CATEGORY_CARDS: CategoryCardConfig[] = [
    {label: 'Marvel', href: '/category/marvel', bannerPosition: 'home_category_card_01'},
    {label: 'DC', href: '/category/dc', bannerPosition: 'home_category_card_02'},
    {label: 'Hot Wheels', href: '/category/hot wheels', bannerPosition: 'home_category_card_03'},
    {label: 'Transformer', href: '/category/transformer', bannerPosition: 'home_category_card_04'},
    {label: 'Naruto', href: '/category/naruto', bannerPosition: 'home_category_card_05'},
    {label: 'Pacific Rim', href: '/category/pacific rim', bannerPosition: 'home_category_card_06'},
];

const CATEGORY_CARD_POSITIONS: BannerPosition[] = [
    'home_category_card_01',
    'home_category_card_02',
    'home_category_card_03',
    'home_category_card_04',
    'home_category_card_05',
    'home_category_card_06',
];

const DEFAULT_PARTNER_BRANDS: PartnerBrandConfig[] = [
    {name: 'BANDAI', logoUrl: 'https://placehold.co/180x60/e60012/ffffff?text=BANDAI'},
    {name: 'HOT TOYS', logoUrl: 'https://placehold.co/180x60/111111/f1b82d?text=HOT+TOYS'},
    {name: 'TAMIYA', logoUrl: 'https://placehold.co/180x60/0054a6/ffffff?text=TAMIYA'},
    {name: 'LEGO', logoUrl: 'https://placehold.co/180x60/ffd500/000000?text=LEGO'},
    {name: 'MATTEL', logoUrl: 'https://placehold.co/180x60/e5142a/ffffff?text=MATTEL'},
    {name: 'HASBRO', logoUrl: 'https://placehold.co/180x60/0072ce/ffffff?text=HASBRO'},
];

const readConfig = (configs: PublicConfigMap, key: string, fallback: string, allowEmpty = false) => {
    if (!Object.prototype.hasOwnProperty.call(configs, key)) return fallback;
    const value = configs[key] ?? '';
    const trimmedValue = value.trim();
    if (trimmedValue || allowEmpty) return trimmedValue;
    return fallback;
};

const asArray = <T,>(value: T[] | unknown, fallback: T[]) =>
    Array.isArray(value) ? value : fallback;

const SectionHeader = ({title, subtitle}: SectionHeaderProps) => (
    <div className="mb-4 flex flex-wrap items-center gap-2 sm:mb-6 sm:gap-4">
        <h2 className="text-lg font-black uppercase text-on-surface sm:text-2xl md:text-3xl">
            {title}
        </h2>
        {subtitle && (
            <>
                <div className="h-6 w-[2px] bg-on-surface"/>
                <span className="text-xs font-medium text-on-surface sm:whitespace-nowrap sm:text-sm">{subtitle}</span>
            </>
        )}
        <div className="ml-4 h-[1px] flex-1 bg-on-surface/30"/>
    </div>
);

function SectionWithBanner({
    title,
    subtitle,
    bannerImg,
    products,
    banners = [],
    ctaLabel,
    ctaUrl,
}: {
    title: string;
    subtitle?: string;
    bannerImg: string;
    products: any[];
    banners?: BannerDTO[];
    ctaLabel?: string;
    ctaUrl?: string;
}) {
    return (
        <section className="mb-10 w-full px-3 sm:mb-20 sm:px-6">
            <SectionHeader title={title} subtitle={subtitle}/>
            <BannerCarousel
                banners={banners}
                className="mb-5 h-[150px] w-full rounded-xl bg-surface-container sm:mb-8 sm:h-[200px] md:h-[300px]"
                fallback={(
                    <div className="mb-5 h-[150px] w-full overflow-hidden rounded-xl bg-surface-container sm:mb-8 sm:h-[200px] md:h-[300px]">
                        <img src={bannerImg} className="h-full w-full object-cover" alt={`${title} banner`}/>
                    </div>
                )}
                imageFallback={<img src={bannerImg} className="h-full w-full object-cover" alt={`${title} banner fallback`}/>}
            />
            <ProductSlider products={products}/>
            {ctaLabel && ctaUrl && (
                <div className="mt-5 flex justify-center sm:mt-8">
                    <Link
                        to={ctaUrl}
                        className="rounded-full border border-on-surface px-6 py-2 text-xs font-bold uppercase text-on-surface transition-all hover:bg-on-surface hover:text-surface sm:px-8 sm:text-sm"
                    >
                        {ctaLabel}
                    </Link>
                </div>
            )}
        </section>
    );
}

function BannerImage({
    banner,
    fallbackImage,
    alt,
    className,
    imageClassName = '',
}: {
    banner?: BannerDTO;
    fallbackImage: string;
    alt: string;
    className: string;
    imageClassName?: string;
}) {
    const fallback = (
        <div className={className}>
            <img src={fallbackImage} className={`h-full w-full object-cover ${imageClassName}`} alt={alt}/>
        </div>
    );
    if (!banner) return fallback;
    return (
        <BannerMedia
            banner={banner}
            className={className}
            imageClassName={imageClassName}
            fallback={fallback}
        />
    );
}

function PromoTile({
    banner,
    config,
}: {
    banner?: BannerDTO;
    config: PromoTileConfig;
}) {
    const justifyClass = config.align === 'end' ? 'justify-end' : 'justify-center';

    return (
        <div className={`relative flex h-36 flex-col items-start ${justifyClass} overflow-hidden p-3 text-white min-[390px]:h-40 sm:h-[300px] sm:p-12`}>
            <BannerImage
                banner={banner}
                fallbackImage={config.fallbackImage}
                alt={config.title || config.description}
                className="absolute inset-0 h-full w-full"
            />
            <div className="absolute inset-0 z-10 bg-black/50"/>
            <div className="relative z-20">
                {config.title && (
                    <h2 className="mb-2 text-sm font-black uppercase leading-tight min-[390px]:text-base sm:mb-4 sm:text-3xl">
                        {config.title}
                    </h2>
                )}
                {config.description && (
                    <p className="mb-3 max-w-sm text-[10px] font-medium uppercase leading-snug text-white/85 sm:mb-6 sm:text-sm">
                        {config.description}
                    </p>
                )}
                {config.ctaLabel && config.ctaUrl && (
                    <Link
                        to={config.ctaUrl}
                        className={`inline-flex border border-white px-3 py-1.5 text-[9px] font-bold uppercase transition-colors sm:px-8 sm:py-2 sm:text-xs ${
                            config.solidButton
                                ? 'bg-white text-black hover:bg-transparent hover:text-white'
                                : 'bg-transparent text-white hover:bg-white hover:text-black'
                        }`}
                    >
                        {config.ctaLabel}
                    </Link>
                )}
            </div>
        </div>
    );
}

export default function HomePage() {
    const [current, setCurrent] = useState(0);
    const {products, fetchProducts} = useProductStore();
    const banners = usePublicUiStore((state) => state.banners);
    const configMap = usePublicUiStore((state) => state.configMap);
    const deviceType = useResponsiveDeviceType();

    const bannersFor = (position: BannerPosition) => getBannersForPlacement(banners, position, deviceType);
    const firstBanner = (position: BannerPosition) => getBannersForPlacement(banners, position, deviceType)[0];
    const heroBanners = bannersFor('home_hero_carousel');
    const bestSellerBanners = bannersFor('home_section_01_banner');
    const heroSlideCount = heroBanners.length || HERO_SLIDES.length;
    const heroSlides = heroBanners.length ? heroBanners : HERO_SLIDES;

    useEffect(() => {
        fetchProducts();
    }, [fetchProducts]);

    useEffect(() => {
        setCurrent(0);
    }, [heroSlideCount]);

    useEffect(() => {
        const slideTimer = window.setInterval(() => {
            setCurrent((prev) => (prev + 1) % heroSlideCount);
        }, 5000);
        return () => clearInterval(slideTimer);
    }, [heroSlideCount]);

    const mappedProducts = products.map(mapListItemToProductModel);
    const customBadges = asArray(parseJsonConfig<string[]>(configMap, 'home_custom_service_badges', [
        'Do Led cam ung',
        'Son mo hinh chuan phim',
        'Custom theo y thich',
    ]), [
        'Do Led cam ung',
        'Son mo hinh chuan phim',
        'Custom theo y thich',
    ]);
    const categoryCards = asArray(parseJsonConfig<CategoryCardConfig[]>(configMap, 'home_category_cards', DEFAULT_CATEGORY_CARDS), DEFAULT_CATEGORY_CARDS);
    const partnerBrands = asArray(parseJsonConfig<PartnerBrandConfig[]>(configMap, 'home_partner_brands', DEFAULT_PARTNER_BRANDS), DEFAULT_PARTNER_BRANDS);

    const promoTiles: PromoTileConfig[] = [
        {
            position: 'home_promo_grid_top_left',
            fallbackImage: DEFAULT_PROMO_POSTER,
            title: readConfig(configMap, 'home_promo_grid_top_left_title', 'HOT WHEELS'),
            description: readConfig(configMap, 'home_promo_grid_top_left_description', 'KHAM PHA NHUNG MAU XE MO HINH HOT NHAT DANH CHO NGUOI DAM ME TOC DO.'),
            ctaLabel: readConfig(configMap, 'home_promo_grid_top_left_cta_label', 'Xem them', true),
            ctaUrl: readConfig(configMap, 'home_promo_grid_top_left_cta_url', '/products'),
        },
        {
            position: 'home_promo_grid_bottom_left',
            fallbackImage: DEFAULT_PROMO_CAR,
            title: readConfig(configMap, 'home_promo_grid_bottom_left_title', '', true),
            description: readConfig(configMap, 'home_promo_grid_bottom_left_description', 'SUU TAM NHUNG MAU XE HUYEN THOAI - TU SIEU XE HIEN DAI DEN CLASSIC CO DIEN.'),
            ctaLabel: readConfig(configMap, 'home_promo_grid_bottom_left_cta_label', '', true),
            ctaUrl: readConfig(configMap, 'home_promo_grid_bottom_left_cta_url', '/products'),
            align: 'end',
        },
        {
            position: 'home_promo_grid_top_right',
            fallbackImage: DEFAULT_PROMO_CAR,
            title: readConfig(configMap, 'home_promo_grid_top_right_title', '', true),
            description: readConfig(configMap, 'home_promo_grid_top_right_description', 'DISCOVER LIMITED EDITION CARS AND EXCLUSIVE RELEASES FOR TRUE COLLECTORS.'),
            ctaLabel: readConfig(configMap, 'home_promo_grid_top_right_cta_label', '', true),
            ctaUrl: readConfig(configMap, 'home_promo_grid_top_right_cta_url', '/products'),
        },
        {
            position: 'home_promo_grid_bottom_right',
            fallbackImage: DEFAULT_PROMO_POSTER,
            title: readConfig(configMap, 'home_promo_grid_bottom_right_title', 'GIFT FOR COLLECTORS'),
            description: readConfig(configMap, 'home_promo_grid_bottom_right_description', 'MON QUA HOAN HAO CHO NGUOI YEU XE VA DAM ME MO HINH.'),
            ctaLabel: readConfig(configMap, 'home_promo_grid_bottom_right_cta_label', 'Xem them', true),
            ctaUrl: readConfig(configMap, 'home_promo_grid_bottom_right_cta_url', '/products'),
            solidButton: true,
        },
    ];

    const nextSlide = () => {
        setCurrent((prev) => (prev === heroSlideCount - 1 ? 0 : prev + 1));
    };

    const prevSlide = () => {
        setCurrent((prev) => (prev === 0 ? heroSlideCount - 1 : prev - 1));
    };

    return (
        <main className="w-full min-w-0 space-y-10 bg-surface pb-16 pt-24 sm:space-y-20 sm:pb-24">
            <section className="group/hero relative mx-4 flex h-[300px] items-center justify-center sm:mx-6 sm:h-[400px] md:h-[500px]">
                <div className="absolute bottom-8 left-[5%] top-8 z-0 w-[80%] rounded-3xl bg-outline-variant/20"/>
                <div className="absolute bottom-8 right-[5%] top-8 z-0 w-[80%] rounded-3xl bg-outline-variant/30"/>
                <div className="relative z-10 flex h-full w-[90%] items-center justify-center overflow-hidden rounded-3xl shadow-2xl">
                    {heroSlides.map((slide, index) => (
                        <div
                            key={'id' in slide ? slide.id : index}
                            className={`absolute inset-0 h-full w-full transition-all duration-1000 ease-in-out ${
                                index === current ? 'opacity-100 pointer-events-auto' : 'pointer-events-none opacity-0'
                            }`}
                        >
                            {'imageUrl' in slide ? (
                                <BannerMedia
                                    banner={slide}
                                    className={`absolute inset-0 transition-transform duration-[2000ms] ease-out ${index === current ? 'scale-100' : 'scale-105'}`}
                                    imageClassName="mix-blend-overlay opacity-80"
                                    fallback={<img src={HERO_SLIDES[index % HERO_SLIDES.length].image} className="absolute inset-0 h-full w-full object-cover opacity-80 mix-blend-overlay" alt={`${slide.title} fallback`}/>}
                                />
                            ) : (
                                <img
                                    src={slide.image}
                                    className={`absolute inset-0 h-full w-full object-cover mix-blend-overlay transition-transform duration-[2000ms] ease-out ${
                                        index === current ? 'scale-100 opacity-80' : 'scale-105 opacity-0'
                                    }`}
                                    alt={slide.title}
                                />
                            )}
                            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/30 to-black/20"/>
                        </div>
                    ))}
                </div>
                <div className="absolute -bottom-8 left-1/2 flex -translate-x-1/2 items-center gap-3 rounded-full border border-outline-variant/20 bg-surface px-4 py-2 shadow-xs">
                    <button type="button" onClick={prevSlide} aria-label="Banner truoc" className="cursor-pointer text-on-surface/60 transition-all hover:text-primary">
                        <ChevronLeft className="h-4 w-4"/>
                    </button>
                    {heroSlides.map((slide, index) => (
                        <button
                            key={'id' in slide ? slide.id : index}
                            onClick={() => setCurrent(index)}
                            className={`h-2 rounded-full transition-all duration-300 ${
                                index === current ? 'w-6 bg-primary' : 'w-2 bg-outline/40 hover:bg-outline'
                            }`}
                            aria-label={`Go to slide ${index + 1}`}
                        />
                    ))}
                    <button type="button" onClick={nextSlide} aria-label="Banner sau" className="cursor-pointer text-on-surface/60 transition-all hover:text-primary">
                        <ChevronRight className="h-4 w-4"/>
                    </button>
                </div>
            </section>

            <div className="pt-10">
                <SectionWithBanner
                    title={readConfig(configMap, 'home_section_01_title', 'BÁN CHẠY')}
                    subtitle={readConfig(configMap, 'home_section_01_subtitle', 'Giao Hang Toan Quoc', true)}
                    bannerImg={DEFAULT_BEST_SELLER_BANNER}
                    products={mappedProducts}
                    banners={bestSellerBanners}
                    ctaLabel={readConfig(configMap, 'home_section_01_label', 'Xem them', true)}
                    ctaUrl={readConfig(configMap, 'home_section_01_cta_url', '/products')}
                />
            </div>

            <section className="mb-10 grid w-full grid-cols-1 items-center gap-6 px-3 sm:mb-20 sm:px-6 lg:grid-cols-12 lg:gap-12">
                <div className="space-y-6 lg:col-span-5">
                    <h2 className="text-2xl font-black uppercase leading-tight tracking-tight text-on-surface md:text-4xl">
                        {readConfig(configMap, 'home_custom_service_title', 'DICH VU DO MO HINH SO 1 VIET NAM')}
                    </h2>
                    <div className="flex flex-col items-start gap-2 pt-2 sm:gap-4 sm:pt-4">
                        {customBadges.map((badge) => (
                            <span key={badge} className="rounded-full bg-primary px-4 py-1.5 text-xs font-bold text-on-primary sm:px-6 sm:py-2 sm:text-sm">
                                {badge}
                            </span>
                        ))}
                    </div>
                    <Link
                        to={readConfig(configMap, 'home_custom_service_cta_url', '/services')}
                        className="mt-3 inline-block rounded-full border border-on-surface px-6 py-2 text-xs font-black uppercase tracking-widest text-on-surface transition-colors hover:bg-on-surface hover:text-surface sm:mt-4 sm:px-8"
                    >
                        {readConfig(configMap, 'home_custom_service_cta_label', 'CUSTOM NGAY')}
                    </Link>
                </div>
                <div className="relative h-[240px] sm:h-[400px] lg:col-span-7">
                    <div className="absolute right-0 top-0 h-[60%] w-[55%] overflow-hidden rounded-lg bg-surface-container shadow-lg">
                        <BannerImage banner={firstBanner('home_custom_service_image_primary')} fallbackImage={placeholderImages.custom1} alt="Custom primary" className="h-full w-full"/>
                    </div>
                    <div className="absolute bottom-10 right-[10%] z-10 h-[60%] w-[55%] overflow-hidden rounded-lg bg-surface-container-high shadow-xl">
                        <BannerImage banner={firstBanner('home_custom_service_image_secondary')} fallbackImage={placeholderImages.custom2} alt="Custom secondary" className="h-full w-full"/>
                    </div>
                    <div className="absolute left-0 top-20 h-[50%] w-[45%] overflow-hidden rounded-lg bg-surface-container-low shadow-md">
                        <BannerImage banner={firstBanner('home_custom_service_image_tertiary')} fallbackImage={DEFAULT_CUSTOM_TERTIARY} alt="Custom tertiary" className="h-full w-full" imageClassName="opacity-80"/>
                    </div>
                </div>
            </section>

            <section className="mb-10 w-full px-3 sm:mb-20 sm:px-6">
                <SectionHeader
                    title={readConfig(configMap, 'home_section_02_title', 'MO HINH CUSTOM')}
                    subtitle={readConfig(configMap, 'home_section_02_subtitle', 'Giao Hang Toan Quoc', true)}
                />
                <ProductSlider products={mappedProducts.slice().reverse()}/>
                {readConfig(configMap, 'home_section_02_cta_label', 'Xem them', true) && (
                    <div className="mt-8 flex justify-center">
                        <Link
                            to={readConfig(configMap, 'home_section_02_cta_url', '/products')}
                            className="rounded-full border border-on-surface px-8 py-2 text-sm font-bold uppercase text-on-surface transition-all hover:bg-on-surface hover:text-surface"
                        >
                            {readConfig(configMap, 'home_section_02_cta_label', 'Xem them', true)}
                        </Link>
                    </div>
                )}
            </section>

            <section className="mb-10 w-full px-3 text-center sm:mb-20 sm:px-6">
                <h2 className="mb-6 text-2xl font-black uppercase text-on-surface sm:mb-12 sm:text-3xl">
                    {readConfig(configMap, 'home_category_title', 'The loai mo hinh')}
                </h2>
                <div className="mb-6 grid grid-cols-2 gap-3 sm:mb-8 sm:gap-6 md:grid-cols-3">
                    {categoryCards.map((cat, index) => {
                        const bannerPosition = CATEGORY_CARD_POSITIONS[index] || cat.bannerPosition;
                        const banner = bannerPosition ? firstBanner(bannerPosition) : undefined;
                        return (
                            <Link
                                to={cat.href}
                                key={`${cat.label}-${cat.href}`}
                                className="group relative h-28 overflow-hidden rounded-xl bg-surface-container shadow-sm transition-all hover:shadow-md sm:h-40 sm:rounded-2xl md:h-56"
                            >
                                <div className="absolute inset-0 z-10 bg-gradient-to-t from-black/60 to-transparent"/>
                                <BannerImage
                                    banner={banner}
                                    fallbackImage={placeholderImages.hotwheels}
                                    alt={cat.label}
                                    className="absolute inset-0 h-full w-full transition-transform duration-500 group-hover:scale-105"
                                />
                                <div className="absolute inset-0 z-20 flex items-end justify-center pb-6">
                                    <h3 className="text-sm font-black text-white sm:text-xl">{cat.label}</h3>
                                </div>
                            </Link>
                        );
                    })}
                </div>
            </section>

            <SectionWithBanner
                title={readConfig(configMap, 'home_section_03_title', 'Dung Cu')}
                subtitle={readConfig(configMap, 'home_section_03_subtitle', 'Giao Hang Toan Quoc', true)}
                bannerImg={DEFAULT_TOOLS_BANNER}
                products={mappedProducts}
                banners={bannersFor('home_section_02_banner')}
                ctaLabel={readConfig(configMap, 'home_section_03_cta_label', 'Xem them', true)}
                ctaUrl={readConfig(configMap, 'home_section_03_cta_url', '/products')}
            />

            <section className="mb-14 grid grid-cols-2 sm:mb-20">
                <div className="flex h-full flex-col">
                    <PromoTile banner={firstBanner(promoTiles[0].position)} config={promoTiles[0]}/>
                    <PromoTile banner={firstBanner(promoTiles[1].position)} config={promoTiles[1]}/>
                </div>
                <div className="flex h-full flex-col">
                    <PromoTile banner={firstBanner(promoTiles[2].position)} config={promoTiles[2]}/>
                    <PromoTile banner={firstBanner(promoTiles[3].position)} config={promoTiles[3]}/>
                </div>
            </section>

            <SectionWithBanner
                title={readConfig(configMap, 'home_section_04_title', 'Hotwheels')}
                subtitle={readConfig(configMap, 'home_section_04_subtitle', 'Giao Hang Toan Quoc', true)}
                bannerImg={DEFAULT_HOTWHEELS_BANNER}
                products={mappedProducts}
                banners={bannersFor('home_section_03_banner')}
                ctaLabel={readConfig(configMap, 'home_section_04_cta_label', 'Xem them', true)}
                ctaUrl={readConfig(configMap, 'home_section_04_cta_url', '/products')}
            />

            <SectionWithBanner
                title={readConfig(configMap, 'home_section_05_title', 'Giam gia cuc manh')}
                subtitle={readConfig(configMap, 'home_section_05_subtitle', '', true)}
                bannerImg={DEFAULT_SALE_BANNER}
                products={mappedProducts}
                banners={bannersFor('home_section_04_banner')}
                ctaLabel={readConfig(configMap, 'home_section_05_cta_label', 'Xem them', true)}
                ctaUrl={readConfig(configMap, 'home_section_05_cta_url', '/products')}
            />

            <section className="mb-10 w-full px-3 sm:mb-20 sm:px-6">
                <h2 className="mb-5 text-xl font-black uppercase text-on-surface sm:mb-8 sm:text-2xl">
                    {readConfig(configMap, 'home_news_title', 'Tin Tuc')}
                </h2>
                <div className="grid grid-cols-1 gap-4 sm:gap-6 md:grid-cols-4">
                    {mockBlogs.slice(0, 4).map((blog) => (
                        <Link
                            to={`/blog/${blog.id}`}
                            key={blog.id}
                            className="group flex flex-col overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm transition-all hover:shadow-md sm:rounded-2xl"
                        >
                            <div className="aspect-[16/10] w-full overflow-hidden bg-surface-container-low">
                                <img src={blog.image} className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105" alt={blog.title}/>
                            </div>
                            <div className="flex flex-1 flex-col justify-between p-4 sm:p-5">
                                <div>
                                    <h3 className="mb-2 line-clamp-2 text-base font-bold text-on-surface transition-colors group-hover:text-primary">
                                        {blog.title}
                                    </h3>
                                    <p className="mb-4 line-clamp-3 text-xs text-outline">
                                        {blog.excerpt || blog.content}
                                    </p>
                                </div>
                                <span className="text-[10px] font-bold uppercase tracking-wider text-outline/70">
                                    {blog.date}
                                </span>
                            </div>
                        </Link>
                    ))}
                </div>
                <div className="mt-6 flex justify-end">
                    <Link
                        to={readConfig(configMap, 'home_news_more_url', '/blog')}
                        className="rounded-full bg-outline px-5 py-2 text-xs font-bold uppercase text-surface transition-all hover:bg-primary sm:px-6"
                    >
                        {readConfig(configMap, 'home_news_more_label', 'MORE')}
                    </Link>
                </div>
            </section>

            <section className="relative mb-14 w-full overflow-hidden border-y border-outline-variant/10 bg-surface-container-low py-10 sm:mb-20 sm:py-16">
                <div className="relative z-10 mx-auto w-full px-4 text-center sm:px-6">
                    <h2 className="mb-10 text-xs font-black uppercase tracking-[0.3em] text-outline/80">
                        {readConfig(configMap, 'home_partners_title', 'Doi tac chien luoc & Thuong hieu dong hanh')}
                    </h2>
                    <div className="grid grid-cols-2 items-center justify-items-center gap-3 sm:grid-cols-3 sm:gap-6 lg:grid-cols-6">
                        {partnerBrands.map((brand) => (
                            <div
                                key={`${brand.name}-${brand.logoUrl}`}
                                className="group flex h-12 w-full max-w-[150px] cursor-pointer items-center justify-center rounded-xl border border-outline-variant/20 bg-surface-container-lowest/70 p-2 opacity-75 shadow-sm backdrop-blur-xs transition-all duration-300 hover:bg-surface-container-lowest hover:opacity-100 sm:h-14 sm:max-w-[160px]"
                            >
                                <img
                                    src={brand.logoUrl}
                                    alt={brand.name}
                                    className="max-h-full max-w-full rounded-md object-contain grayscale transition-all duration-300 group-hover:grayscale-0"
                                />
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            <section className="mx-auto w-full px-3 text-center sm:px-6">
                <h2 className="mb-6 text-2xl font-black uppercase text-on-surface sm:mb-12 sm:text-3xl">
                    {readConfig(configMap, 'home_testimonials_title', 'Danh gia tu khach hang')}
                </h2>
                <div className="mb-6 grid grid-cols-1 gap-4 sm:gap-6 md:grid-cols-3">
                    {[1, 2, 3, 4, 5, 6].map((index) => (
                        <div key={index} className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-4 text-left shadow-sm sm:rounded-2xl sm:p-6">
                            <div className="mb-3 flex gap-1 text-[#FFB800]">
                                <Star className="h-4 w-4 fill-current"/>
                                <Star className="h-4 w-4 fill-current"/>
                                <Star className="h-4 w-4 fill-current"/>
                                <Star className="h-4 w-4 fill-current"/>
                                <Star className="h-4 w-4 fill-current"/>
                            </div>
                            <p className="mb-6 line-clamp-3 text-sm font-medium text-on-surface-variant">
                                "The flowers are beautiful and super fresh. They arrived in bud form and are even more beautiful now that they've bloomed."
                            </p>
                            <p className="text-sm font-bold text-on-surface">Customer {index}</p>
                            <p className="text-xs text-outline">Student</p>
                        </div>
                    ))}
                </div>
            </section>
        </main>
    );
}
