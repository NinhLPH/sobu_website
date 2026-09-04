import {useEffect, useState} from 'react';
import {ArrowRight, CalendarDays, ChevronLeft, ChevronRight, FileText, RefreshCw} from 'lucide-react';
import {Link, useSearchParams} from 'react-router-dom';
import Breadcrumbs from '../components/common/Breadcrumbs';
import SeoHead from '../components/common/SeoHead';
import {ArticleDTO} from '../interface/article.model';
import {PageResponse} from '../interface/api-response';
import {ArticleService} from '../service/article.service';
import {getPublicImageUrl} from '../utils/file-url';

const PAGE_SIZE = 9;
const emptyPage: PageResponse<ArticleDTO> = {
    content: [], pageNumber: 0, pageSize: PAGE_SIZE, totalElements: 0, totalPages: 0,
    first: true, last: true, hasNext: false, hasPrevious: false,
};

const formatDate = (value?: string | null) => value
    ? new Intl.DateTimeFormat('vi-VN', {day: '2-digit', month: 'long', year: 'numeric'}).format(new Date(value))
    : 'Đang cập nhật';

function ArticleCard({article}: { article: ArticleDTO }) {
    return (
        <article
            className="group flex h-full flex-col overflow-hidden rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm transition-colors hover:border-primary/25 sm:rounded-[2rem]">
            <Link to={`/blog/${article.slug}`}
                  className="block aspect-[16/10] overflow-hidden bg-surface-container-low">
                {article.thumbnailUrl ? (
                    <img src={getPublicImageUrl(article.thumbnailUrl)} alt={article.thumbnailAlt || article.title}
                         className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"/>
                ) : <div className="flex h-full items-center justify-center"><FileText
                    className="h-10 w-10 text-outline/40"/></div>}
            </Link>
            <div className="flex flex-1 flex-col p-5 sm:p-6">
                <div
                    className="mb-3 flex flex-wrap items-center gap-2 text-[10px] font-black uppercase tracking-widest text-outline">
                    {article.category && <Link to={`/blog?category=${encodeURIComponent(article.category)}`}
                                               className="text-primary hover:underline">{article.category}</Link>}
                    <span className="inline-flex items-center gap-1"><CalendarDays
                        className="h-3.5 w-3.5"/>{formatDate(article.publishedAt)}</span>
                </div>
                <h2 className="mb-3 text-xl font-black leading-tight text-on-surface transition-colors group-hover:text-primary">
                    <Link to={`/blog/${article.slug}`}>{article.title}</Link>
                </h2>
                <p className="mb-5 line-clamp-3 text-sm font-medium leading-relaxed text-on-surface-variant">{article.excerpt || 'Khám phá nội dung mới nhất từ SOBU.'}</p>
                <Link to={`/blog/${article.slug}`}
                      className="mt-auto inline-flex w-fit items-center gap-2 text-xs font-black uppercase tracking-widest text-primary">
                    Đọc bài viết <ArrowRight className="h-4 w-4"/>
                </Link>
            </div>
        </article>
    );
}

export default function BlogList() {
    const [searchParams, setSearchParams] = useSearchParams();
    const page = Math.max(0, Number(searchParams.get('page') || 1) - 1);
    const category = searchParams.get('category')?.trim() || undefined;
    const [pageData, setPageData] = useState<PageResponse<ArticleDTO>>(emptyPage);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [retry, setRetry] = useState(0);

    useEffect(() => {
        let active = true;
        setLoading(true);
        setError('');
        ArticleService.getPublishedArticles({page, size: PAGE_SIZE, category})
            .then((result) => active && setPageData(result))
            .catch((loadError) => active && setError(loadError?.response?.data?.message || loadError?.message || 'Không thể tải bài viết.'))
            .finally(() => active && setLoading(false));
        return () => {
            active = false;
        };
    }, [category, page, retry]);

    const featured = page === 0 ? pageData.content[0] : undefined;
    const articles = featured ? pageData.content.slice(1) : pageData.content;
    const changePage = (nextPage: number) => {
        const next = new URLSearchParams(searchParams);
        next.set('page', String(nextPage + 1));
        setSearchParams(next);
    };

    return (
        <main className="w-full min-w-0 bg-surface px-4 pb-20 pt-28 sm:px-6 sm:pb-24 sm:pt-32">
            <SeoHead title={category ? `${category} | Tin tức SOBU` : 'Tin tức & Góc sưu tầm | SOBU'}
                     description={category ? `Bài viết mới nhất thuộc chủ đề ${category} tại SOBU.` : 'Tin tức, kinh nghiệm sưu tầm và câu chuyện dành cho cộng đồng collector tại SOBU.'}
                     canonicalPath={category ? `/blog?category=${encodeURIComponent(category)}` : '/blog'}
                     structuredData={{
                         '@context': 'https://schema.org',
                         '@type': 'CollectionPage',
                         name: category ? `Tin tức ${category}` : 'SOBU Blog'
                     }}/>
            <Breadcrumbs items={[{label: 'Trang chủ', to: '/'}, {label: 'Tin tức'}]}/>
            <header className="mb-8 border-b border-outline-variant/20 pb-8 sm:mb-12 sm:pb-10">
                <p className="mb-3 text-xs font-black uppercase tracking-[0.24em] text-primary">SOBU Editorial</p>
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <div><h1
                        className="text-3xl font-black uppercase tracking-tight text-on-surface sm:text-5xl">{category || 'Tin tức & Góc sưu tầm'}</h1>
                        <p className="mt-3 max-w-2xl text-sm font-medium leading-relaxed text-on-surface-variant sm:text-base">Cập
                            nhật xu hướng, kiến thức và những câu chuyện thú vị trong thế giới mô hình.</p></div>
                    {category && <Link to="/blog"
                                       className="rounded-full border border-outline-variant/40 px-4 py-2 text-xs font-black uppercase text-primary transition-colors hover:bg-primary/10">Xem
                        tất cả</Link>}
                </div>
            </header>
            {loading ? (
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3"
                     aria-label="Đang tải bài viết">{Array.from({length: 6}).map((_, index) => <div key={index}
                                                                                                    className="h-[390px] animate-pulse rounded-[2rem] bg-surface-container"/>)}</div>
            ) : error ? (
                <div className="rounded-2xl border border-error/20 bg-error/10 p-10 text-center"><p
                    className="font-bold text-error">{error}</p>
                    <button onClick={() => setRetry((value) => value + 1)}
                            className="mt-5 inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-black text-on-primary">
                        <RefreshCw className="h-4 w-4"/>Thử lại
                    </button>
                </div>
            ) : !pageData.content.length ? (
                <div
                    className="rounded-2xl border-2 border-dashed border-outline-variant/30 bg-surface-container-lowest p-12 text-center">
                    <FileText className="mx-auto mb-3 h-10 w-10 text-outline/40"/><h2
                    className="text-xl font-black text-on-surface">Chưa có bài viết</h2></div>
            ) : (
                <>
                    {featured && <article
                        className="mb-8 grid overflow-hidden rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm md:grid-cols-12 sm:rounded-[2rem]">
                        <Link to={`/blog/${featured.slug}`}
                              className="min-h-64 overflow-hidden bg-surface-container-low md:col-span-7">{featured.thumbnailUrl ?
                            <img src={getPublicImageUrl(featured.thumbnailUrl)}
                                 alt={featured.thumbnailAlt || featured.title}
                                 className="h-full w-full object-cover"/> :
                            <div className="flex h-full items-center justify-center"><FileText
                                className="h-12 w-12 text-outline/40"/></div>}</Link>
                        <div className="flex flex-col justify-center p-6 md:col-span-5 sm:p-9"><p
                            className="mb-3 text-[10px] font-black uppercase tracking-[0.2em] text-primary">Bài viết nổi
                            bật</p><h2 className="text-2xl font-black leading-tight text-on-surface sm:text-3xl"><Link
                            to={`/blog/${featured.slug}`}>{featured.title}</Link></h2><p
                            className="mt-4 line-clamp-4 text-sm font-medium leading-relaxed text-on-surface-variant">{featured.excerpt}</p>
                            <Link to={`/blog/${featured.slug}`}
                                  className="mt-6 inline-flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary">Đọc
                                ngay <ArrowRight className="h-4 w-4"/></Link></div>
                    </article>}
                    {!!articles.length &&
                        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">{articles.map((article) =>
                            <ArticleCard key={article.id} article={article}/>)}</div>}
                    {pageData.totalPages > 1 && <nav aria-label="Phân trang bài viết"
                                                     className="mt-10 flex items-center justify-between border-t border-outline-variant/20 pt-6">
                        <span
                            className="text-xs font-bold text-outline">Trang {pageData.pageNumber + 1}/{pageData.totalPages}</span>
                        <div className="flex gap-2">
                            <button onClick={() => changePage(page - 1)} disabled={!pageData.hasPrevious}
                                    aria-label="Trang trước"
                                    className="cursor-pointer rounded-xl bg-surface-container p-2.5 text-on-surface disabled:cursor-not-allowed disabled:opacity-40">
                                <ChevronLeft className="h-4 w-4"/></button>
                            <button onClick={() => changePage(page + 1)} disabled={!pageData.hasNext}
                                    aria-label="Trang sau"
                                    className="cursor-pointer rounded-xl bg-surface-container p-2.5 text-on-surface disabled:cursor-not-allowed disabled:opacity-40">
                                <ChevronRight className="h-4 w-4"/></button>
                        </div>
                    </nav>}
                </>
            )}
        </main>
    );
}
