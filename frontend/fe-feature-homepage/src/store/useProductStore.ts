import { create } from 'zustand';
import { PublicCatalogService } from '../service/public-catalog.service';
import { ProductListItemDTO } from '../interface/product.model';
import { CategoryListItemDTO } from '../interface/category.model';
import { BrandListItemDTO } from '../interface/brand.model';

interface ProductState {
    products: ProductListItemDTO[];
    categories: CategoryListItemDTO[];
    brands: BrandListItemDTO[];
    isLoading: boolean;
    error: string | null;

    fetchProducts: (params?: any) => Promise<void>;
    fetchCategories: () => Promise<void>;
    fetchBrands: () => Promise<void>;
}

export const useProductStore = create<ProductState>((set) => ({
    products: [],
    categories: [],
    brands: [],
    isLoading: false,
    error: null,

    fetchProducts: async (params) => {
        set({ isLoading: true, error: null });
        try {
            // Note: API returns thẳng data (PageResponse<ProductListItemDTO> or array)
            const response = await PublicCatalogService.getProducts(params);
            
            // getProducts resolves to PageResponse<ProductListItemDTO>
            // We store the array of products from response.content (or response itself if it's an array)
            const productsList = response && 'content' in response 
                ? (response as any).content 
                : (Array.isArray(response) ? response : []);
                
            set({ products: productsList, isLoading: false });
        } catch (err: any) {
            set({ 
                error: err?.message || 'Không thể tải danh sách sản phẩm!', 
                isLoading: false 
            });
        }
    },

    fetchCategories: async () => {
        set({ isLoading: true, error: null });
        try {
            const data = await PublicCatalogService.getCategories();
            set({ categories: data, isLoading: false });
        } catch (err: any) {
            set({ 
                error: err?.message || 'Không thể tải danh mục sản phẩm!', 
                isLoading: false 
            });
        }
    },

    fetchBrands: async () => {
        set({ isLoading: true, error: null });
        try {
            const data = await PublicCatalogService.getBrands();
            set({ brands: data, isLoading: false });
        } catch (err: any) {
            set({ 
                error: err?.message || 'Không thể tải danh sách thương hiệu!', 
                isLoading: false 
            });
        }
    },
}));
