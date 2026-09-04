// @ts-ignore
import 'react-quill-new/dist/quill.snow.css';
import {FormEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import ReactQuill from 'react-quill-new';
import {AlertCircle, ChevronLeft, ChevronRight, Edit, FileText, Loader2, Plus, Save, Trash2, X} from 'lucide-react';
import {ArticleDTO, ArticleDetailDTO, ArticleMutationPayload} from '../../interface/article.model';
import {PageResponse} from '../../interface/api-response';
import {ArticleService} from '../../service/article.service';
import {ToastService} from '../../service/toast.service';
import {useConfirmDialog} from '../../components/common/ConfirmDialog';
import ImageUploader from '../../components/common/ImageUploader';
import {getPublicImageUrl} from '../../utils/file-url';

const PAGE_SIZE = 10;
const emptyPage: PageResponse<ArticleDTO> = {
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
const emptyForm = (): ArticleMutationPayload => ({
    title: '',
    slug: '',
    seoTitle: '',
    metaDescription: '',
    canonicalUrl: '',
    thumbnailUrl: '',
    thumbnailAlt: '',
    excerpt: '',
    content: '',
    authorName: '',
    category: '',
    status: 'PUBLISHED'
});
const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const quillModules = {toolbar: [[{header: [1, 2, 3, false]}], ['bold', 'italic', 'underline', 'strike', 'blockquote'], [{list: 'ordered'}, {list: 'bullet'}], ['link'], ['clean']]};
const quillFormats = ['header', 'bold', 'italic', 'underline', 'strike', 'blockquote', 'list', 'bullet', 'link'];
const errorMessage = (error: any) => error?.response?.data?.message || error?.message || 'Thao tác thất bại.';
const formatDate = (value?: string | null) => value ? new Date(value).toLocaleDateString('vi-VN') : '-';

const toPayload = (article: ArticleDetailDTO): ArticleMutationPayload => ({
    title: article.title || '', slug: article.slug || '', seoTitle: article.seoTitle || '',
    metaDescription: article.metaDescription || '', canonicalUrl: article.canonicalUrl || '',
    thumbnailUrl: article.thumbnailUrl || '', thumbnailAlt: article.thumbnailAlt || '',
    excerpt: article.excerpt || '', content: article.content || '', authorName: article.authorName || '',
    category: article.category || '', status: 'PUBLISHED',
});

export default function AdminArticles() {
    const confirm = useConfirmDialog();
    const titleInputRef = useRef<HTMLInputElement>(null);
    const [pageData, setPageData] = useState(emptyPage);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState<ArticleDetailDTO | null>(null);
    const [form, setForm] = useState<ArticleMutationPayload>(() => emptyForm());
    const [submitting, setSubmitting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setPageData(await ArticleService.getPublishedArticles({page, size: PAGE_SIZE}));
        } catch (loadError) {
            setError(errorMessage(loadError));
        } finally {
            setLoading(false);
        }
    }, [page]);
    useEffect(() => {
        void load();
    }, [load]);
    useEffect(() => {
        if (!modalOpen) return;
        window.setTimeout(() => titleInputRef.current?.focus(), 0);
        const onKeyDown = (event: KeyboardEvent) => event.key === 'Escape' && setModalOpen(false);
        document.addEventListener('keydown', onKeyDown);
        return () => document.removeEventListener('keydown', onKeyDown);
    }, [modalOpen]);

    const openCreate = () => {
        setEditing(null);
        setForm(emptyForm());
        setError('');
        setModalOpen(true);
    };
    const openEdit = async (article: ArticleDTO) => {
        setLoading(true);
        setError('');
        try {
            const detail = await ArticleService.getPublishedArticle(article.slug);
            setEditing(detail);
            setForm(toPayload(detail));
            setModalOpen(true);
        } catch (loadError) {
            setError(errorMessage(loadError));
        } finally {
            setLoading(false);
        }
    };
    const validate = () => {
        if (!form.title.trim()) return 'Tiêu đề là bắt buộc.';
        if (form.slug?.trim() && !slugPattern.test(form.slug.trim())) return 'Slug chỉ gồm chữ thường, số và dấu gạch ngang.';
        if (!form.excerpt?.trim()) return 'Mô tả ngắn là bắt buộc.';
        if (!form.content?.replace(/<[^>]*>/g, '').trim()) return 'Nội dung bài viết là bắt buộc.';
        if ((form.seoTitle || '').length > 70) return 'SEO title không nên vượt quá 70 ký tự.';
        if ((form.metaDescription || '').length > 170) return 'Meta description không nên vượt quá 170 ký tự.';
        if (form.thumbnailUrl && !form.thumbnailAlt?.trim()) return 'Vui lòng nhập alt text cho ảnh đại diện.';
        if (form.canonicalUrl?.trim()) {
            try {
                const canonical = new URL(form.canonicalUrl.trim());
                if (!['http:', 'https:'].includes(canonical.protocol)) return 'Canonical URL phải dùng HTTP hoặc HTTPS.';
            } catch {
                return 'Canonical URL không hợp lệ.';
            }
        }
        return '';
    };
    const submit = async (event: FormEvent) => {
        event.preventDefault();
        const invalid = validate();
        if (invalid) {
            setError(invalid);
            return;
        }
        setSubmitting(true);
        setError('');
        const payload: ArticleMutationPayload = {
            ...form,
            title: form.title.trim(),
            slug: form.slug?.trim(),
            excerpt: form.excerpt?.trim(),
            status: 'PUBLISHED'
        };
        try {
            if (editing) await ArticleService.updateArticle(editing.id, payload); else await ArticleService.createArticle(payload);
            setModalOpen(false);
            await load();
            ToastService.success(editing ? 'Đã cập nhật bài viết.' : 'Đã xuất bản bài viết.');
        } catch (submitError) {
            setError(errorMessage(submitError));
        } finally {
            setSubmitting(false);
        }
    };
    const remove = async (article: ArticleDTO) => {
        if (!await confirm({
            title: 'Xóa bài viết?',
            message: `Bài “${article.title}” sẽ bị xóa khỏi website.`,
            confirmLabel: 'Xóa bài',
            tone: 'danger'
        })) return;
        setLoading(true);
        setError('');
        try {
            await ArticleService.deleteArticle(article.id);
            await load();
            ToastService.success('Đã xóa bài viết.');
        } catch (removeError) {
            setError(errorMessage(removeError));
        } finally {
            setLoading(false);
        }
    };
    const contentLength = useMemo(() => (form.content || '').replace(/<[^>]*>/g, '').trim().length, [form.content]);

    return <div className="space-y-6 pt-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
            <div><h1 className="text-2xl font-black uppercase text-on-surface">Bài viết</h1><p
                className="mt-1 text-xs font-semibold text-outline">Quản lý nội dung đã xuất bản trên SOBU Blog.</p>
            </div>
            <button onClick={openCreate}
                    className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-black uppercase text-on-primary shadow-sm transition-colors hover:bg-primary-container">
                <Plus className="h-4 w-4"/>Thêm bài viết
            </button>
        </div>
        {error && !modalOpen && <div role="alert"
                                     className="flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0"/>{error}</div>}
        <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface shadow-sm">
            <div className="space-y-3 p-3 md:hidden">{loading ?
                <Loader2 className="mx-auto my-12 h-8 w-8 animate-spin text-primary"/> : pageData.content.map(article =>
                    <article key={article.id}
                             className="rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4">
                        <div className="flex gap-3">{article.thumbnailUrl ?
                            <img src={getPublicImageUrl(article.thumbnailUrl)}
                                 alt={article.thumbnailAlt || article.title}
                                 className="h-16 w-20 rounded-lg object-cover"/> : <div
                                className="flex h-16 w-20 shrink-0 items-center justify-center rounded-lg bg-surface-container">
                                <FileText className="h-5 w-5 text-outline"/></div>}
                            <div className="min-w-0 flex-1"><h2
                                className="line-clamp-2 text-sm font-black text-on-surface">{article.title}</h2><p
                                className="mt-1 truncate font-mono text-[10px] text-primary">{article.slug}</p><p
                                className="mt-2 text-[10px] font-bold text-outline">{article.category || 'Chưa phân loại'} · {formatDate(article.publishedAt)}</p>
                            </div>
                        </div>
                        <div className="mt-3 flex justify-end gap-2 border-t border-outline-variant/20 pt-3">
                            <button onClick={() => void openEdit(article)}
                                    className="inline-flex cursor-pointer items-center gap-1 rounded-lg bg-primary/10 px-3 py-2 text-xs font-black text-primary">
                                <Edit className="h-3.5 w-3.5"/>Sửa
                            </button>
                            <button onClick={() => void remove(article)}
                                    className="inline-flex cursor-pointer items-center gap-1 rounded-lg bg-error/10 px-3 py-2 text-xs font-black text-error">
                                <Trash2 className="h-3.5 w-3.5"/>Xóa
                            </button>
                        </div>
                    </article>)}{!loading && !pageData.content.length &&
                <div className="py-12 text-center text-xs font-bold text-outline">Chưa có bài viết đã xuất
                    bản.</div>}</div>
            <div className="hidden overflow-x-auto md:block">
                <table className="w-full min-w-[780px] text-left text-xs">
                    <thead className="bg-surface-variant text-on-surface-variant">
                    <tr>
                        <th className="px-5 py-4">Bài viết</th>
                        <th className="px-5 py-4">Chủ đề</th>
                        <th className="px-5 py-4">Xuất bản</th>
                        <th className="px-5 py-4 text-center">Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? <tr>
                        <td colSpan={4} className="py-16 text-center"><Loader2
                            className="mx-auto h-8 w-8 animate-spin text-primary"/></td>
                    </tr> : pageData.content.map((article) => <tr key={article.id}
                                                                  className="border-t border-outline-variant/20">
                        <td className="px-5 py-4">
                            <div className="flex items-center gap-3">{article.thumbnailUrl ?
                                <img src={getPublicImageUrl(article.thumbnailUrl)}
                                     alt={article.thumbnailAlt || article.title}
                                     className="h-12 w-16 rounded-lg object-cover"/> : <div
                                    className="flex h-12 w-16 items-center justify-center rounded-lg bg-surface-container">
                                    <FileText className="h-5 w-5 text-outline"/></div>}
                                <div className="min-w-0"><p
                                    className="max-w-md truncate font-black text-on-surface">{article.title}</p><p
                                    className="mt-1 max-w-md truncate font-mono text-[10px] text-primary">{article.slug}</p>
                                </div>
                            </div>
                        </td>
                        <td className="px-5 py-4 font-bold text-outline">{article.category || '-'}</td>
                        <td className="px-5 py-4 font-bold text-outline">{formatDate(article.publishedAt)}</td>
                        <td className="px-5 py-4">
                            <div className="flex justify-center gap-2">
                                <button onClick={() => void openEdit(article)} aria-label={`Sửa ${article.title}`}
                                        className="cursor-pointer rounded-lg bg-primary/10 p-2 text-primary hover:bg-primary/20">
                                    <Edit className="h-4 w-4"/></button>
                                <button onClick={() => void remove(article)} aria-label={`Xóa ${article.title}`}
                                        className="cursor-pointer rounded-lg bg-error/10 p-2 text-error hover:bg-error/20">
                                    <Trash2 className="h-4 w-4"/></button>
                            </div>
                        </td>
                    </tr>)}
                    {!loading && !pageData.content.length && <tr>
                        <td colSpan={4} className="py-16 text-center font-bold text-outline"><FileText
                            className="mx-auto mb-2 h-9 w-9 opacity-30"/>Chưa có bài viết đã xuất bản.
                        </td>
                    </tr>}
                    </tbody>
                </table>
            </div>
            {pageData.totalPages > 1 &&
                <div className="flex items-center justify-between border-t border-outline-variant/20 px-5 py-4"><span
                    className="text-xs font-bold text-outline">Trang {pageData.pageNumber + 1}/{pageData.totalPages}</span>
                    <div className="flex gap-2">
                        <button disabled={!pageData.hasPrevious || loading}
                                onClick={() => setPage((value) => Math.max(0, value - 1))} aria-label="Trang trước"
                                className="cursor-pointer rounded-lg bg-surface-container p-2 disabled:opacity-40">
                            <ChevronLeft className="h-4 w-4"/></button>
                        <button disabled={!pageData.hasNext || loading} onClick={() => setPage((value) => value + 1)}
                                aria-label="Trang sau"
                                className="cursor-pointer rounded-lg bg-surface-container p-2 disabled:opacity-40">
                            <ChevronRight className="h-4 w-4"/></button>
                    </div>
                </div>}</div>

        {modalOpen && <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"
                           onMouseDown={(event) => event.target === event.currentTarget && setModalOpen(false)}>
            <div role="dialog" aria-modal="true" aria-labelledby="article-dialog-title"
                 className="max-h-[92vh] w-full max-w-5xl overflow-y-auto rounded-3xl bg-surface shadow-xl">
                <div
                    className="sticky top-0 z-10 flex items-center justify-between border-b border-outline-variant/30 bg-surface p-5 sm:p-6">
                    <h2 id="article-dialog-title"
                        className="text-xl font-black text-on-surface">{editing ? 'Cập nhật bài viết' : 'Thêm bài viết'}</h2>
                    <button onClick={() => setModalOpen(false)} aria-label="Đóng"
                            className="cursor-pointer rounded-lg p-2 text-outline hover:bg-surface-container"><X
                        className="h-5 w-5"/></button>
                </div>
                <form onSubmit={submit} className="space-y-5 p-5 sm:p-6">
                    <div className="grid gap-4 md:grid-cols-2"><label
                        className="text-xs font-black uppercase text-outline">Tiêu đề *<input ref={titleInputRef}
                                                                                              value={form.title}
                                                                                              onChange={(event) => setForm({
                                                                                                  ...form,
                                                                                                  title: event.target.value
                                                                                              })}
                                                                                              className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm font-semibold text-on-surface outline-none focus:ring-2 focus:ring-primary/20"/></label><label
                        className="text-xs font-black uppercase text-outline">Slug<input value={form.slug}
                                                                                         onChange={(event) => setForm({
                                                                                             ...form,
                                                                                             slug: event.target.value.toLowerCase().replace(/\s+/g, '-')
                                                                                         })}
                                                                                         placeholder="Tự sinh từ tiêu đề nếu để trống"
                                                                                         className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 font-mono text-sm text-on-surface outline-none focus:ring-2 focus:ring-primary/20"/></label>
                    </div>
                    <div className="grid gap-4 md:grid-cols-2"><label
                        className="text-xs font-black uppercase text-outline">Tác giả<input value={form.authorName}
                                                                                            onChange={(event) => setForm({
                                                                                                ...form,
                                                                                                authorName: event.target.value
                                                                                            })}
                                                                                            className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm text-on-surface"/></label><label
                        className="text-xs font-black uppercase text-outline">Chủ đề<input value={form.category}
                                                                                           onChange={(event) => setForm({
                                                                                               ...form,
                                                                                               category: event.target.value
                                                                                           })}
                                                                                           className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm text-on-surface"/></label>
                    </div>
                    <label className="block text-xs font-black uppercase text-outline">Mô tả ngắn *<textarea
                        value={form.excerpt} onChange={(event) => setForm({...form, excerpt: event.target.value})}
                        rows={3}
                        className="mt-2 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-4 text-sm text-on-surface"/></label>
                    <div><p className="mb-2 text-xs font-black uppercase text-outline">Ảnh đại diện</p><ImageUploader
                        uploadedUrls={form.thumbnailUrl ? [form.thumbnailUrl] : []}
                        onChange={(urls) => setForm({...form, thumbnailUrl: urls[urls.length - 1] || ''})}
                        subDirectory="articles"/></div>
                    <label className="block text-xs font-black uppercase text-outline">Alt ảnh<input
                        value={form.thumbnailAlt}
                        onChange={(event) => setForm({...form, thumbnailAlt: event.target.value})}
                        className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm text-on-surface"/></label>
                    <div className="grid gap-4 md:grid-cols-2"><label
                        className="text-xs font-black uppercase text-outline">SEO title<input value={form.seoTitle}
                                                                                              onChange={(event) => setForm({
                                                                                                  ...form,
                                                                                                  seoTitle: event.target.value
                                                                                              })}
                                                                                              className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm text-on-surface"/></label><label
                        className="text-xs font-black uppercase text-outline">Canonical URL<input
                        value={form.canonicalUrl}
                        onChange={(event) => setForm({...form, canonicalUrl: event.target.value})}
                        className="mt-2 h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm text-on-surface"/></label>
                    </div>
                    <label className="block text-xs font-black uppercase text-outline">Meta description<textarea
                        value={form.metaDescription}
                        onChange={(event) => setForm({...form, metaDescription: event.target.value})} rows={2}
                        className="mt-2 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-4 text-sm text-on-surface"/></label>
                    <div>
                        <div className="mb-2 flex items-center justify-between gap-3"><label
                            className="text-xs font-black uppercase text-outline">Nội dung *</label><span
                            className="text-[11px] font-bold text-outline">{contentLength} ký tự</span></div>
                        <ReactQuill theme="snow" value={form.content || ''}
                                    onChange={(value) => setForm({...form, content: value})} modules={quillModules}
                                    formats={quillFormats} className="static-page-editor"/></div>
                    {error && <div role="alert"
                                   className="rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">{error}</div>}
                    <div className="flex justify-end gap-3 border-t border-outline-variant/30 pt-4">
                        <button type="button" onClick={() => setModalOpen(false)}
                                className="cursor-pointer rounded-xl px-5 py-2.5 text-xs font-black uppercase text-on-surface hover:bg-surface-container">Hủy
                        </button>
                        <button type="submit" disabled={submitting}
                                className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-black uppercase text-on-primary disabled:opacity-50">{submitting ?
                            <Loader2 className="h-4 w-4 animate-spin"/> :
                            <Save className="h-4 w-4"/>}{editing ? 'Lưu thay đổi' : 'Xuất bản'}</button>
                    </div>
                </form>
            </div>
        </div>}
    </div>;
}
