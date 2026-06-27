import {useState, useEffect} from 'react';
import {Link} from 'react-router-dom';
import {ChevronLeft, ChevronRight, Star} from 'lucide-react';
import {placeholderImages, mockBlogs, HERO_SLIDES} from '../data/mockData';
import ProductSlider from '../components/common/ProductSlider';
import {useProductStore} from '../store/useProductStore';
import {mapListItemToProductModel} from '../interface/product.model';
import {BannerDTO} from '../interface/public-ui-config.model';
import {getBannersForPlacement, usePublicUiStore} from '../store/usePublicUiStore';
import {useResponsiveDeviceType} from '../hooks/useResponsiveDeviceType';
import BannerMedia from '../components/common/BannerMedia';
import BannerCarousel from '../components/common/BannerCarousel';

const SectionHeader = ({title, subtitle}: { title: string, subtitle?: string }) => (
    <div className="mb-4 flex flex-wrap items-center gap-2 sm:mb-6 sm:gap-4">
        <h2 className="text-lg font-black uppercase text-on-surface sm:text-2xl md:text-3xl">
            {title}
        </h2>
        {subtitle && (
            <>
                <div className="w-[2px] h-6 bg-on-surface"></div>
                <span className="text-xs font-medium text-on-surface sm:whitespace-nowrap sm:text-sm">{subtitle}</span>
            </>
        )}
        <div className="flex-1 h-[1px] bg-on-surface/30 ml-4"></div>
    </div>
);

const SectionWithBanner = ({title, subtitle, bannerImg, products, banners = []}: { title: string, subtitle?: string, bannerImg: string, products: any[], banners?: BannerDTO[] }) => (
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
        <div className="mt-5 flex justify-center sm:mt-8">
            <Link to="/products"
                  className="rounded-full border border-on-surface px-6 py-2 text-xs font-bold uppercase text-on-surface transition-all hover:bg-on-surface hover:text-surface sm:px-8 sm:text-sm">
                Xem thêm
            </Link>
        </div>
    </section>
);



export default function HomePage() {
    const [current, setCurrent] = useState(0);
    const { products, fetchProducts } = useProductStore();
    const banners = usePublicUiStore((state) => state.banners);
    const deviceType = useResponsiveDeviceType();
    const topBanners = getBannersForPlacement(banners, 'HOME_TOP', deviceType);
    const middleBanners = getBannersForPlacement(banners, 'HOME_MIDDLE', deviceType);
    const heroSlideCount = topBanners.length || HERO_SLIDES.length;

    useEffect(() => {
        fetchProducts();
    }, [fetchProducts]);

    const mappedProducts = products.map(mapListItemToProductModel);

    const nextSlide = () => {
        setCurrent((prev) => (prev === heroSlideCount - 1 ? 0 : prev + 1));
    };

    const prevSlide = () => {
        setCurrent((prev) => (prev === 0 ? heroSlideCount - 1 : prev - 1));
    };

    // Tự động chạy slide sau mỗi 5 giây
    useEffect(() => {
        setCurrent(0);
    }, [heroSlideCount]);

    useEffect(() => {
        const slideTimer = window.setInterval(() => {
            setCurrent((prev) => (prev + 1) % heroSlideCount);
        }, 5000);
        return () => clearInterval(slideTimer);
    }, [heroSlideCount]);

    return (
        <main className="w-full min-w-0 space-y-10 bg-surface pb-16 pt-24 sm:space-y-20 sm:pb-24">
            <section
                className="group/hero relative mx-4 flex h-[300px] items-center justify-center sm:mx-6 sm:h-[400px] md:h-[500px]">
                <div className="absolute left-[5%] top-8 bottom-8 w-[80%] bg-outline-variant/20 rounded-3xl z-0"></div>
                <div className="absolute right-[5%] top-8 bottom-8 w-[80%] bg-outline-variant/30 rounded-3xl z-0"></div>
                <div
                    className="relative z-10 w-[90%] h-full rounded-3xl overflow-hidden shadow-2xl flex items-center justify-center">
                    {(topBanners.length ? topBanners : HERO_SLIDES).map((slide, index) => (
                        <div
                            key={index}
                            className={`absolute inset-0 w-full h-full transition-all duration-1000 ease-in-out ${
                                index === current ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
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
                                    className={`absolute inset-0 w-full h-full object-cover mix-blend-overlay transition-transform duration-[2000ms] ease-out ${
                                        index === current ? 'opacity-80 scale-100' : 'opacity-0 scale-105'
                                    }`}
                                    alt={slide.title}
                                />
                            )}
                            <div
                                className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/30 to-black/20"></div>
                            {/*<div*/}
                            {/*    className="absolute inset-0 flex flex-col items-center justify-center text-center px-4">*/}
                            {/*    <h1 className={`text-4xl md:text-7xl font-black text-white tracking-widest uppercase transition-all duration-700 delay-300 transform ${*/}
                            {/*        index === current ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'*/}
                            {/*    }`}>*/}
                            {/*        {slide.title}*/}
                            {/*    </h1>*/}
                            {/*    {'subtitle' in slide && slide.subtitle && (*/}
                            {/*        <p className={`mt-4 text-xs md:text-sm font-bold text-white/80 tracking-widest uppercase transition-all duration-700 delay-500 transform ${*/}
                            {/*            index === current ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'*/}
                            {/*        }`}>*/}
                            {/*            {slide.subtitle}*/}
                            {/*        </p>*/}
                            {/*    )}*/}
                            {/*</div>*/}
                        </div>
                    ))}
                </div>
                <div
                    className="absolute -bottom-8 left-1/2 -translate-x-1/2 flex items-center gap-3 bg-surface px-4 py-2 rounded-full shadow-xs border border-outline-variant/20">
                    <ChevronLeft
                        className="w-4 h-4 cursor-pointer text-on-surface/60 hover:text-primary hover:scale-12 transition-all"
                        onClick={prevSlide}
                    />
                    {(topBanners.length ? topBanners : HERO_SLIDES).map((slide, index) => (
                        <button
                            key={'id' in slide ? slide.id : index}
                            onClick={() => setCurrent(index)}
                            className={`h-2 rounded-full transition-all duration-300 ${
                                index === current ? 'w-6 bg-primary' : 'w-2 bg-outline/40 hover:bg-outline'
                            }`}
                            aria-label={`Go to slide ${index + 1}`}
                        />
                    ))}

                    <ChevronRight
                        className="w-4 h-4 cursor-pointer text-on-surface/60 hover:text-primary hover:scale-12 transition-all"
                        onClick={nextSlide}
                    />
                </div>
            </section>

            {/* 2. BÁN CHẠY  */}
            <div className="pt-10">
                <SectionWithBanner
                    title="BÁN CHẠY"
                    subtitle="Giao Hàng Toàn Quốc"
                    bannerImg="https://i0.wp.com/www.comicbookrevolution.com/wp-content/uploads/2023/12/transformers-4-previw-banner.jpg"
                    products={mappedProducts}
                    banners={middleBanners}
                />
            </div>

            {/* 3. DỊCH VỤ ĐỘ MÔ HÌNH  */}
            <section
                className="mb-10 grid w-full grid-cols-1 items-center gap-6 px-3 sm:mb-20 sm:px-6 lg:grid-cols-12 lg:gap-12">
                <div className="lg:col-span-5 space-y-6">
                    <h2 className="text-2xl font-black uppercase leading-tight tracking-tight text-on-surface md:text-4xl">
                        DỊCH VỤ ĐỘ MÔ HÌNH SỐ 1 <br/> VIỆT NAM
                    </h2>
                    <div className="flex flex-col items-start gap-2 pt-2 sm:gap-4 sm:pt-4">
                        <span
                            className="rounded-full bg-primary px-4 py-1.5 text-xs font-bold text-on-primary sm:px-6 sm:py-2 sm:text-sm">Độ Led cảm ứng</span>
                        <span className="rounded-full bg-primary px-4 py-1.5 text-xs font-bold text-on-primary sm:px-6 sm:py-2 sm:text-sm">Sơn mô hình chuẩn phim</span>
                        <span className="rounded-full bg-primary px-4 py-1.5 text-xs font-bold text-on-primary sm:px-6 sm:py-2 sm:text-sm">Custom theo ý thích</span>
                    </div>
                    <Link to="/services"
                          className="mt-3 inline-block rounded-full border border-on-surface px-6 py-2 text-xs font-black uppercase tracking-widest text-on-surface transition-colors hover:bg-on-surface hover:text-surface sm:mt-4 sm:px-8">
                        CUSTOM NGAY
                    </Link>
                </div>
                <div className="relative h-[240px] sm:h-[400px] lg:col-span-7">
                    <div
                        className="absolute top-0 right-0 w-[55%] h-[60%] bg-surface-container rounded-lg overflow-hidden shadow-lg">
                        <img src={placeholderImages.custom1} className="w-full h-full object-cover" alt="Custom 1"/>
                    </div>
                    <div
                        className="absolute bottom-10 right-[10%] w-[55%] h-[60%] bg-surface-container-high rounded-lg overflow-hidden shadow-xl z-10">
                        <img src={placeholderImages.custom2} className="w-full h-full object-cover" alt="Custom 2"/>
                    </div>
                    <div
                        className="absolute top-20 left-0 w-[45%] h-[50%] bg-surface-container-low rounded-lg overflow-hidden shadow-md">
                        <img
                            src="https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop"
                            className="w-full h-full object-cover opacity-80" alt="Custom 3"/>
                    </div>
                </div>
            </section>

            {/* 4. MÔ HÌNH CUSTOM */}
            <section className="mb-10 w-full px-3 sm:mb-20 sm:px-6">
                <SectionHeader title="MÔ HÌNH CUSTOM" subtitle="Giao Hàng Toàn Quốc"/>
                <ProductSlider products={mappedProducts.slice().reverse()}/>
                <div className="flex justify-center mt-8">
                    <Link to="/products"
                          className="px-8 py-2 border border-on-surface text-on-surface rounded-full font-bold hover:bg-on-surface hover:text-surface transition-all uppercase text-sm">Xem
                        thêm</Link>
                </div>
            </section>

            {/* 5. THỂ LOẠI MÔ HÌNH */}
            <section className="mb-10 w-full px-3 text-center sm:mb-20 sm:px-6">
                <h2 className="mb-6 text-2xl font-black uppercase text-on-surface sm:mb-12 sm:text-3xl">Thể loại mô hình</h2>
                <div className="mb-6 grid grid-cols-2 gap-3 sm:mb-8 sm:gap-6 md:grid-cols-3">
                    {['Marvel', 'DC', 'Hot Wheels', 'Transformer', 'Naruto', 'Pacific Rim'].map((cat) => (
                        <Link to={`/category/${cat.toLowerCase()}`} key={cat}
                              className="group relative h-28 overflow-hidden rounded-xl bg-surface-container shadow-sm transition-all hover:shadow-md sm:h-40 sm:rounded-2xl md:h-56">
                            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent z-10"></div>
                            <img src={placeholderImages.hotwheels}
                                 className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                 alt={cat}/>
                            <div className="absolute inset-0 flex items-end justify-center pb-6 z-20">
                                <h3 className="text-sm font-black text-white sm:text-xl">{cat}</h3>
                            </div>
                        </Link>
                    ))}
                </div>
                <div className="flex items-center justify-center gap-4">
                    <div
                        className="w-10 h-10 rounded-full bg-on-surface text-surface flex items-center justify-center cursor-pointer">
                        <ChevronLeft className="w-5 h-5"/></div>
                    <div
                        className="w-10 h-10 rounded-full bg-on-surface text-surface flex items-center justify-center cursor-pointer">
                        <ChevronRight className="w-5 h-5"/></div>
                </div>
            </section>

            {/* 6. DỤNG CỤ */}
            <SectionWithBanner
                title="Dụng Cụ"
                subtitle="Giao Hàng Toàn Quốc"
                bannerImg="https://tooltechvietnam.com/wp-content/uploads/2023/03/handtools.jpg"
                products={mappedProducts}
            />

            {/* 7. KHỐI PROMO 2x2 */}
            <section className="mb-14 grid grid-cols-2 sm:mb-20">
                <div className="flex flex-col h-full">
                    <div
                        className="relative flex h-36 flex-col items-start justify-center overflow-hidden p-3 text-white min-[390px]:h-40 sm:h-[300px] sm:p-12">
                        <img
                            src="https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg"
                            className="absolute inset-0 w-full h-full object-cover" alt="Hot Wheels 1"/>
                        <div className="absolute inset-0 bg-black/50 z-10"></div>

                        <div className="relative z-20">
                            <h2 className="mb-2 text-sm font-black uppercase leading-tight min-[390px]:text-base sm:mb-4 sm:text-3xl">HOT WHEELS</h2>
                            <p className="mb-3 max-w-sm text-[10px] font-medium leading-snug text-white/85 sm:mb-6 sm:text-sm">KHÁM PHÁ NHỮNG MẪU XE MÔ HÌNH
                                HOT NHẤT DÀNH CHO NGƯỜI ĐAM MÊ TỐC ĐỘ.</p>
                            <button
                                className="border border-white bg-transparent px-3 py-1.5 text-[9px] font-bold uppercase text-white transition-colors hover:bg-white hover:text-black sm:px-8 sm:py-2 sm:text-xs">Xem
                                thêm
                            </button>
                        </div>
                    </div>

                    <div
                        className="relative flex h-36 flex-col items-start justify-end overflow-hidden p-3 text-white min-[390px]:h-40 sm:h-[300px] sm:p-12">
                        <img src="https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg"
                             className="absolute inset-0 w-full h-full object-cover" alt="Hot Wheels 2"/>
                        <div className="absolute inset-0 bg-black/40 z-10"></div>

                        <div className="relative z-20">
                            <p className="max-w-sm text-[10px] font-medium uppercase leading-snug sm:text-sm">SƯU TẦM NHỮNG MẪU XE HUYỀN THOẠI - TỪ
                                SIÊU XE HIỆN ĐẠI ĐẾN CLASSIC CỔ ĐIỂN.</p>
                        </div>
                    </div>
                </div>

                <div className="flex flex-col h-full">
                    <div
                        className="relative flex h-36 flex-col items-start justify-center overflow-hidden p-3 text-white min-[390px]:h-40 sm:h-[300px] sm:p-12">
                        <img src="https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg"
                             className="absolute inset-0 w-full h-full object-cover" alt="Hot Wheels 3"/>
                        <div className="absolute inset-0 bg-black/40 z-10"></div>

                        <div className="relative z-20">
                            <p className="max-w-sm text-[10px] font-medium uppercase leading-snug sm:text-sm">DISCOVER LIMITED EDITION CARS AND
                                EXCLUSIVE RELEASES FOR TRUE COLLECTORS.</p>
                        </div>
                    </div>

                    <div
                        className="relative flex h-36 flex-col items-start justify-center overflow-hidden p-3 text-white min-[390px]:h-40 sm:h-[300px] sm:p-12">
                        <img
                            src="https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg"
                            className="absolute inset-0 w-full h-full object-cover" alt="Hot Wheels 4"/>
                        <div className="absolute inset-0 bg-black/50 z-10"></div>

                        <div className="relative z-20">
                            <h2 className="mb-2 text-sm font-black uppercase leading-tight min-[390px]:text-base sm:mb-4 sm:text-3xl">GIFT FOR COLLECTORS</h2>
                            <p className="mb-3 max-w-sm text-[10px] font-medium uppercase leading-snug text-white/85 sm:mb-6 sm:text-sm">MÓN QUÀ HOÀN HẢO
                                CHO NGƯỜI YÊU XE VÀ ĐAM MÊ MÔ HÌNH.</p>
                            <button
                                className="border border-white bg-white px-3 py-1.5 text-[9px] font-bold uppercase text-black transition-colors hover:bg-transparent hover:text-white sm:px-8 sm:py-2 sm:text-xs">Xem
                                thêm
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            {/* 8. HOTWHEELS */}
            <SectionWithBanner
                title="Hotwheels"
                subtitle="Giao Hàng Toàn Quốc"
                bannerImg="https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1600&auto=format&fit=crop"
                products={mappedProducts}
            />

            <SectionWithBanner
                title="GIảm giá cực mạnh"
                bannerImg="https://img.magnific.com/free-vector/modern-black-friday-holiday-sale-offer-banner-get-30-percent-price-drop-vector_1017-47794.jpg?semt=ais_hybrid&w=740&q=80"
                products={mappedProducts}
            />

            {/* 11. TIN TỨC */}
            <section className="mb-10 w-full px-3 sm:mb-20 sm:px-6">
                <h2 className="mb-5 text-xl font-black uppercase text-on-surface sm:mb-8 sm:text-2xl">Tin Tức</h2>
                <div className="grid grid-cols-1 gap-4 sm:gap-6 md:grid-cols-4">
                    {mockBlogs.slice(0, 4).map(blog => (
                        <Link
                            to={`/blog/${blog.id}`}
                            key={blog.id}
                            className="group flex flex-col overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm transition-all hover:shadow-md sm:rounded-2xl"
                        >
                            <div className="aspect-[16/10] w-full overflow-hidden bg-surface-container-low">
                                <img
                                    src={blog.image}
                                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                    alt={blog.title}
                                />
                            </div>
                            <div className="flex flex-1 flex-col justify-between p-4 sm:p-5">
                                <div>
                                    <h3 className="font-bold text-base text-on-surface group-hover:text-primary mb-2 line-clamp-2 transition-colors">
                                        {blog.title}
                                    </h3>
                                    <p className="text-xs text-outline line-clamp-3 mb-4">
                                        {blog.excerpt || blog.content}
                                    </p>
                                </div>
                                <span className="text-[10px] font-bold text-outline/70 uppercase tracking-wider">
                        {blog.date}
                    </span>
                            </div>
                        </Link>
                    ))}
                </div>
                <div className="flex justify-end mt-6">
                    <Link to="/blog"
                          className="rounded-full bg-outline px-5 py-2 text-xs font-bold uppercase text-surface transition-all hover:bg-primary sm:px-6">MORE
                        →</Link>
                </div>
            </section>

            {/* 12. ĐỐI TÁC CHIẾN LƯỢC */}
            <section
                className="relative mb-14 w-full overflow-hidden border-y border-outline-variant/10 bg-surface-container-low py-10 sm:mb-20 sm:py-16">
                <div
                    className="absolute -top-24 -left-20 w-80 h-80 bg-primary/5 rounded-full blur-3xl pointer-events-none"></div>
                <div
                    className="absolute -bottom-24 -right-20 w-80 h-80 bg-secondary/5 rounded-full blur-3xl pointer-events-none"></div>

                <div className="relative z-10 mx-auto w-full px-4 text-center sm:px-6">
                    <h2 className="text-xs font-black tracking-[0.3em] uppercase text-outline/80 mb-10">
                        Đối tác chiến lược & Thương hiệu đồng hành
                    </h2>
                    <div
                        className="grid grid-cols-2 items-center justify-items-center gap-3 sm:grid-cols-3 sm:gap-6 lg:grid-cols-6">
                        {[
                            {
                                name: 'BANDAI',
                                url: 'https://placehold.co/180x60/e60012/ffffff?text=BANDAI',
                                glow: 'hover:shadow-[0_0_20px_rgba(230,0,18,0.3)]'
                            },
                            {
                                name: 'HOT TOYS',
                                url: 'https://placehold.co/180x60/111111/f1b82d?text=HOT+TOYS',
                                glow: 'hover:shadow-[0_0_20px_rgba(241,184,45,0.25)]'
                            },
                            {
                                name: 'TAMIYA',
                                url: 'https://placehold.co/180x60/0054a6/ffffff?text=TAMIYA',
                                glow: 'hover:shadow-[0_0_20px_rgba(0,84,166,0.3)]'
                            },
                            {
                                name: 'LEGO',
                                url: 'https://placehold.co/180x60/ffd500/000000?text=LEGO',
                                glow: 'hover:shadow-[0_0_20px_rgba(255,213,0,0.4)]'
                            },
                            {
                                name: 'MATTEL',
                                url: 'https://placehold.co/180x60/e5142a/ffffff?text=MATTEL',
                                glow: 'hover:shadow-[0_0_20px_rgba(229,20,42,0.3)]'
                            },
                            {
                                name: 'HASBRO',
                                url: 'https://placehold.co/180x60/0072ce/ffffff?text=HASBRO',
                                glow: 'hover:shadow-[0_0_20px_rgba(0,114,206,0.3)]'
                            },
                        ].map((brand, index) => (
                            <div
                                key={index}
                                className={`group flex h-12 w-full max-w-[150px] cursor-pointer items-center justify-center rounded-xl border border-outline-variant/20 bg-surface-container-lowest/70 p-2 opacity-75 shadow-sm backdrop-blur-xs transition-all duration-300 hover:scale-105 hover:bg-surface-container-lowest hover:opacity-100 sm:h-14 sm:max-w-[160px] ${brand.glow}`}
                            >
                                <img
                                    src={brand.url}
                                    alt={brand.name}
                                    className="max-h-full max-w-full object-contain rounded-md grayscale group-hover:grayscale-0 transition-all duration-300"
                                />
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* 13. ĐÁNH GIÁ */}
            <section className="mx-auto w-full px-3 text-center sm:px-6">
                <h2 className="mb-6 text-2xl font-black uppercase text-on-surface sm:mb-12 sm:text-3xl">Đánh giá từ khách hàng</h2>
                <div className="mb-6 grid grid-cols-1 gap-4 sm:gap-6 md:grid-cols-3">
                    {[1, 2, 3, 4, 5, 6].map(i => (
                        <div key={i}
                             className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-4 text-left shadow-sm sm:rounded-2xl sm:p-6">
                            <div className="flex gap-1 mb-3 text-[#FFB800]">
                                <Star className="w-4 h-4 fill-current"/> <Star className="w-4 h-4 fill-current"/> <Star
                                className="w-4 h-4 fill-current"/> <Star className="w-4 h-4 fill-current"/> <Star
                                className="w-4 h-4 fill-current"/>
                            </div>
                            <p className="font-medium text-on-surface-variant text-sm mb-6 line-clamp-3">"The flowers
                                are beautiful and super fresh. They arrived in bud form and are even more beautiful now
                                that they've bloomed."</p>
                            <p className="font-bold text-on-surface text-sm">Customer {i}</p>
                            <p className="text-xs text-outline">Student</p>
                        </div>
                    ))}
                </div>
                <div className="flex items-center justify-center gap-4">
                    <div
                        className="w-10 h-10 rounded-full bg-on-surface text-surface flex items-center justify-center cursor-pointer">
                        <ChevronLeft className="w-5 h-5"/></div>
                    <div
                        className="w-10 h-10 rounded-full bg-on-surface text-surface flex items-center justify-center cursor-pointer">
                        <ChevronRight className="w-5 h-5"/></div>
                </div>
            </section>
        </main>
    );
}
