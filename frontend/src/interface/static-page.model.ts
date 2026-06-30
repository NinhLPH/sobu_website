export interface StaticPageDTO {
    id: number;
    slug: string;
    title: string;
    htmlContent: string;
    isPublished: boolean;
    createdAt?: string;
    updatedAt?: string;
}

export interface StaticPageMutationPayload {
    slug: string;
    title: string;
    htmlContent: string;
    isPublished: boolean;
}
