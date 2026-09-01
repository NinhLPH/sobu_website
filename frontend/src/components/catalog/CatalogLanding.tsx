import {useEffect, useMemo, useState} from 'react';
import {AlertCircle, ChevronLeft, ChevronRight, Image as ImageIcon, RefreshCw} from 'lucide-react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {BrandDetailDTO} from '../../interface/brand.model';
import {CategoryDetailDTO, CategoryListItemDTO} from '../../interface/category.model';
import {PageResponse} from '../../interface/api-response';
import {mapListItemToProductModel, ProductListItemDTO} from '../../interface/product.model';
import {PublicCatalogService} from '../../service/public-catalog.service';
import {getPublicImageUrl} from '../../utils/file-url';
import {sanitizeRichHtml} from '../../utils/sanitize-html';
import Breadcrumbs from '../common/Breadcrumbs';
import ProductCard from '../common/ProductCard';
import SeoHead from '../common/SeoHead';

type LandingKind = 'category' | 'brand';
type Detail = CategoryDetailDTO | BrandDetailDTO;
const PAGE_SIZE = 12;
const emptyProducts: PageResponse<ProductListItemDTO> = {
    content: [],
    pageNumber: 0,
    pageSize: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false
};

export default function CatalogLanding({kind}: { kind: LandingKind }) {
    const {slugOrId} = useParams();
    const navigate = useNavigate();
    const [detail, setDetail] = useState<Detail | null>(null);
    const [children, setChildren] = useState<CategoryListItemDTO[]>([]);
    const [products, setProducts] = useState(emptyProducts);
    const [page, setPage] = useState(0);
    const [sort, setSort] = useState('createdAt,DESC');
    const [loadingDetail, setLoadingDetail] = useState(true);
    const [loadingProducts, setLoadingProducts] = useState(false);
    const [detailError, setDetailError] = useState('');
    const [productError, setProductError] = useState('');
    const [retry, setRetry] = useState(0);
    const [productRetry, setProductRetry] = useState(0);

    useEffect(() => {
        if (!slugOrId) return;
        let active = true;
        setLoadingDetail(true);
        setDetailError('');
        setProductError('');
        setDetail(null);
        setPage(0);
        const request = kind === 'category' ? PublicCatalogService.getCategoryDetail(slugOrId) : PublicCatalogService.getBrandDetail(slugOrId);
        request.then(async (result) => {
            if (!active) return;
            if (result.status !== 1) throw new Error(`${kind === 'category' ? 'Danh mục' : 'Thương hiệu'} này hiện không hoạt động.`);
            setDetail(result);
            if (result.slug && result.slug !== slugOrId) navigate(`/${kind}/${result.slug}`, {replace: true});
            if (kind === 'category') {
                const categories = await PublicCatalogService.getCategories();
                if (active) setChildren(categories.filter((item) => item.status === 1 && String(item.parentId) === String(result.id)));
            }
        }).catch((loadError) => active && setDetailError(loadError?.response?.data?.message || loadError?.message || 'Không thể tải nội dung.'))
            .finally(() => active && setLoadingDetail(false));
        return () => {
            active = false;
        };
    }, [kind, navigate, retry, slugOrId]);

    useEffect(() => {
        if (!detail) return;
        let active = true;
        const [sortBy, sortDirection] = sort.split(',');
        setLoadingProducts(true);
        setProductError('');
        PublicCatalogService.getProducts({
            page,
            pageSize: PAGE_SIZE,
            sortBy,
            sortDirection, ...(kind === 'category' ? {categoryId: detail.id} : {brandId: detail.id})
        })
            .then((result) => active && setProducts(result))
            .catch((loadError) => active && setProductError(loadError?.response?.data?.message || loadError?.message || 'Không thể tải sản phẩm.'))
            .finally(() => active && setLoadingProducts(false));
        return () => {
            active = false;
        };
    }, [detail, kind, page, productRetry, sort]);

    const isCategory = kind === 'category';
    const category = isCategory ? detail as CategoryDetailDTO | null : null;
    const brand = !isCategory ? detail as BrandDetailDTO | null : null;
    const image = category?.image || brand?.logoUrl;
    const imageAlt = category?.imageAlt || brand?.logoAlt || detail?.name;
    const intro = category?.introContent || category?.content || brand?.description;
    const footer = category?.footerContent;
    const safeIntro = useMemo(() => sanitizeRichHtml(intro), [intro]);
    const safeFooter = useMemo(() => sanitizeRichHtml(footer), [footer]);

    if (loadingDetail) return <main className="min-h-[65vh] bg-surface px-4 pb-24 pt-32 sm:px-6"><SeoHead
        title="Đang tải | SOBU" noIndex/>
        <div className="h-80 animate-pulse rounded-[2rem] bg-surface-container"/>
    </main>;
    if (detailError || !detail) return <main className="min-h-[65vh] bg-surface px-4 pb-24 pt-32 sm:px-6"><SeoHead
        title="Không tìm thấy | SOBU" noIndex/>
        <div className="rounded-2xl border border-error/20 bg-error/10 p-10 text-center"><AlertCircle
            className="mx-auto h-10 w-10 text-error"/><h1 className="mt-4 text-2xl font-black text-on-surface">Không tìm
            thấy {isCategory ? 'danh mục' : 'thương hiệu'}</h1><p
            className="mt-2 text-sm font-semibold text-outline">{detailError}</p>
            <button onClick={() => setRetry((value) => value + 1)}
                    className="mt-6 inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-black text-on-primary">
                <RefreshCw className="h-4 w-4"/>Thử lại
            </button>
        </div>
    </main>;

    const canonicalPath = `/${kind}/${detail.slug || detail.id}`;
    return (
        <main className="w-full min-w-0 bg-surface px-4 pb-20 pt-28 sm:px-6 sm:pb-24 sm:pt-32">
            <SeoHead title={detail.seo?.seoTitle || `${detail.name} | SOBU`}
                     description={detail.seo?.metaDescription || (brand?.description ?? `Khám phá sản phẩm ${detail.name} tại SOBU.`)}
                     canonicalPath={canonicalPath} image={image} metadata={detail.seo}
                     structuredData={[
                         {
                             '@context': 'https://schema.org',
                             '@type': 'CollectionPage',
                             name: detail.name,
                             description: detail.seo?.metaDescription || brand?.description,
                             url: typeof window !== 'undefined' ? new URL(canonicalPath, window.location.origin).toString() : canonicalPath
                         },
                         {
                             '@context': 'https://schema.org', '@type': 'BreadcrumbList', itemListElement: [
                                 {
                                     '@type': 'ListItem',
                                     position: 1,
                                     name: 'Sản phẩm',
                                     item: typeof window === 'undefined' ? '/products' : new URL('/products', window.location.origin).toString()
                                 },
                                 {'@type': 'ListItem', position: 2, name: detail.name},
                             ]
                         },
                     ]}/>
            <Breadcrumbs
                items={[{label: 'Trang chủ', to: '/'}, {label: 'Sản phẩm', to: '/products'}, {label: detail.name}]}/>
            <header
                className="mb-8 grid overflow-hidden rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm md:grid-cols-12 sm:rounded-[2rem]">
                <div className="flex min-h-56 items-center justify-center bg-surface-container-low p-8 md:col-span-5">
                    {image ? <img src={getPublicImageUrl(image)} alt={imageAlt || detail.name}
                                  className="max-h-72 w-full object-contain"/> :
                        <ImageIcon className="h-16 w-16 text-outline/30"/>}
                </div>
                <div className="flex flex-col justify-center p-6 md:col-span-7 sm:p-10">
                    <p className="mb-3 text-[10px] font-black uppercase tracking-[0.22em] text-primary">{isCategory ? 'Danh mục sản phẩm' : 'Thương hiệu'}</p>
                    <h1 className="text-3xl font-black uppercase tracking-tight text-on-surface sm:text-5xl">{detail.name}</h1>
                    {safeIntro && <div className="static-page-content mt-5 text-sm"
                                       dangerouslySetInnerHTML={{__html: safeIntro}}/>}
                </div>
            </header>

            {isCategory && children.length > 0 && <section className="mb-8"><h2
                className="mb-4 text-sm font-black uppercase tracking-widest text-on-surface">Khám phá danh mục con</h2>
                <div className="flex flex-wrap gap-2">{children.map((child) => <Link key={child.id}
                                                                                     to={`/category/${child.slug || child.id}`}
                                                                                     className="rounded-full border border-outline-variant/30 bg-surface-container-lowest px-4 py-2 text-xs font-bold text-on-surface transition-colors hover:border-primary/30 hover:text-primary">{child.name}</Link>)}</div>
            </section>}

            <section aria-labelledby="landing-products-title">
                <div
                    className="mb-6 flex flex-wrap items-center justify-between gap-3 border-b border-outline-variant/20 pb-5">
                    <div><h2 id="landing-products-title" className="text-2xl font-black uppercase text-on-surface">Sản
                        phẩm</h2><p className="mt-1 text-xs font-bold text-outline">{products.totalElements} sản
                        phẩm</p></div>
                    <select value={sort} onChange={(event) => {
                        setSort(event.target.value);
                        setPage(0);
                    }} aria-label="Sắp xếp sản phẩm"
                            className="cursor-pointer rounded-full bg-surface-container px-4 py-2.5 text-xs font-bold text-on-surface outline-none focus-visible:ring-2 focus-visible:ring-primary/30">
                        <option value="createdAt,DESC">Mới nhất</option>
                        <option value="price,ASC">Giá tăng dần</option>
                        <option value="price,DESC">Giá giảm dần</option>
                        <option value="discountPercent,DESC">Giảm giá nhiều nhất</option>
                    </select></div>
                {loadingProducts ? <div className="grid grid-cols-2 gap-3 sm:gap-6 lg:grid-cols-3">
                    <div className="h-80 animate-pulse rounded-2xl bg-surface-container"/>
                    <div className="h-80 animate-pulse rounded-2xl bg-surface-container"/>
                    <div className="hidden h-80 animate-pulse rounded-2xl bg-surface-container lg:block"/>
                </div> : productError ?
                    <div className="rounded-2xl border border-error/20 bg-error/10 p-8 text-center"><p
                        className="text-sm font-bold text-error">{productError}</p>
                        <button onClick={() => setProductRetry(value => value + 1)}
                                className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-black text-on-primary">
                            <RefreshCw className="h-4 w-4"/>Thử lại
                        </button>
                    </div> : products.content.length ? <div
                            className="grid grid-cols-2 gap-3 sm:gap-6 lg:grid-cols-3">{products.content.map(mapListItemToProductModel).map((product) =>
                            <ProductCard key={product.id} product={product}/>)}</div> :
                        <div className="rounded-2xl border-2 border-dashed border-outline-variant/30 p-12 text-center">
                            <p className="font-black text-on-surface">Chưa có sản phẩm
                                trong {isCategory ? 'danh mục' : 'thương hiệu'} này.</p></div>}
                {products.totalPages > 1 && <nav aria-label="Phân trang sản phẩm"
                                                 className="mt-8 flex items-center justify-between border-t border-outline-variant/20 pt-5">
                    <span
                        className="text-xs font-bold text-outline">Trang {products.pageNumber + 1}/{products.totalPages}</span>
                    <div className="flex gap-2">
                        <button disabled={!products.hasPrevious || loadingProducts}
                                onClick={() => setPage((value) => Math.max(0, value - 1))} aria-label="Trang trước"
                                className="cursor-pointer rounded-xl bg-surface-container p-2.5 disabled:cursor-not-allowed disabled:opacity-40">
                            <ChevronLeft className="h-4 w-4"/></button>
                        <button disabled={!products.hasNext || loadingProducts}
                                onClick={() => setPage((value) => value + 1)} aria-label="Trang sau"
                                className="cursor-pointer rounded-xl bg-surface-container p-2.5 disabled:cursor-not-allowed disabled:opacity-40">
                            <ChevronRight className="h-4 w-4"/></button>
                    </div>
                </nav>}
            </section>
            {safeFooter && <section className="mt-12 border-t border-outline-variant/20 pt-8">
                <div className="static-page-content" dangerouslySetInnerHTML={{__html: safeFooter}}/>
            </section>}
        </main>
    );
}
