import apiClient from '../api/api-client';
import { PageResponse } from '../interface/api-response';
import { ProductDetailDTO, ProductListItemDTO } from '../interface/product.model';
import { CategoryListItemDTO } from '../interface/category.model';
import { BrandListItemDTO } from '../interface/brand.model';

type CatalogParams = Record<string, string | number | boolean | undefined>;
type ProductSearchBody = Record<string, unknown>;

const inFlightRequests = new Map<string, Promise<unknown>>();

const serializeRecord = (value?: Record<string, unknown>) => JSON.stringify(
    Object.entries(value || {})
        .filter(([, item]) => item !== undefined)
        .sort(([left], [right]) => left.localeCompare(right))
);

const dedupeRequest = <T>(key: string, request: () => Promise<T>): Promise<T> => {
    const existingRequest = inFlightRequests.get(key) as Promise<T> | undefined;
    if (existingRequest) {
        return existingRequest;
    }

    const pendingRequest = request().finally(() => {
        inFlightRequests.delete(key);
    });
    inFlightRequests.set(key, pendingRequest);
    return pendingRequest;
};

export const PublicCatalogService = {
    getProducts: (params?: CatalogParams): Promise<PageResponse<ProductListItemDTO>> => {
        return dedupeRequest(
            `products:${serializeRecord(params)}`,
            () => apiClient.get('/api/public/products', { params })
        );
    },

    getAllProducts: (): Promise<ProductListItemDTO[]> => {
        return dedupeRequest(
            'products:all',
            () => apiClient.get('/api/public/products/all')
        );
    },

    getProductDetail: (id: string | number): Promise<ProductDetailDTO> => {
        return dedupeRequest(
            `products:detail:${id}`,
            () => apiClient.get(`/api/public/products/${id}`)
        );
    },

    searchProducts: (
        q: string,
        params?: CatalogParams
    ): Promise<PageResponse<ProductListItemDTO>> => {
        const searchParams = { ...params, q };
        return dedupeRequest(
            `products:search:${serializeRecord(searchParams)}`,
            () => apiClient.get('/api/public/products/search', {
                params: searchParams
            })
        );
    },

    searchProductsPost: (
        filterBody: ProductSearchBody
    ): Promise<PageResponse<ProductListItemDTO>> => {
        return dedupeRequest(
            `products:search-post:${serializeRecord(filterBody)}`,
            () => apiClient.post('/api/public/products/search', filterBody)
        );
    },

    getCategories: (): Promise<CategoryListItemDTO[]> => {
        return dedupeRequest(
            'categories',
            () => apiClient.get('/api/public/categories')
        );
    },

    getBrands: (): Promise<BrandListItemDTO[]> => {
        return dedupeRequest(
            'brands',
            () => apiClient.get('/api/public/brands')
        );
    }
};
