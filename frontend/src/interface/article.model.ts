import {SeoMetadataDTO} from './seo.model';

export interface ArticleDTO {
    id: number;
    title: string;
    slug: string;
    thumbnailUrl?: string | null;
    thumbnailAlt?: string | null;
    excerpt?: string | null;
    authorName?: string | null;
    category?: string | null;
    status: string;
    publishedAt?: string | null;
    updatedAt?: string | null;
    seo?: SeoMetadataDTO | null;
}

export interface ArticleDetailDTO extends ArticleDTO {
    seoTitle?: string | null;
    metaDescription?: string | null;
    canonicalUrl?: string | null;
    content?: string | null;
    createdAt?: string | null;
}

export type ArticleMutationPayload = {
    title: string;
    slug?: string;
    seoTitle?: string;
    metaDescription?: string;
    canonicalUrl?: string;
    thumbnailUrl?: string;
    thumbnailAlt?: string;
    excerpt?: string;
    content?: string;
    authorName?: string;
    category?: string;
    status: 'PUBLISHED';
};

