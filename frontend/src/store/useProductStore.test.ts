import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { PublicCatalogService } from '../service/public-catalog.service';
import { AdminSyncService } from '../service/sync.service';
import { useProductStore } from './useProductStore';

jest.mock('../service/public-catalog.service');
jest.mock('../service/sync.service');

const mockedCatalogService = jest.mocked(PublicCatalogService);
const mockedSyncService = jest.mocked(AdminSyncService);

describe('useProductStore request guards', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useProductStore.setState({
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
            error: null
        });
    });

    it('caches an empty product page instead of fetching it repeatedly', async () => {
        mockedCatalogService.getProducts.mockResolvedValue({
            content: [],
            pageNumber: 0,
            pageSize: 20,
            totalElements: 0,
            totalPages: 0
        });

        await useProductStore.getState().fetchProducts();
        await useProductStore.getState().fetchProducts();

        expect(mockedCatalogService.getProducts).toHaveBeenCalledTimes(1);
        expect(useProductStore.getState().productsLoaded).toBe(true);
    });

    it('loads the complete product catalog once for request selectors', async () => {
        mockedCatalogService.getAllProducts.mockResolvedValue([]);

        await useProductStore.getState().fetchAllProducts();
        await useProductStore.getState().fetchAllProducts();

        expect(mockedCatalogService.getAllProducts).toHaveBeenCalledTimes(1);
        expect(useProductStore.getState().allProductsLoaded).toBe(true);
    });

    it('deduplicates concurrent category requests', async () => {
        let resolveRequest: ((categories: []) => void) | undefined;
        mockedCatalogService.getCategories.mockImplementation(() => (
            new Promise((resolve) => {
                resolveRequest = resolve;
            })
        ));

        const firstRequest = useProductStore.getState().fetchCategories();
        const secondRequest = useProductStore.getState().fetchCategories();
        resolveRequest?.([]);
        await Promise.all([firstRequest, secondRequest]);

        expect(mockedCatalogService.getCategories).toHaveBeenCalledTimes(1);
        expect(useProductStore.getState().categoriesLoaded).toBe(true);
    });

    it('refreshes products after a successful admin sync', async () => {
        mockedSyncService.syncProducts.mockResolvedValue({ message: 'Sync success' });
        mockedCatalogService.getProducts.mockResolvedValue({
            content: [],
            pageNumber: 0,
            pageSize: 20,
            totalElements: 0,
            totalPages: 0
        });

        const response = await useProductStore.getState().triggerSyncProducts();

        expect(response).toEqual({ message: 'Sync success' });
        expect(mockedSyncService.syncProducts).toHaveBeenCalledTimes(1);
        expect(mockedCatalogService.getProducts).toHaveBeenCalledTimes(1);
        expect(useProductStore.getState().isSyncingProducts).toBe(false);
        expect(useProductStore.getState().productsLoaded).toBe(true);
        expect(useProductStore.getState().allProductsLoaded).toBe(false);
    });

    it('keeps the latest forced product filter response when requests overlap', async () => {
        let resolveFirst: ((value: any) => void) | undefined;
        mockedCatalogService.getProducts
            .mockImplementationOnce(() => new Promise(resolve => { resolveFirst = resolve; }))
            .mockResolvedValueOnce({
                content: [{id: 2, name: 'Latest sale result', price: 80, oldPrice: 100}],
                pageNumber: 0,
                pageSize: 20,
                totalElements: 1,
                totalPages: 1
            });

        const first = useProductStore.getState().fetchProducts({sortBy: 'createdAt'}, true);
        const second = useProductStore.getState().fetchProducts({onSale: true}, true);
        await second;
        resolveFirst?.({
            content: [{id: 1, name: 'Stale result', price: 100}],
            pageNumber: 0,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1
        });
        await first;

        expect(useProductStore.getState().products.map(product => product.id)).toEqual([2]);
    });

    it('exposes sync errors and clears the loading state', async () => {
        mockedSyncService.syncBrands.mockRejectedValue(new Error('Sync failed'));

        await expect(
            useProductStore.getState().triggerSyncBrands()
        ).rejects.toThrow('Sync failed');

        expect(mockedCatalogService.getBrands).not.toHaveBeenCalled();
        expect(useProductStore.getState().isSyncingBrands).toBe(false);
        expect(useProductStore.getState().error).toBe('Sync failed');
    });
});
