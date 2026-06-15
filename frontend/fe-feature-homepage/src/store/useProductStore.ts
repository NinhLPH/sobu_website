import { create } from 'zustand';
import { PublicCatalogService } from '../service/public-catalog.service';
import { ProductListItemDTO } from '../interface/product.model';
import { CategoryListItemDTO } from '../interface/category.model';
import { BrandListItemDTO } from '../interface/brand.model';
import { AdminSyncService } from '../service/sync.service';

type CatalogParams = Record<string, string | number | boolean | undefined>;
type SyncResponse = { message: string };

interface ProductState {
    products: ProductListItemDTO[];
    allProducts: ProductListItemDTO[];
    categories: CategoryListItemDTO[];
    brands: BrandListItemDTO[];
    productsLoaded: boolean;
    allProductsLoaded: boolean;
    categoriesLoaded: boolean;
    brandsLoaded: boolean;
    isProductsLoading: boolean;
    isAllProductsLoading: boolean;
    isCategoriesLoading: boolean;
    isBrandsLoading: boolean;
    isSyncingProducts: boolean;
    isSyncingCategories: boolean;
    isSyncingBrands: boolean;
    isLoading: boolean;
    error: string | null;

    fetchProducts: (params?: CatalogParams, force?: boolean) => Promise<void>;
    fetchAllProducts: (force?: boolean) => Promise<void>;
    fetchCategories: (force?: boolean) => Promise<void>;
    fetchBrands: (force?: boolean) => Promise<void>;
    triggerSyncProducts: () => Promise<SyncResponse>;
    triggerSyncCategories: () => Promise<SyncResponse>;
    triggerSyncBrands: () => Promise<SyncResponse>;
}

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || error?.message || fallback;

export const useProductStore = create<ProductState>((set, get) => ({
    products: [],
    allProducts: [],
    categories: [],
    brands: [],
    productsLoaded: false,
    allProductsLoaded: false,
    categoriesLoaded: false,
    brandsLoaded: false,
    isProductsLoading: false,
    isAllProductsLoading: false,
    isCategoriesLoading: false,
    isBrandsLoading: false,
    isSyncingProducts: false,
    isSyncingCategories: false,
    isSyncingBrands: false,
    isLoading: false,
    error: null,

    fetchProducts: async (params, force = false) => {
        const state = get();
        if (state.isProductsLoading || (!force && state.productsLoaded)) {
            return;
        }

        set({ isProductsLoading: true, isLoading: true, error: null });
        try {
            const response = await PublicCatalogService.getProducts(params);
            set({
                products: response.content ?? [],
                productsLoaded: true,
                isProductsLoading: false,
                isLoading: get().isCategoriesLoading || get().isBrandsLoading
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải danh sách sản phẩm.'),
                isProductsLoading: false,
                isLoading: get().isCategoriesLoading || get().isBrandsLoading
            });
        }
    },

    fetchAllProducts: async (force = false) => {
        const state = get();
        if (state.isAllProductsLoading || (!force && state.allProductsLoaded)) {
            return;
        }

        set({ isAllProductsLoading: true, error: null });
        try {
            const allProducts = await PublicCatalogService.getAllProducts();
            set({
                allProducts: allProducts ?? [],
                allProductsLoaded: true,
                isAllProductsLoading: false
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải toàn bộ catalog sản phẩm.'),
                isAllProductsLoading: false
            });
        }
    },

    fetchCategories: async (force = false) => {
        const state = get();
        if (state.isCategoriesLoading || (!force && state.categoriesLoaded)) {
            return;
        }

        set({ isCategoriesLoading: true, isLoading: true, error: null });
        try {
            const categories = await PublicCatalogService.getCategories();
            set({
                categories: categories ?? [],
                categoriesLoaded: true,
                isCategoriesLoading: false,
                isLoading: get().isProductsLoading || get().isBrandsLoading
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải danh mục sản phẩm.'),
                isCategoriesLoading: false,
                isLoading: get().isProductsLoading || get().isBrandsLoading
            });
        }
    },

    fetchBrands: async (force = false) => {
        const state = get();
        if (state.isBrandsLoading || (!force && state.brandsLoaded)) {
            return;
        }

        set({ isBrandsLoading: true, isLoading: true, error: null });
        try {
            const brands = await PublicCatalogService.getBrands();
            set({
                brands: brands ?? [],
                brandsLoaded: true,
                isBrandsLoading: false,
                isLoading: get().isProductsLoading || get().isCategoriesLoading
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải danh sách thương hiệu.'),
                isBrandsLoading: false,
                isLoading: get().isProductsLoading || get().isCategoriesLoading
            });
        }
    },

    triggerSyncProducts: async () => {
        set({ isSyncingProducts: true, error: null });
        try {
            const response = await AdminSyncService.syncProducts();
            await get().fetchProducts(undefined, true);
            set({ allProductsLoaded: false });
            return response;
        } catch (error) {
            set({ error: getErrorMessage(error, 'Không thể đồng bộ sản phẩm.') });
            throw error;
        } finally {
            set({ isSyncingProducts: false });
        }
    },

    triggerSyncCategories: async () => {
        set({ isSyncingCategories: true, error: null });
        try {
            const response = await AdminSyncService.syncCategories();
            await get().fetchCategories(true);
            return response;
        } catch (error) {
            set({ error: getErrorMessage(error, 'Không thể đồng bộ danh mục.') });
            throw error;
        } finally {
            set({ isSyncingCategories: false });
        }
    },

    triggerSyncBrands: async () => {
        set({ isSyncingBrands: true, error: null });
        try {
            const response = await AdminSyncService.syncBrands();
            await get().fetchBrands(true);
            return response;
        } catch (error) {
            set({ error: getErrorMessage(error, 'Không thể đồng bộ thương hiệu.') });
            throw error;
        } finally {
            set({ isSyncingBrands: false });
        }
    }
}));
