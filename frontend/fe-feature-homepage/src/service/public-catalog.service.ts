import apiClient from "../api/api-client";
import {PageResponse} from "../interface/api-response";
import {ProductDetailDTO, ProductListItemDTO} from "../interface/product.model";
import {CategoryListItemDTO} from "../interface/category.model";
import {BrandListItemDTO} from "../interface/brand.model";


export const PublicCatalogService = {
    getProducts: (params?: any): Promise<PageResponse<ProductListItemDTO>> => {
        return apiClient.get('/api/public/products', { params });
    },

    getAllProducts: (): Promise<ProductListItemDTO[]> => {
        return apiClient.get('/api/public/products/all');
    },

    getProductDetail: (id: string | number): Promise<ProductDetailDTO> => {
        return apiClient.get(`/api/public/products/${id}`);
    },

    searchProducts: (q: string, params?: any): Promise<PageResponse<ProductListItemDTO>> => {
        return apiClient.get('/api/public/products/search', { params: { q, ...params } });
    },

    searchProductsPost: (filterBody: any): Promise<PageResponse<ProductListItemDTO>> => {
        return apiClient.post('/api/public/products/search', filterBody);
    },

    getCategories: (): Promise<CategoryListItemDTO[]> => {
        return apiClient.get('/api/public/categories');
    },

    getBrands: (): Promise<BrandListItemDTO[]> => {
        return apiClient.get('/api/public/brands');
    },
};