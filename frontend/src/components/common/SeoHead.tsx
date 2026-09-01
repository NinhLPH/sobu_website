import {useEffect, useMemo} from 'react';
import {SeoMetadataDTO} from '../../interface/seo.model';
import {usePublicUiStore} from '../../store/usePublicUiStore';
import {getPublicImageUrl} from '../../utils/file-url';

type StructuredData = Record<string, unknown> | Array<Record<string, unknown>>;

type SeoHeadProps = {
    title?: string | null;
    description?: string | null;
    canonicalPath?: string;
    image?: string | null;
    type?: string;
    metadata?: SeoMetadataDTO | null;
    keywords?: string | null;
    noIndex?: boolean;
    structuredData?: StructuredData | null;
};

const absoluteUrl = (value?: string | null): string | undefined => {
    if (!value) return undefined;
    if (/^https?:\/\//i.test(value)) return value;
    if (typeof window === 'undefined') return value;
    return new URL(getPublicImageUrl(value), window.location.origin).toString();
};

const upsertMeta = (key: 'name' | 'property', value: string, content?: string) => {
    const selector = `meta[${key}="${value}"]`;
    let element = document.head.querySelector<HTMLMetaElement>(selector);
    if (!content) {
        element?.remove();
        return;
    }
    if (!element) {
        element = document.createElement('meta');
        element.setAttribute(key, value);
        document.head.appendChild(element);
    }
    element.dataset.sobuSeo = 'true';
    element.content = content;
};

export default function SeoHead({
                                    title,
                                    description,
                                    canonicalPath,
                                    image,
                                    type = 'website',
                                    metadata,
                                    keywords,
                                    noIndex = false,
                                    structuredData,
                                }: SeoHeadProps) {
    const configMap = usePublicUiStore((state) => state.configMap);
    const resolved = useMemo(() => {
        const siteName = configMap.site_name || 'SOBU';
        const resolvedTitle = metadata?.seoTitle?.trim() || title?.trim() || configMap.seo_default_title || siteName;
        const resolvedDescription = metadata?.metaDescription?.trim() || description?.trim() || configMap.seo_default_description || 'SOBU';
        const path = canonicalPath || (typeof window === 'undefined' ? '/' : window.location.pathname);
        const fallbackCanonical = typeof window === 'undefined' ? path : new URL(path, window.location.origin).toString();
        return {
            siteName,
            title: resolvedTitle,
            description: resolvedDescription,
            canonical: metadata?.canonicalUrl?.trim() || fallbackCanonical,
            robots: noIndex ? 'noindex, nofollow' : metadata?.robots?.trim() || (configMap.seo_robots_index_enabled === 'false' ? 'noindex, nofollow' : 'index, follow'),
            ogTitle: metadata?.ogTitle?.trim() || configMap.seo_og_title || resolvedTitle,
            ogDescription: metadata?.ogDescription?.trim() || configMap.seo_og_description || resolvedDescription,
            image: absoluteUrl(metadata?.ogImage || image || configMap.seo_og_image),
            type: metadata?.ogType?.trim() || configMap.seo_og_type || type,
            keywords: keywords?.trim() || configMap.seo_default_keywords,
        };
    }, [canonicalPath, configMap, description, image, keywords, metadata, noIndex, title, type]);

    useEffect(() => {
        document.title = resolved.title;
        upsertMeta('name', 'description', resolved.description);
        upsertMeta('name', 'keywords', resolved.keywords);
        upsertMeta('name', 'robots', resolved.robots);
        upsertMeta('property', 'og:title', resolved.ogTitle);
        upsertMeta('property', 'og:description', resolved.ogDescription);
        upsertMeta('property', 'og:type', resolved.type);
        upsertMeta('property', 'og:url', resolved.canonical);
        upsertMeta('property', 'og:site_name', resolved.siteName);
        upsertMeta('property', 'og:image', resolved.image);
        upsertMeta('name', 'twitter:card', resolved.image ? 'summary_large_image' : 'summary');
        upsertMeta('name', 'twitter:title', resolved.ogTitle);
        upsertMeta('name', 'twitter:description', resolved.ogDescription);
        upsertMeta('name', 'twitter:image', resolved.image);

        let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
        if (!canonical) {
            canonical = document.createElement('link');
            canonical.rel = 'canonical';
            document.head.appendChild(canonical);
        }
        canonical.dataset.sobuSeo = 'true';
        canonical.href = resolved.canonical;

        const scriptId = 'sobu-structured-data';
        document.getElementById(scriptId)?.remove();
        if (structuredData) {
            const script = document.createElement('script');
            script.id = scriptId;
            script.type = 'application/ld+json';
            script.text = JSON.stringify(structuredData).replace(/</g, '\\u003c');
            document.head.appendChild(script);
        }
    }, [resolved, structuredData]);

    return null;
}
