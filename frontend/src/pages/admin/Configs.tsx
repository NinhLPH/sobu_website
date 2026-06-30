import {ChangeEvent, useCallback, useEffect, useMemo, useState} from 'react';
import {
    AlertCircle,
    Check,
    FileJson,
    Loader2,
    RefreshCw,
    Save,
    UploadCloud,
    Layout,
    Code,
    Plus,
    Trash2
} from 'lucide-react';
import {AdminUiService} from '../../service/admin-ui.service';
import {WebsiteConfigurationDTO} from '../../interface/public-ui-config.model';
import {PageResponse} from '../../interface/api-response';
import {ToastService} from '../../service/toast.service';
import {usePublicUiStore} from '../../store/usePublicUiStore';
import {groupConfigsByGroupName} from '../../utils/website-config';
import {FileService} from '../../service/file.service';
import {getPublicImageUrl} from '../../utils/file-url';

const CONFIG_PAGE_SIZE = 500;
const HIDDEN_CONFIG_GROUPS = new Set(['CHECKOUT', 'BUSINESS']);
const GROUP_ORDER = ['THEME', 'GENERAL', 'HOME_SECTION', 'HOME_PROMO', 'HOME_PARTNER', 'FOOTER', 'SOCIAL', 'SEO'];

const emptyPage: PageResponse<WebsiteConfigurationDTO> = {
    content: [], pageNumber: 1, pageSize: CONFIG_PAGE_SIZE, totalElements: 0, totalPages: 0,
    first: true, last: true, hasNext: false, hasPrevious: false,
};

type DraftValues = Record<string, string>;
type GroupErrors = Record<string, string | null>;

const errorMessage = (error: any) =>
    error?.response?.data?.message || error?.message || 'Thao tác cấu hình thất bại.';

const isSupportedUrl = (value: string) =>
    !value || value.startsWith('/') || /^https?:\/\//i.test(value);

const isHexColor = (value: string) => /^#[0-9a-f]{6}$/i.test(value);

const getConfigImagePreviewUrl = (value: string) => {
    const trimmedValue = value.trim();
    if (!trimmedValue) return '';
    if (/^https?:\/\//i.test(trimmedValue) || trimmedValue.startsWith('/assets/')) {
        return trimmedValue;
    }
    return getPublicImageUrl(trimmedValue);
};

const toDraftValues = (configs: WebsiteConfigurationDTO[]) =>
    configs.reduce<DraftValues>((accumulator, config) => {
        accumulator[config.key] = config.value ?? '';
        return accumulator;
    }, {});

const isVisibleConfigGroup = (groupName?: string) =>
    !HIDDEN_CONFIG_GROUPS.has((groupName || '').trim().toUpperCase());

const sortGroupNames = (groupNames: string[]) =>
    [...groupNames].sort((left, right) => {
        const leftIndex = GROUP_ORDER.indexOf(left);
        const rightIndex = GROUP_ORDER.indexOf(right);
        if (leftIndex !== -1 || rightIndex !== -1) {
            return (leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex) -
                (rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex);
        }
        return left.localeCompare(right);
    });

const getGroupTitle = (groupName: string) => {
    switch (groupName) {
        case 'THEME':
            return 'Giao diện';
        case 'GENERAL':
            return 'Thông tin chung';
        case 'HOME_SECTION':
            return 'Trang chủ';
        case 'HOME_PROMO':
            return 'Home promo';
        case 'HOME_PARTNER':
            return 'Đối tác';
        case 'FOOTER':
            return 'Footer';
        case 'SOCIAL':
            return 'Mạng xã hội';
        case 'SEO':
            return 'SEO';
        default:
            return groupName;
    }
};

function JsonField({
                       value,
                       onChange,
                       disabled,
                       onBlur,
                       configKey
                   }: {
    value: string;
    onChange: (v: string) => void;
    disabled?: boolean;
    onBlur: () => void;
    configKey: string;
}) {
    const [mode, setMode] = useState<'visual' | 'raw'>('visual');

    let parsed: any = null;
    let isValid = true;
    try {
        parsed = value ? JSON.parse(value) : null;
    } catch (e) {
        isValid = false;
    }

    useEffect(() => {
        if (!isValid && mode === 'visual') {
            setMode('raw');
        }
    }, [isValid, mode]);

    const updateData = (newData: any) => {
        onChange(JSON.stringify(newData, null, 2));
    };

    const renderVisual = () => {
        if (!isValid) return <div className="p-4 text-xs font-bold text-error">JSON bị lỗi cú pháp, vui lòng dùng chế độ Code để sửa.</div>;
        if (parsed === null) return <div className="p-4 text-xs font-semibold text-outline italic">Dữ liệu rỗng.</div>;

        if (Array.isArray(parsed)) {
            const isObjectArray = parsed.length > 0 && typeof parsed[0] === 'object' && parsed[0] !== null;
            const isStringArray = parsed.length > 0 && typeof parsed[0] === 'string';

            if (parsed.length === 0) {
                return <div className="p-4 text-xs font-semibold text-outline italic">Danh sách đang trống. Hãy chuyển sang Tab Code để nhập dữ liệu mẫu đầu tiên.</div>;
            }

            if (isStringArray) {
                return (
                    <div className="space-y-2 p-2">
                        {parsed.map((str: string, index: number) => (
                            <div key={index} className="flex gap-2">
                                <input
                                    type="text"
                                    value={str}
                                    disabled={disabled}
                                    onChange={(e) => {
                                        const arr = [...parsed];
                                        arr[index] = e.target.value;
                                        updateData(arr);
                                    }}
                                    className="h-10 flex-1 rounded-xl border border-outline-variant/40 bg-white px-3 text-xs font-semibold text-on-surface outline-none focus:border-primary/50"
                                />
                                <button
                                    type="button"
                                    disabled={disabled}
                                    onClick={() => updateData(parsed.filter((_: any, i: number) => i !== index))}
                                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-error/20 bg-error/10 text-error transition-colors hover:bg-error/20 disabled:opacity-50"
                                >
                                    <Trash2 className="h-4 w-4" />
                                </button>
                            </div>
                        ))}
                        <button
                            type="button"
                            disabled={disabled}
                            onClick={() => updateData([...parsed, ""])}
                            className="mt-2 inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl border border-dashed border-primary/40 bg-primary/5 px-4 text-xs font-bold text-primary transition-colors hover:bg-primary/10"
                        >
                            <Plus className="h-4 w-4" /> Thêm dòng mới
                        </button>
                    </div>
                );
            }

            if (isObjectArray) {
                return (
                    <div className="space-y-4 p-2">
                        {parsed.map((item: any, index: number) => {
                            const keys = Object.keys(item);
                            return (
                                <div key={index} className="relative rounded-xl border border-outline-variant/40 bg-white p-4 shadow-[0_2px_10px_rgba(0,0,0,0.02)]">
                                    <div className="absolute right-3 top-3">
                                        <button
                                            type="button"
                                            disabled={disabled}
                                            onClick={() => updateData(parsed.filter((_: any, i: number) => i !== index))}
                                            className="rounded-lg p-1.5 text-outline transition-colors hover:bg-error/10 hover:text-error"
                                        >
                                            <Trash2 className="h-4 w-4" />
                                        </button>
                                    </div>
                                    <span className="mb-3 block text-[10px] font-black uppercase text-outline">Mục {index + 1}</span>
                                    <div className="grid gap-3 sm:grid-cols-2">
                                        {keys.map(k => (
                                            <div key={k}>
                                                <label className="mb-1 block text-[10px] font-bold text-outline-variant">{k}</label>
                                                <input
                                                    type="text"
                                                    value={item[k] || ''}
                                                    disabled={disabled}
                                                    onChange={(e) => {
                                                        const arr = [...parsed];
                                                        arr[index] = { ...arr[index], [k]: e.target.value };
                                                        updateData(arr);
                                                    }}
                                                    className="h-10 w-full rounded-lg border border-outline-variant/40 bg-surface-container-lowest px-3 text-xs font-semibold text-on-surface outline-none focus:border-primary/50"
                                                />
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            );
                        })}
                        <button
                            type="button"
                            disabled={disabled}
                            onClick={() => {
                                const template = { ...parsed[0] };
                                Object.keys(template).forEach(k => template[k] = "");
                                updateData([...parsed, template]);
                            }}
                            className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl border border-dashed border-primary/40 bg-primary/5 px-4 text-xs font-bold text-primary transition-colors hover:bg-primary/10"
                        >
                            <Plus className="h-4 w-4" /> Thêm mục mới
                        </button>
                    </div>
                );
            }
        }

        if (typeof parsed === 'object') {
            const keys = Object.keys(parsed);
            return (
                <div className="space-y-3 p-2">
                    {keys.map(k => (
                        <div key={k} className="flex flex-col sm:flex-row sm:items-center gap-2 rounded-lg bg-white p-2 border border-outline-variant/20">
                            <label className="w-1/3 text-xs font-bold text-on-surface break-all pl-2">{k}</label>
                            <input
                                type="text"
                                value={parsed[k] || ''}
                                disabled={disabled}
                                onChange={(e) => updateData({ ...parsed, [k]: e.target.value })}
                                className="h-10 flex-1 rounded-lg border border-outline-variant/40 bg-surface-container-lowest px-3 text-xs font-semibold text-on-surface outline-none focus:border-primary/50"
                            />
                        </div>
                    ))}
                </div>
            );
        }

        return <div className="p-4 text-xs font-semibold text-outline">Cấu trúc JSON phức tạp, vui lòng dùng chế độ Code.</div>;
    };

    return (
        <div className="flex flex-col gap-2 rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm overflow-hidden">
            <div className="flex justify-end gap-1 bg-surface-container/30 px-3 py-2 border-b border-outline-variant/20">
                <button
                    type="button"
                    onClick={() => setMode('visual')}
                    className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-[10px] font-black uppercase transition-colors ${
                        mode === 'visual' ? 'bg-primary text-on-primary shadow-sm' : 'text-outline hover:bg-surface-container-high hover:text-on-surface'
                    }`}
                >
                    <Layout className="h-3.5 w-3.5" /> Trực quan
                </button>
                <button
                    type="button"
                    onClick={() => setMode('raw')}
                    className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-[10px] font-black uppercase transition-colors ${
                        mode === 'raw' ? 'bg-primary text-on-primary shadow-sm' : 'text-outline hover:bg-surface-container-high hover:text-on-surface'
                    }`}
                >
                    <Code className="h-3.5 w-3.5" /> Code (Raw)
                </button>
            </div>

            <div className="p-2">
                {mode === 'visual' ? (
                    renderVisual()
                ) : (
                    <textarea
                        aria-label={`Giá trị ${configKey}`}
                        value={value}
                        disabled={disabled}
                        onChange={(event) => onChange(event.target.value)}
                        onBlur={onBlur}
                        rows={7}
                        spellCheck={false}
                        className="min-h-[160px] w-full resize-y rounded-xl border border-outline-variant/40 bg-white px-4 py-3 font-mono text-xs font-semibold leading-relaxed text-on-surface outline-none transition-colors focus:border-primary/50 focus:ring-2 focus:ring-primary/15 disabled:opacity-60"
                    />
                )}
            </div>
        </div>
    );
}

function ConfigField({
    config,
    value,
    disabled,
    uploading,
    onChange,
    onJsonBlur,
    onUpload,
}: {
    config: WebsiteConfigurationDTO;
    value: string;
    disabled?: boolean;
    uploading?: boolean;
    onChange: (value: string) => void;
    onJsonBlur: () => void;
    onUpload: (file: File) => void;
}) {
    if (config.type === 'boolean_type') {
        const enabled = value === 'true';
        return (
            <button
                type="button"
                role="switch"
                aria-checked={enabled}
                disabled={disabled}
                onClick={() => onChange(enabled ? 'false' : 'true')}
                className={`inline-flex w-fit cursor-pointer items-center gap-3 rounded-full border px-1.5 py-1.5 pr-4 text-xs font-black transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
                    enabled
                        ? 'border-primary/20 bg-primary/10 text-primary'
                        : 'border-outline-variant/40 bg-surface-container text-outline'
                }`}
            >
                <span className={`flex h-6 w-6 items-center justify-center rounded-full text-white transition-colors ${
                    enabled ? 'bg-primary' : 'bg-outline'
                }`}>
                    {enabled && <Check className="h-3.5 w-3.5"/>}
                </span>
                {enabled ? 'Bật' : 'Tắt'}
            </button>
        );
    }

    if (config.type === 'json') {
        return (
            <JsonField
                value={value}
                onChange={onChange}
                disabled={disabled}
                onBlur={onJsonBlur}
                configKey={config.key}
            />
        );
    }

    if (config.type === 'color') {
        return (
            <div className="flex flex-col gap-2 sm:flex-row">
                <input
                    aria-label={`Chọn màu ${config.key}`}
                    type="color"
                    value={isHexColor(value) ? value : '#000000'}
                    disabled={disabled}
                    onChange={(event) => onChange(event.target.value)}
                    className="h-11 w-full cursor-pointer rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-1 sm:w-20"
                />
                <input
                    aria-label={`Giá trị ${config.key}`}
                    value={value}
                    disabled={disabled}
                    onChange={(event) => onChange(event.target.value)}
                    placeholder="#00618e"
                    className="h-11 flex-1 rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 font-mono text-xs font-bold text-on-surface outline-none focus:border-primary/50 focus:ring-2 focus:ring-primary/15 disabled:opacity-60"
                />
            </div>
        );
    }

    if (config.type === 'image') {
        return (
            <div className="space-y-3">
                <div className="flex flex-col gap-2 sm:flex-row">
                    <input
                        aria-label={`Giá trị ${config.key}`}
                        value={value}
                        disabled={disabled}
                        onChange={(event) => onChange(event.target.value)}
                        placeholder="/api/public/files/configs/logo.png hoặc https://..."
                        className="h-11 flex-1 rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-xs font-semibold text-on-surface outline-none focus:border-primary/50 focus:ring-2 focus:ring-primary/15 disabled:opacity-60"
                    />
                    <label className={`inline-flex h-11 cursor-pointer items-center justify-center gap-2 rounded-xl border border-primary/20 px-4 text-xs font-black uppercase text-primary transition-colors hover:bg-primary/10 ${
                        disabled ? 'pointer-events-none opacity-50' : ''
                    }`}>
                        {uploading ? <Loader2 className="h-4 w-4 animate-spin"/> : <UploadCloud className="h-4 w-4"/>}
                        Upload
                        <input
                            type="file"
                            accept="image/*"
                            disabled={disabled}
                            className="hidden"
                            onChange={(event: ChangeEvent<HTMLInputElement>) => {
                                const file = event.target.files?.[0];
                                if (file) onUpload(file);
                                event.currentTarget.value = '';
                            }}
                        />
                    </label>
                </div>
                {value && (
                    <div className="flex items-center gap-3 rounded-xl bg-surface-container p-2">
                        <div className="flex h-14 w-20 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-surface-container-lowest">
                            <img src={getConfigImagePreviewUrl(value)} alt={`Preview ${config.key}`} className="h-full w-full object-cover"/>
                        </div>
                        <span className="line-clamp-2 break-all text-[11px] font-semibold text-outline">{value}</span>
                    </div>
                )}
            </div>
        );
    }

    return (
        <input
            aria-label={`Giá trị ${config.key}`}
            type={config.type === 'number' ? 'number' : 'text'}
            value={value}
            disabled={disabled}
            onChange={(event) => onChange(event.target.value)}
            className="h-11 w-full rounded-xl border border-outline-variant/40 bg-surface-container-lowest px-4 text-sm font-semibold text-on-surface outline-none focus:border-primary/50 focus:ring-2 focus:ring-primary/15 disabled:opacity-60"
        />
    );
}

export default function AdminConfigs() {
    const [pageData, setPageData] = useState(emptyPage);
    const [loading, setLoading] = useState(false);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [activeGroup, setActiveGroup] = useState('');
    const [draftValues, setDraftValues] = useState<DraftValues>({});
    const [groupErrors, setGroupErrors] = useState<GroupErrors>({});
    const [savingGroup, setSavingGroup] = useState<string | null>(null);
    const [uploadingKey, setUploadingKey] = useState<string | null>(null);
    const invalidateConfigs = usePublicUiStore((state) => state.invalidateConfigs);
    const fetchPublicConfigs = usePublicUiStore((state) => state.fetchConfigs);

    const load = useCallback(async () => {
        setLoading(true);
        setLoadError(null);
        try {
            const page = await AdminUiService.searchConfigs('', {
                page: 1,
                pageSize: CONFIG_PAGE_SIZE,
                sortBy: 'groupName',
                sortDirection: 'ASC',
            });
            setPageData(page);
            setDraftValues(toDraftValues(page.content || []));
        } catch (loadConfigError) {
            setLoadError(errorMessage(loadConfigError));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void load();
    }, [load]);

    const groupedConfigs = useMemo(
        () => groupConfigsByGroupName((pageData.content || []).filter((config) => isVisibleConfigGroup(config.groupName))),
        [pageData.content]
    );
    const groupNames = useMemo(
        () => sortGroupNames(Object.keys(groupedConfigs)),
        [groupedConfigs]
    );
    const activeConfigs = activeGroup ? groupedConfigs[activeGroup] || [] : [];

    useEffect(() => {
        if (!activeGroup && groupNames.length) {
            setActiveGroup(groupNames[0]);
            return;
        }
        if (activeGroup && groupNames.length && !groupNames.includes(activeGroup)) {
            setActiveGroup(groupNames[0]);
            return;
        }
        if (activeGroup && !groupNames.length) {
            setActiveGroup('');
        }
    }, [activeGroup, groupNames]);

    const updateDraft = (groupName: string, key: string, value: string) => {
        setDraftValues((current) => ({...current, [key]: value}));
        setGroupErrors((current) => ({...current, [groupName]: null}));
    };

    const formatJson = (key: string) => {
        const value = draftValues[key];
        if (!value?.trim()) return;
        try {
            const parsed = JSON.parse(value);
            setDraftValues((current) => ({
                ...current,
                [key]: JSON.stringify(parsed, null, 2),
            }));
        } catch {
            // Validation is shown on save so typing invalid JSON remains possible.
        }
    };

    const validateGroup = (groupName: string) => {
        const configs = groupedConfigs[groupName] || [];
        for (const config of configs) {
            const value = draftValues[config.key] ?? '';
            if (config.type === 'json') {
                try {
                    JSON.parse(value);
                } catch {
                    return `${config.key} phải là JSON hợp lệ.`;
                }
            }
            if (config.type === 'number' && !Number.isFinite(Number(value))) {
                return `${config.key} phải là số hợp lệ.`;
            }
            if (config.type === 'color' && value && !isHexColor(value)) {
                return `${config.key} phải là mã màu hex hợp lệ.`;
            }
            if (config.type === 'image' && !isSupportedUrl(value.trim())) {
                return `${config.key} phải là URL ảnh hợp lệ.`;
            }
        }
        return null;
    };

    const refreshPublic = async () => {
        invalidateConfigs();
        await fetchPublicConfigs(true);
    };

    const saveGroup = async (groupName: string) => {
        const validationError = validateGroup(groupName);
        if (validationError) {
            setGroupErrors((current) => ({...current, [groupName]: validationError}));
            return;
        }

        const configs = groupedConfigs[groupName] || [];
        setSavingGroup(groupName);
        setGroupErrors((current) => ({...current, [groupName]: null}));
        try {
            await AdminUiService.bulkUpdateConfigs(configs.map((config) => ({
                key: config.key,
                value: draftValues[config.key] ?? '',
            })));
            await Promise.all([load(), refreshPublic()]);
            ToastService.success(`Đã lưu nhóm ${getGroupTitle(groupName)}.`);
        } catch (saveError) {
            setGroupErrors((current) => ({...current, [groupName]: errorMessage(saveError)}));
        } finally {
            setSavingGroup(null);
        }
    };

    const uploadImage = async (groupName: string, key: string, file: File) => {
        setUploadingKey(key);
        setGroupErrors((current) => ({...current, [groupName]: null}));
        try {
            const result = await FileService.uploadFile(file, 'configs');
            updateDraft(groupName, key, result.url);
            ToastService.success('Đã upload ảnh cấu hình.');
        } catch (uploadError) {
            setGroupErrors((current) => ({...current, [groupName]: errorMessage(uploadError)}));
        } finally {
            setUploadingKey(null);
        }
    };

    const isGroupDirty = (groupName: string) =>
        (groupedConfigs[groupName] || []).some((config) =>
            (draftValues[config.key] ?? '') !== (config.value ?? '')
        );

    return (
        <div className="space-y-6 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-black uppercase text-on-surface">Cấu hình website</h1>
                    <p className="mt-1 text-xs font-semibold text-outline">
                        Cập nhật các key có sẵn theo từng nhóm. Mỗi tab lưu riêng để tránh ghi đè ngoài phạm vi.
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => void load()}
                    disabled={loading}
                    className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-outline-variant/40 bg-white px-4 py-2.5 text-xs font-black uppercase text-on-surface transition-colors hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {loading ? <Loader2 className="h-4 w-4 animate-spin"/> : <RefreshCw className="h-4 w-4"/>}
                    Tải lại
                </button>
            </div>

            {loadError && (
                <div className="flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0"/>
                    <span>{loadError}</span>
                </div>
            )}

            <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-white shadow-sm">
                <div className="border-b border-outline-variant/20 bg-surface-container-lowest px-4 pt-4">
                    <div className="flex gap-2 overflow-x-auto pb-3">
                        {groupNames.map((groupName) => {
                            const isActive = groupName === activeGroup;
                            const dirty = isGroupDirty(groupName);
                            return (
                                <button
                                    type="button"
                                    key={groupName}
                                    onClick={() => setActiveGroup(groupName)}
                                    className={`flex shrink-0 cursor-pointer items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-black uppercase transition-colors ${
                                        isActive
                                            ? 'bg-primary text-on-primary shadow-sm'
                                            : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                                    }`}
                                >
                                    {getGroupTitle(groupName)}
                                    <span className={`rounded-full px-2 py-0.5 text-[10px] ${
                                        isActive ? 'bg-white/20 text-on-primary' : 'bg-white text-outline'
                                    }`}>
                                        {groupedConfigs[groupName]?.length || 0}
                                    </span>
                                    {dirty && <span className="h-2 w-2 rounded-full bg-amber-400" aria-label="Có thay đổi"/>}
                                </button>
                            );
                        })}
                    </div>
                </div>

                {loading ? (
                    <div className="flex min-h-[360px] items-center justify-center">
                        <Loader2 className="h-9 w-9 animate-spin text-primary"/>
                    </div>
                ) : activeConfigs.length ? (
                    <div className="p-4 sm:p-6">
                        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                            <div>
                                <h2 className="text-lg font-black uppercase text-on-surface">{getGroupTitle(activeGroup)}</h2>
                                <p className="mt-1 text-xs font-semibold text-outline">
                                    {activeConfigs.length} cấu hình trong nhóm {activeGroup}
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => void saveGroup(activeGroup)}
                                disabled={savingGroup === activeGroup || uploadingKey !== null || !isGroupDirty(activeGroup)}
                                className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-xs font-black uppercase text-on-primary shadow-sm transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {savingGroup === activeGroup ? <Loader2 className="h-4 w-4 animate-spin"/> : <Save className="h-4 w-4"/>}
                                Lưu thay đổi
                            </button>
                        </div>

                        {groupErrors[activeGroup] && (
                            <div className="mb-5 flex items-start gap-3 rounded-xl border border-error/20 bg-error/10 p-4 text-xs font-bold text-error">
                                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0"/>
                                <span>{groupErrors[activeGroup]}</span>
                            </div>
                        )}

                        <div className="grid gap-4">
                            {activeConfigs.map((config) => (
                                <section
                                    key={config.key}
                                    className="rounded-2xl border border-outline-variant/30 bg-surface-container-lowest p-4 shadow-sm"
                                >
                                    <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                                        <div className="min-w-0">
                                            <div className="flex flex-wrap items-center gap-2">
                                                <h3 className="break-all font-mono text-sm font-black text-primary">{config.key}</h3>
                                                <span className="rounded-full bg-surface-container px-2.5 py-1 text-[10px] font-black uppercase text-outline">
                                                    {config.type}
                                                </span>
                                                {config.isPublic && (
                                                    <span className="rounded-full bg-green-100 px-2.5 py-1 text-[10px] font-black uppercase text-green-700">
                                                        Public
                                                    </span>
                                                )}
                                            </div>
                                            {config.description && (
                                                <p className="mt-1 text-xs font-semibold leading-relaxed text-outline">
                                                    {config.description}
                                                </p>
                                            )}
                                        </div>
                                        <div className="flex shrink-0 items-center gap-2 text-[10px] font-black uppercase text-outline">
                                            {((draftValues[config.key] ?? '') !== (config.value ?? '')) ? 'Đã sửa' : 'Chưa đổi'}
                                        </div>
                                    </div>

                                    <ConfigField
                                        config={config}
                                        value={draftValues[config.key] ?? ''}
                                        disabled={savingGroup === activeGroup || uploadingKey === config.key}
                                        uploading={uploadingKey === config.key}
                                        onChange={(value) => updateDraft(activeGroup, config.key, value)}
                                        onJsonBlur={() => formatJson(config.key)}
                                        onUpload={(file) => void uploadImage(activeGroup, config.key, file)}
                                    />
                                </section>
                            ))}
                        </div>
                    </div>
                ) : (
                    <div className="flex min-h-[360px] flex-col items-center justify-center p-8 text-center font-bold text-outline">
                        <FileJson className="mb-3 h-10 w-10 opacity-40"/>
                        Không có cấu hình website.
                    </div>
                )}
            </div>

            {pageData.totalPages > 1 && (
                <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs font-bold text-amber-800">
                    Đang tải {pageData.content.length}/{pageData.totalElements} cấu hình. Hãy tăng CONFIG_PAGE_SIZE nếu database vượt quá giới hạn này.
                </div>
            )}
        </div>
    );
}
