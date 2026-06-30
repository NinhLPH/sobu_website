import 'react-quill-new/dist/quill.snow.css';

import {FormEvent, useCallback, useEffect, useMemo, useState} from 'react';
import ReactQuill from 'react-quill-new';
import {
    AlertCircle,
    ChevronLeft,
    ChevronRight,
    Edit,
    FileText,
    Loader2,
    Plus,
    Save,
    Search,
    Trash2,
    X
} from 'lucide-react';
import {PageResponse} from '../../interface/api-response';
import {StaticPageDTO, StaticPageMutationPayload} from '../../interface/static-page.model';
import {StaticPageService} from '../../service/static-page.service';
import {ToastService} from '../../service/toast.service';

const PAGE_SIZE = 10;

const emptyPage: PageResponse<StaticPageDTO> = {
    content: [],
    pageNumber: 1,
    pageSize: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false,
};

const emptyForm = (): StaticPageMutationPayload => ({
    slug: '',
    title: '',
    htmlContent: '',
    isPublished: true,
});

const toFormValue = (page: StaticPageDTO): StaticPageMutationPayload => ({
    slug: page.slug,
    title: page.title,
    htmlContent: page.htmlContent || '',
    isPublished: page.isPublished,
});

const errorMessage = (error: any) =>
    error?.response?.data?.message || error?.message || 'Thao tác thất bại.';

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

const quillModules = {
    toolbar: [
        [{header: [1, 2, 3, false]}],
        ['bold', 'italic', 'underline', 'strike', 'blockquote'],
        [{list: 'ordered'}, {list: 'bullet'}],
        ['link'],
        ['clean'],
    ],
};

const quillFormats = [
    'header',
    'bold',
    'italic',
    'underline',
    'strike',
    'blockquote',
    'list',
    'bullet',
    'link',
];

const formatDate = (value?: string) => value ? new Date(value).toLocaleDateString('vi-VN') : '-';

export default function AdminStaticPages() {
    const [pageData, setPageData] = useState(emptyPage);
    const [page, setPage] = useState(1);
    const [searchInput, setSearchInput] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState<StaticPageDTO | null>(null);
    const [form, setForm] = useState<StaticPageMutationPayload>(emptyForm);
    const [submitting, setSubmitting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const result = await StaticPageService.searchPages(searchTerm, {
                page,
                pageSize: PAGE_SIZE,
                sortBy: 'updatedAt',
                sortDirection: 'DESC',
            });
            setPageData(result);
        } catch (loadError) {
            setError(errorMessage(loadError));
        } finally {
            setLoading(false);
        }
    }, [page, searchTerm]);

    useEffect(() => {
        void load();
    }, [load]);

    const contentLength = useMemo(
        () => form.htmlContent.replace(/<[^>]*>/g, '').trim().length,
        [form.htmlContent]
    );

    const openCreate = () => {
        setEditing(null);
        setForm(emptyForm());
        setError(null);
        setModalOpen(true);
    };

    const openEdit = (staticPage: StaticPageDTO) => {
        setEditing(staticPage);
        setForm(toFormValue(staticPage));
        setError(null);
        setModalOpen(true);
    };

    const validate = () => {
        if (!form.slug.trim()) return 'Slug là bắt buộc.';
        if (!slugPattern.test(form.slug.trim())) return 'Slug chỉ gồm chữ thường, số và dấu gạch ngang.';
        if (!form.title.trim()) return 'Tiêu đề là bắt buộc.';
        return null;
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }
        setSubmitting(true);
        setError(null);
        const payload = {
            ...form,
            slug: form.slug.trim(),
            title: form.title.trim(),
            htmlContent: form.htmlContent || '',
        };
        try {
            if (editing) {
                await StaticPageService.updatePage(editing.id, payload);
            } else {
                await StaticPageService.createPage(payload);
            }
            await load();
            ToastService.success(editing ? 'Đã cập nhật trang tĩnh.' : 'Đã tạo trang tĩnh.');
            setModalOpen(false);
        } catch (submitError) {
            setError(errorMessage(submitError));
        } finally {
            setSubmitting(false);
        }
    };

    const remove = async (staticPage: StaticPageDTO) => {
        if (!window.confirm(`Bạn có chắc muốn xóa trang "${staticPage.title}"?`)) return;
        setLoading(true);
        setError(null);
        try {
            await StaticPageService.deletePage(staticPage.id);
            await load();
            ToastService.success('Đã xóa trang.');
        } catch (removeError) {
            setError(errorMessage(removeError));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-black uppercase text-on-surface">Trang tĩnh</h1>
                    <p className="mt-1 text-xs font-semibold text-outline">
                        Quản lý nội dung các trang tĩnh như About, Privacy Policy và Terms.
                    </p>
                </div>
                <button
                    type="button"
                    onClick={openCreate}
                    className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-black uppercase text-on-primary shadow-sm transition-colors hover:bg-primary-container"
                >
                    <Plus className="h-4 w-4"/>
                    Thêm trang
                </button>
            </div>

            <form
                onSubmit={(event) => {
                    event.preventDefault();
                    setPage(1);
                    setSearchTerm(searchInput.trim());
                }}
                className="flex gap-3 rounded-2xl border border-outline-variant/30 bg-white p-4"
            >
                <div className="relative flex-1">
                    <label htmlFor="static-page-search" className="sr-only">Tìm trang</label>
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/>
                    <input
                        id="static-page-search"
                        value={searchInput}
                        onChange={(event) => setSearchInput(event.target.value)}
                        placeholder="Tìm theo Slug hoặc Tiêu đề..."
                        className="h-11 w-full rounded-xl bg-surface-container py-2.5 pl-10 pr-4 text-xs font-semibold text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                    />
                </div>
                <button
                    type="submit"
                    className="cursor-pointer rounded-xl bg-surface-container px-4 text-xs font-black uppercase text-on-surface transition-colors hover:bg-surface-container-high"
                >
                    Tìm
                </button>
            </form>

            {error && !modalOpen && (
                <div className="flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0"/>
                    <span>{error}</span>
                </div>
            )}

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full min-w-[840px] text-left text-xs">
                        <thead className="bg-surface-variant text-on-surface-variant">
                        <tr>
                            <th className="px-5 py-4">Tiêu đề</th>
                            <th className="px-5 py-4">Slug</th>
                            <th className="px-5 py-4">Trạng thái</th>
                            <th className="px-5 py-4">Cập nhật</th>
                            <th className="px-5 py-4 text-center">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan={5} className="py-16 text-center">
                                    <Loader2 className="mx-auto h-8 w-8 animate-spin text-primary"/>
                                </td>
                            </tr>
                        ) : pageData.content.map((staticPage) => (
                            <tr key={staticPage.id} className="border-t border-outline-variant/20">
                                <td className="max-w-sm px-5 py-4">
                                    <p className="truncate font-black text-on-surface">{staticPage.title}</p>
                                    <p className="mt-1 text-[10px] font-semibold text-outline">
                                        {staticPage.htmlContent?.trim() ? 'Đã có nội dung' : 'Nội dung rỗng'}
                                    </p>
                                </td>
                                <td className="px-5 py-4 font-mono font-bold text-primary">{staticPage.slug}</td>
                                <td className="px-5 py-4">
                                    <span className={`rounded-full px-2.5 py-1 text-[10px] font-black uppercase ${
                                        staticPage.isPublished ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                                    }`}>
                                        {staticPage.isPublished ? 'Published' : 'Draft'}
                                    </span>
                                </td>
                                <td className="px-5 py-4 font-bold text-outline">{formatDate(staticPage.updatedAt)}</td>
                                <td className="px-5 py-4">
                                    <div className="flex justify-center gap-2">
                                        <button
                                            type="button"
                                            onClick={() => openEdit(staticPage)}
                                            aria-label={`Sửa trang ${staticPage.title}`}
                                            className="cursor-pointer rounded-lg bg-primary/10 p-2 text-primary transition-colors hover:bg-primary/20"
                                        >
                                            <Edit className="h-4 w-4"/>
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => void remove(staticPage)}
                                            aria-label={`Xóa trang ${staticPage.title}`}
                                            className="cursor-pointer rounded-lg bg-error/10 p-2 text-error transition-colors hover:bg-error/20"
                                        >
                                            <Trash2 className="h-4 w-4"/>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {!loading && !pageData.content.length && (
                            <tr>
                                <td colSpan={5} className="py-16 text-center font-bold text-outline">
                                    <FileText className="mx-auto mb-2 h-9 w-9 opacity-30"/>
                                    Không có trang tĩnh.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>

                {pageData.totalPages > 1 && (
                    <div className="flex items-center justify-between border-t border-outline-variant/20 px-5 py-4">
                        <span className="text-xs font-bold text-outline">
                            Trang {pageData.pageNumber}/{pageData.totalPages}
                        </span>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                disabled={!pageData.hasPrevious || loading}
                                onClick={() => setPage((value) => Math.max(1, value - 1))}
                                className="cursor-pointer rounded-lg bg-surface-container p-2 disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                <ChevronLeft className="h-4 w-4"/>
                            </button>
                            <button
                                type="button"
                                disabled={!pageData.hasNext || loading}
                                onClick={() => setPage((value) => value + 1)}
                                className="cursor-pointer rounded-lg bg-surface-container p-2 disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                <ChevronRight className="h-4 w-4"/>
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {modalOpen && (
                <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4">
                    <div className="max-h-[92vh] w-full max-w-5xl overflow-y-auto rounded-3xl bg-white shadow-xl">
                        <div className="flex items-center justify-between border-b border-outline-variant/30 p-6">
                            <div>
                                <h2 className="text-xl font-black text-on-surface">
                                    {editing ? 'Cập nhật trang tĩnh' : 'Thêm trang tĩnh'}
                                </h2>
                            </div>
                            <button
                                type="button"
                                onClick={() => setModalOpen(false)}
                                aria-label="Dong"
                                className="cursor-pointer rounded-lg p-2 text-outline transition-colors hover:bg-surface-container hover:text-on-surface"
                            >
                                <X className="h-6 w-6"/>
                            </button>
                        </div>

                        <form onSubmit={submit} className="space-y-5 p-6">
                            <div className="grid gap-4 sm:grid-cols-2">
                                <div>
                                    <label htmlFor="static-page-title" className="mb-1 block text-xs font-black uppercase text-outline">
                                        Tiêu đề
                                    </label>
                                    <input
                                        id="static-page-title"
                                        required
                                        value={form.title}
                                        onChange={(event) => setForm({...form, title: event.target.value})}
                                        className="h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm font-semibold text-on-surface outline-none focus:border-primary/50 focus:ring-2 focus:ring-primary/15"
                                    />
                                </div>
                                <div>
                                    <label htmlFor="static-page-slug" className="mb-1 block text-xs font-black uppercase text-outline">
                                        Slug
                                    </label>
                                    <input
                                        id="static-page-slug"
                                        required
                                        value={form.slug}
                                        onChange={(event) => setForm({...form, slug: event.target.value})}
                                        placeholder="privacy-policy"
                                        className="h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 font-mono text-sm font-semibold text-on-surface outline-none focus:border-primary/50 focus:ring-2 focus:ring-primary/15"
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                                    <label className="block text-xs font-black uppercase text-outline">
                                        Nội dung
                                    </label>
                                    <span className="text-[11px] font-bold text-outline">{contentLength} ký tự nội dung</span>
                                </div>
                                <ReactQuill
                                    theme="snow"
                                    value={form.htmlContent}
                                    onChange={(value) => setForm({...form, htmlContent: value})}
                                    modules={quillModules}
                                    formats={quillFormats}
                                    className="static-page-editor"
                                />
                            </div>

                            <label className="inline-flex cursor-pointer items-center gap-3 text-sm font-bold text-on-surface">
                                <input
                                    type="checkbox"
                                    checked={form.isPublished}
                                    onChange={(event) => setForm({...form, isPublished: event.target.checked})}
                                    className="h-4 w-4 accent-primary"
                                />
                                Publish
                            </label>

                            {error && (
                                <div className="flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0"/>
                                    <span>{error}</span>
                                </div>
                            )}

                            <div className="flex justify-end gap-3 border-t border-outline-variant/30 pt-4">
                                <button
                                    type="button"
                                    onClick={() => setModalOpen(false)}
                                    className="cursor-pointer rounded-xl px-5 py-2.5 text-xs font-black uppercase text-on-surface transition-colors hover:bg-surface-container"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    disabled={submitting}
                                    className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-black uppercase text-on-primary shadow-sm transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                    {submitting ? <Loader2 className="h-4 w-4 animate-spin"/> : <Save className="h-4 w-4"/>}
                                    Lưu trang
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
