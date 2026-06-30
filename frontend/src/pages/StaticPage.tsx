import {useEffect, useState} from 'react';
import {AlertCircle, FileText, Loader2} from 'lucide-react';
import {StaticPageDTO} from '../interface/static-page.model';
import {StaticPageService} from '../service/static-page.service';

interface StaticPageProps {
    slug: string;
}

const errorMessage = (error: any) =>
    error?.response?.data?.message || error?.message || 'Không thể tải nội dung trang.';

const pageShellClass = 'min-h-[60vh] bg-background px-4 pb-24 pt-32 text-on-surface sm:px-6 sm:pt-36 lg:px-8 lg:pt-40';

export default function StaticPage({slug}: StaticPageProps) {
    const [page, setPage] = useState<StaticPageDTO | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let active = true;
        setLoading(true);
        setError(null);
        setPage(null);

        StaticPageService.getPublishedPageBySlug(slug)
            .then((result) => {
                if (active) setPage(result);
            })
            .catch((loadError) => {
                if (active) setError(errorMessage(loadError));
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, [slug]);

    if (loading) {
        return (
            <main className={pageShellClass}>
                <div className="mx-auto flex w-full max-w-4xl min-w-0 items-center justify-center py-24">
                    <Loader2 className="h-9 w-9 animate-spin text-primary"/>
                </div>
            </main>
        );
    }

    if (error || !page) {
        return (
            <main className={pageShellClass}>
                <div className="mx-auto w-full max-w-4xl min-w-0 rounded-2xl border border-error/20 bg-error/10 p-8 text-center">
                    <AlertCircle className="mx-auto mb-3 h-10 w-10 text-error"/>
                    <h1 className="text-xl font-black text-on-surface">Không tìm thấy trang</h1>
                    <p className="mt-2 text-sm font-semibold text-outline">{error || 'Trang này chưa sẵn sàng.'}</p>
                </div>
            </main>
        );
    }

    const hasContent = Boolean(page.htmlContent?.trim());

    return (
        <main className={pageShellClass}>
            <article className="mx-auto w-full max-w-4xl min-w-0">
                <header className="mb-8 border-b border-outline-variant/30 pb-6">
                    <p className="mb-3 text-xs font-black uppercase tracking-widest text-primary">SOBU</p>
                    <h1 className="text-3xl font-black leading-tight text-on-surface sm:text-4xl">
                        {page.title}
                    </h1>
                </header>

                {hasContent ? (
                    <div
                        className="static-page-content min-w-0 max-w-full"
                        dangerouslySetInnerHTML={{__html: page.htmlContent}}
                    />
                ) : (
                    <div className="rounded-2xl border border-dashed border-outline-variant/50 bg-surface-container-lowest p-10 text-center">
                        <FileText className="mx-auto mb-3 h-10 w-10 text-outline"/>
                        <h2 className="text-lg font-black text-on-surface">Nội dung đang được cập nhật</h2>
                        <p className="mt-2 text-sm font-semibold text-outline">
                            Vui lòng quay lại sau ít phút.
                        </p>
                    </div>
                )}
            </article>
        </main>
    );
}
