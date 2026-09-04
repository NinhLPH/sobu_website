import {useEffect, useMemo, useState} from 'react';
import {AlertCircle, CalendarDays, ChevronLeft, RefreshCw, UserRound} from 'lucide-react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import Breadcrumbs from '../components/common/Breadcrumbs';
import SeoHead from '../components/common/SeoHead';
import {ArticleDetailDTO} from '../interface/article.model';
import {ArticleService} from '../service/article.service';
import {getPublicImageUrl} from '../utils/file-url';
import {sanitizeRichHtml} from '../utils/sanitize-html';

const formatDate = (value?: string | null) => value
    ? new Intl.DateTimeFormat('vi-VN', {day: '2-digit', month: 'long', year: 'numeric'}).format(new Date(value))
    : 'Đang cập nhật';

export default function BlogDetail() {
    const {slugOrId} = useParams();
    const navigate = useNavigate();
    const [article, setArticle] = useState<ArticleDetailDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [retry, setRetry] = useState(0);

    useEffect(() => {
        if (!slugOrId) return;
        let active = true;
        setLoading(true);
        setError('');
        setArticle(null);
        ArticleService.getPublishedArticle(slugOrId)
            .then((result) => {
                if (!active) return;
                setArticle(result);
                if (result.slug && result.slug !== slugOrId) navigate(`/blog/${result.slug}`, {replace: true});
            })
            .catch((loadError) => active && setError(loadError?.response?.data?.message || loadError?.message || 'Không tìm thấy bài viết.'))
            .finally(() => active && setLoading(false));
        return () => {
            active = false;
        };
    }, [navigate, retry, slugOrId]);

    const safeContent = useMemo(() => sanitizeRichHtml(article?.content), [article?.content]);
    if (loading) return <main className="min-h-[60vh] bg-surface px-4 pb-24 pt-32 sm:px-6"><SeoHead
        title="Đang tải bài viết | SOBU" noIndex/>
        <div className="mx-auto h-96 max-w-4xl animate-pulse rounded-[2rem] bg-surface-container"/>
    </main>;
    if (error || !article) return <main className="min-h-[60vh] bg-surface px-4 pb-24 pt-32 sm:px-6"><SeoHead
        title="Không tìm thấy bài viết | SOBU" noIndex/>
        <div className="mx-auto max-w-3xl rounded-2xl border border-error/20 bg-error/10 p-10 text-center"><AlertCircle
            className="mx-auto h-10 w-10 text-error"/><h1 className="mt-4 text-2xl font-black text-on-surface">Không tìm
            thấy bài viết</h1><p className="mt-2 text-sm font-semibold text-outline">{error}</p>
            <button onClick={() => setRetry((value) => value + 1)}
                    className="mt-6 inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-black text-on-primary">
                <RefreshCw className="h-4 w-4"/>Thử lại
            </button>
        </div>
    </main>;

    const imageUrl = article.thumbnailUrl ? getPublicImageUrl(article.thumbnailUrl) : undefined;
    const structuredData = [{
        '@context': 'https://schema.org',
        '@type': 'Article',
        headline: article.title,
        description: article.seo?.metaDescription || article.metaDescription || article.excerpt,
        image: imageUrl ? [imageUrl] : undefined,
        datePublished: article.publishedAt,
        dateModified: article.updatedAt,
        author: article.authorName ? {'@type': 'Person', name: article.authorName} : undefined,
    }, {
        '@context': 'https://schema.org', '@type': 'BreadcrumbList', itemListElement: [
            {
                '@type': 'ListItem',
                position: 1,
                name: 'Tin tức',
                item: typeof window === 'undefined' ? '/blog' : new URL('/blog', window.location.origin).toString()
            },
            {'@type': 'ListItem', position: 2, name: article.title},
        ],
    }];

    return (
        <main className="w-full min-w-0 bg-surface px-4 pb-20 pt-28 sm:px-6 sm:pb-24 sm:pt-32">
            <SeoHead title={article.seoTitle || article.title} description={article.metaDescription || article.excerpt}
                     canonicalPath={`/blog/${article.slug}`} image={article.thumbnailUrl} type="article"
                     metadata={article.seo} structuredData={structuredData}/>
            <Breadcrumbs
                items={[{label: 'Trang chủ', to: '/'}, {label: 'Tin tức', to: '/blog'}, {label: article.title}]}/>
            <div className="w-full min-w-0">
                <Link to="/blog"
                      className="mb-8 inline-flex items-center gap-2 text-xs font-black uppercase tracking-widest text-outline transition-colors hover:text-primary"><ChevronLeft
                    className="h-4 w-4"/>Quay lại Tin tức</Link>
                <article>
                    <header className="mb-8 border-b border-outline-variant/20 pb-8 sm:mb-10 sm:pb-10">
                        {article.category && <Link to={`/blog?category=${encodeURIComponent(article.category)}`}
                                                   className="mb-4 inline-block rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">{article.category}</Link>}
                        <h1 className="text-3xl font-black leading-tight text-on-surface sm:text-5xl">{article.title}</h1>
                        <div className="mt-5 flex flex-wrap items-center gap-4 text-xs font-bold text-outline"><span
                            className="inline-flex items-center gap-1.5"><CalendarDays
                            className="h-4 w-4"/>{formatDate(article.publishedAt)}</span>{article.authorName &&
                            <span className="inline-flex items-center gap-1.5"><UserRound
                                className="h-4 w-4"/>{article.authorName}</span>}</div>
                    </header>
                    {imageUrl && <figure
                        className="mb-10 overflow-hidden rounded-2xl bg-surface-container-low shadow-sm sm:rounded-[2rem]">
                        <img src={imageUrl} alt={article.thumbnailAlt || article.title}
                             className="aspect-[16/9] w-full object-cover"/></figure>}
                    {article.excerpt &&
                        <p className="mb-8 text-lg font-bold leading-relaxed text-on-surface sm:text-xl">{article.excerpt}</p>}
                    {safeContent ? <div className="static-page-content article-content"
                                        dangerouslySetInnerHTML={{__html: safeContent}}/> : <div
                        className="rounded-2xl border border-dashed border-outline-variant/40 p-8 text-center text-sm font-bold text-outline">Nội
                        dung đang được cập nhật.</div>}
                </article>
            </div>
        </main>
    );
}
