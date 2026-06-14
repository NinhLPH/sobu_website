import { create } from 'zustand';
import { ProductModel } from '../interface/product.model';
import { CategoryModel } from '../interface/category.model';
import { ServiceRequest } from '../interface/service-request';
import {
    AdminOrderQueryParams,
    OrderPaymentResponseDto,
    OrderResponseDto,
    OrderSyncResultDto
} from '../interface/order.model';
import { PageResponse } from '../interface/api-response';
import { AdminWorkflowService } from '../service/admin.service';
import { mockProducts, mockCategories, mockRequests } from '../data/mockData';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || error?.message || fallback;

const upsertPayment = (
    payments: OrderPaymentResponseDto[],
    payment: OrderPaymentResponseDto
) => {
    const existingIndex = payments.findIndex(item => item.id === payment.id);
    if (existingIndex < 0) {
        return [...payments, payment];
    }
    return payments.map(item => item.id === payment.id ? payment : item);
};

interface AdminState {
    products: ProductModel[];
    categories: CategoryModel[];
    requests: ServiceRequest[];
    workflowOrders: OrderResponseDto[];
    currentOrderDetail: OrderResponseDto | null;
    adminPayments: OrderPaymentResponseDto[];
    ordersPage: Omit<PageResponse<OrderResponseDto>, 'content'>;
    isOrdersLoading: boolean;
    isOrderDetailLoading: boolean;
    isRetryingOrderSync: boolean;
    isCreatingFinalPayment: boolean;
    confirmingPaymentCode: string | null;
    ordersError: string | null;
    orderActionMessage: string | null;

    addProduct: (product: ProductModel) => void;
    updateProduct: (id: string, updates: Partial<ProductModel>) => void;
    deleteProduct: (id: string) => void;
    addCategory: (category: CategoryModel) => void;
    updateCategory: (id: string, updates: Partial<CategoryModel>) => void;
    deleteCategory: (id: string) => void;
    updateRequest: (id: string, updates: Partial<ServiceRequest>) => void;
    fetchOrders: (params?: AdminOrderQueryParams) => Promise<void>;
    fetchOrderDetail: (id: string | number) => Promise<void>;
    retryOrderSync: (id: string | number) => Promise<OrderSyncResultDto>;
    createPreorderFinalPayment: (id: string | number) => Promise<OrderPaymentResponseDto>;
    confirmMockPayment: (paymentCode: string) => Promise<OrderPaymentResponseDto>;
    clearOrdersError: () => void;
    clearOrderActionMessage: () => void;
    clearCurrentOrder: () => void;
}

export const useAdminStore = create<AdminState>((set) => ({
    products: mockProducts,
    categories: mockCategories,
    requests: mockRequests,
    workflowOrders: [],
    currentOrderDetail: null,
    adminPayments: [],
    ordersPage: {
        pageNumber: 0,
        pageSize: 10,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false
    },
    isOrdersLoading: false,
    isOrderDetailLoading: false,
    isRetryingOrderSync: false,
    isCreatingFinalPayment: false,
    confirmingPaymentCode: null,
    ordersError: null,
    orderActionMessage: null,

    addProduct: (product) => set((state) => ({
        products: [...state.products, product]
    })),

    updateProduct: (id, updates) => set((state) => ({
        products: state.products.map(product =>
            product.id === id ? { ...product, ...updates } : product
        )
    })),

    deleteProduct: (id) => set((state) => ({
        products: state.products.filter(product => product.id !== id)
    })),

    addCategory: (category) => set((state) => {
        if (category.parentId) {
            const addNested = (categories: CategoryModel[]): CategoryModel[] =>
                categories.map(current =>
                    current.id === category.parentId
                        ? { ...current, children: [...(current.children || []), category] }
                        : {
                            ...current,
                            children: current.children
                                ? addNested(current.children)
                                : current.children
                        }
                );
            return { categories: addNested(state.categories) };
        }
        return { categories: [...state.categories, category] };
    }),

    updateCategory: (id, updates) => set((state) => {
        const updateNested = (categories: CategoryModel[]): CategoryModel[] =>
            categories.map(current =>
                current.id === id
                    ? { ...current, ...updates }
                    : {
                        ...current,
                        children: current.children
                            ? updateNested(current.children)
                            : current.children
                    }
            );
        return { categories: updateNested(state.categories) };
    }),

    deleteCategory: (id) => set((state) => {
        const deleteNested = (categories: CategoryModel[]): CategoryModel[] =>
            categories
                .filter(current => current.id !== id)
                .map(current => ({
                    ...current,
                    children: current.children
                        ? deleteNested(current.children)
                        : current.children
                }));
        return { categories: deleteNested(state.categories) };
    }),

    updateRequest: (id, updates) => set((state) => ({
        requests: state.requests.map(request =>
            request.id === id ? { ...request, ...updates } : request
        )
    })),

    fetchOrders: async (params) => {
        set({ isOrdersLoading: true, ordersError: null });
        try {
            const response = await AdminWorkflowService.getAdminOrders(params);
            const data = response.data;
            const pageNumber = data.pageNumber ?? params?.page ?? 0;
            const pageSize = data.pageSize ?? params?.size ?? 10;
            const totalElements = data.totalElements ?? data.content?.length ?? 0;
            const totalPages = data.totalPages ?? (totalElements > 0 ? 1 : 0);

            set({
                workflowOrders: data.content ?? [],
                ordersPage: {
                    pageNumber,
                    pageSize,
                    totalElements,
                    totalPages,
                    first: data.first ?? pageNumber === 0,
                    last: data.last ?? pageNumber + 1 >= totalPages,
                    hasNext: data.hasNext ?? pageNumber + 1 < totalPages,
                    hasPrevious: data.hasPrevious ?? pageNumber > 0
                },
                isOrdersLoading: false
            });
        } catch (error) {
            set({
                ordersError: getErrorMessage(error, 'Không thể tải danh sách đơn hàng.'),
                isOrdersLoading: false
            });
        }
    },

    fetchOrderDetail: async (id) => {
        set({
            isOrderDetailLoading: true,
            ordersError: null,
            orderActionMessage: null,
            currentOrderDetail: null,
            adminPayments: []
        });
        try {
            const response = await AdminWorkflowService.getAdminOrderDetail(id);
            set({
                currentOrderDetail: response.data,
                isOrderDetailLoading: false
            });
        } catch (error) {
            set({
                ordersError: getErrorMessage(error, 'Không thể tải chi tiết đơn hàng.'),
                isOrderDetailLoading: false
            });
        }
    },

    retryOrderSync: async (id) => {
        set({
            isRetryingOrderSync: true,
            ordersError: null,
            orderActionMessage: null
        });
        try {
            const response = await AdminWorkflowService.retryOrderSync(id);
            const result = response.data;
            set((state) => ({
                currentOrderDetail: state.currentOrderDetail
                    ? {
                        ...state.currentOrderDetail,
                        id: result.orderId,
                        orderCode: result.orderCode ?? state.currentOrderDetail.orderCode,
                        syncStatus: result.syncStatus,
                        nhanhSyncStage: result.nhanhSyncStage,
                        nhanhOrderId: result.nhanhOrderId,
                        nhanhOrderCode: result.nhanhOrderCode,
                        syncError: result.syncError,
                        lastSyncMessage: result.lastSyncMessage,
                        lastSyncAt: result.lastSyncAt
                    }
                    : null,
                isRetryingOrderSync: false,
                orderActionMessage: response.message || 'Đã thử đồng bộ lại đơn hàng.'
            }));
            return result;
        } catch (error) {
            set({
                ordersError: getErrorMessage(error, 'Không thể đồng bộ lại đơn hàng.'),
                isRetryingOrderSync: false
            });
            throw error;
        }
    },

    createPreorderFinalPayment: async (id) => {
        set({
            isCreatingFinalPayment: true,
            ordersError: null,
            orderActionMessage: null
        });
        try {
            const response = await AdminWorkflowService.createPreorderFinalPayment(id);
            set((state) => ({
                adminPayments: upsertPayment(state.adminPayments, response.data),
                isCreatingFinalPayment: false,
                orderActionMessage: response.message || 'Đã tạo thanh toán đợt cuối.'
            }));
            try {
                const detailResponse = await AdminWorkflowService.getAdminOrderDetail(id);
                set({ currentOrderDetail: detailResponse.data });
            } catch (refreshError) {
                set({
                    ordersError: getErrorMessage(
                        refreshError,
                        'Đã tạo thanh toán nhưng chưa thể làm mới chi tiết đơn hàng.'
                    )
                });
            }
            return response.data;
        } catch (error) {
            set({
                ordersError: getErrorMessage(error, 'Không thể tạo thanh toán đợt cuối.'),
                isCreatingFinalPayment: false
            });
            throw error;
        }
    },

    confirmMockPayment: async (paymentCode) => {
        const normalizedCode = paymentCode.trim();
        set({
            confirmingPaymentCode: normalizedCode,
            ordersError: null,
            orderActionMessage: null
        });
        try {
            const response = await AdminWorkflowService.confirmMockPayment(normalizedCode);
            const orderId = response.data.orderId;
            set((state) => ({
                adminPayments: upsertPayment(state.adminPayments, response.data),
                confirmingPaymentCode: null,
                orderActionMessage: response.message || 'Đã xác nhận thanh toán.'
            }));
            try {
                const detailResponse = await AdminWorkflowService.getAdminOrderDetail(orderId);
                set({ currentOrderDetail: detailResponse.data });
            } catch (refreshError) {
                set({
                    ordersError: getErrorMessage(
                        refreshError,
                        'Đã xác nhận thanh toán nhưng chưa thể làm mới chi tiết đơn hàng.'
                    )
                });
            }
            return response.data;
        } catch (error) {
            set({
                ordersError: getErrorMessage(error, 'Không thể xác nhận thanh toán.'),
                confirmingPaymentCode: null
            });
            throw error;
        }
    },

    clearOrdersError: () => set({ ordersError: null }),
    clearOrderActionMessage: () => set({ orderActionMessage: null }),
    clearCurrentOrder: () => set({
        currentOrderDetail: null,
        adminPayments: [],
        ordersError: null,
        orderActionMessage: null,
        isCreatingFinalPayment: false,
        confirmingPaymentCode: null
    })
}));
