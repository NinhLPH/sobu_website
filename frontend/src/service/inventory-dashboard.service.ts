import {AdminProductListItem} from '../interface/admin-catalog.model';
import {AdminCatalogService} from './admin-catalog.service';
import {PublicUiService} from './public-ui.service';

export const DEFAULT_LOW_STOCK_THRESHOLD = 5;
const LOW_STOCK_PAGE_SIZE = 100;

export interface LowStockOverview {
    threshold: number;
    products: AdminProductListItem[];
}

export const inventoryQuantity = (product: AdminProductListItem): number => {
    const value = product.stockAvailable ?? product.stockRemain ?? 0;
    return Number.isFinite(Number(value)) ? Number(value) : 0;
};

export const parseLowStockThreshold = (value: unknown): number => {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : DEFAULT_LOW_STOCK_THRESHOLD;
};

const throwIfAborted = (signal?: AbortSignal) => {
    if (signal?.aborted) {
        throw new DOMException('The request was aborted.', 'AbortError');
    }
};

export const InventoryDashboardService = {
    getLowStockThreshold: async (signal?: AbortSignal): Promise<number> => {
        try {
            const config = await PublicUiService.getConfigByKey('business_low_stock_threshold', signal);
            throwIfAborted(signal);
            return parseLowStockThreshold(config?.value);
        } catch (error) {
            throwIfAborted(signal);
            return DEFAULT_LOW_STOCK_THRESHOLD;
        }
    },

    getLowStockProducts: async (
        threshold: number,
        signal?: AbortSignal
    ): Promise<AdminProductListItem[]> => {
        const products: AdminProductListItem[] = [];
        let page = 0;
        let totalPages = 1;

        while (page < totalPages) {
            throwIfAborted(signal);
            const response = await AdminCatalogService.getProducts({
                page,
                pageSize: LOW_STOCK_PAGE_SIZE,
                active: true,
                sortBy: 'stockAvailable',
                sortDirection: 'ASC'
            }, signal);
            throwIfAborted(signal);

            const content = response.content ?? [];
            products.push(...content.filter(product => inventoryQuantity(product) <= threshold));
            totalPages = Math.max(response.totalPages ?? 0, 1);

            if (content.some(product => inventoryQuantity(product) > threshold)) break;
            page += 1;
        }

        return products;
    },

    getOverview: async (signal?: AbortSignal): Promise<LowStockOverview> => {
        const threshold = await InventoryDashboardService.getLowStockThreshold(signal);
        const products = await InventoryDashboardService.getLowStockProducts(threshold, signal);
        return {threshold, products};
    }
};
