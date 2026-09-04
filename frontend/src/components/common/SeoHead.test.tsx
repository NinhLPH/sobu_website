import {beforeEach, describe, expect, it} from '@jest/globals';
import {render, waitFor} from '@testing-library/react';
import SeoHead from './SeoHead';
import {usePublicUiStore} from '../../store/usePublicUiStore';

describe('SeoHead', () => {
    beforeEach(() => {
        document.head.innerHTML = '';
        window.history.replaceState({}, '', '/san-pham');
        usePublicUiStore.setState({
            configMap: {
                site_name: 'SOBU',
                seo_default_title: 'SEO mặc định',
                seo_default_description: 'Mô tả mặc định',
                seo_og_image: '/uploads/default.jpg',
            },
        });
    });

    it('applies overrides and keeps a single canonical and structured-data tag', async () => {
        const {rerender} = render(<SeoHead title="Sản phẩm" description="Mô tả sản phẩm" canonicalPath="/product/mo-hinh-a" structuredData={{'@context': 'https://schema.org', '@type': 'Product'}}/>);

        await waitFor(() => expect(document.title).toBe('Sản phẩm'));
        expect(document.querySelector('meta[name="description"]')?.getAttribute('content')).toBe('Mô tả sản phẩm');
        expect(document.querySelector('meta[property="og:title"]')?.getAttribute('content')).toBe('Sản phẩm');
        expect(document.querySelectorAll('link[rel="canonical"]')).toHaveLength(1);
        expect(document.querySelectorAll('#sobu-structured-data')).toHaveLength(1);

        rerender(<SeoHead metadata={{seoTitle: 'Tiêu đề backend', metaDescription: 'Mô tả backend'}} canonicalPath="/product/mo-hinh-b" noIndex/>);
        await waitFor(() => expect(document.title).toBe('Tiêu đề backend'));
        expect(document.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe('noindex, nofollow');
        expect(document.querySelectorAll('link[rel="canonical"]')).toHaveLength(1);
        expect(document.querySelector('#sobu-structured-data')).toBeNull();
    });

    it('falls back to public website configuration', async () => {
        render(<SeoHead/>);
        await waitFor(() => expect(document.title).toBe('SEO mặc định'));
        expect(document.querySelector('meta[name="description"]')?.getAttribute('content')).toBe('Mô tả mặc định');
        expect(document.querySelector('meta[name="twitter:card"]')?.getAttribute('content')).toBe('summary_large_image');
    });
});
