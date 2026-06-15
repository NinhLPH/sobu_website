import { create } from 'zustand';
import { CustomerService } from '../service/custom.service';
import { AdminWorkflowService } from '../service/admin.service';
import {
    CreateRequestDto,
    ProcessRequestDto,
    RequestResponseDto,
    UpdateRequestDto
} from '../interface/customer-request.model';
import { PageResponse } from '../interface/api-response';
import { createIdempotencyKey } from '../utils/idempotency';

type RequestRole = 'user' | 'admin';
type RequestPage = Omit<PageResponse<RequestResponseDto>, 'content'>;

const emptyPage: RequestPage = {
    pageNumber: 0,
    pageSize: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false
};

const getErrorMessage = (error: any, fallback: string) => {
    const backendMessage = error?.response?.data?.message || error?.response?.data?.error;
    if (backendMessage) {
        return backendMessage;
    }
    if (error?.response?.status === 409) {
        return 'Yêu cầu đã bị khóa hoặc trạng thái chuyển đổi không còn hợp lệ.';
    }
    return error?.message || fallback;
};

interface RequestState {
    myRequests: RequestResponseDto[];
    adminRequests: RequestResponseDto[];
    myRequestsPage: RequestPage;
    adminRequestsPage: RequestPage;
    currentRequestDetail: RequestResponseDto | null;
    isLoading: boolean;
    isSubmitting: boolean;
    error: string | null;
    pendingRequestKey: string | null;
    pendingRequestFingerprint: string | null;

    fetchMyRequests: (params?: Record<string, unknown>) => Promise<void>;
    fetchAdminRequests: (params?: Record<string, unknown>) => Promise<void>;
    getRequestDetail: (id: string | number, role: RequestRole) => Promise<void>;
    createRequestAction: (
        data: CreateRequestDto
    ) => Promise<RequestResponseDto>;
    updateRequestAction: (
        id: string | number,
        data: UpdateRequestDto,
        role?: RequestRole
    ) => Promise<RequestResponseDto>;
    processRequestAction: (
        id: string | number,
        data: ProcessRequestDto
    ) => Promise<RequestResponseDto>;
    clearError: () => void;
    clearCurrentDetail: () => void;
}

export const useRequestStore = create<RequestState>((set, get) => ({
    myRequests: [],
    adminRequests: [],
    myRequestsPage: emptyPage,
    adminRequestsPage: emptyPage,
    currentRequestDetail: null,
    isLoading: false,
    isSubmitting: false,
    error: null,
    pendingRequestKey: null,
    pendingRequestFingerprint: null,

    fetchMyRequests: async (params) => {
        set({ isLoading: true, error: null });
        try {
            const response = await CustomerService.getMyRequests(params);
            const { content, ...page } = response.data;
            set({
                myRequests: content,
                myRequestsPage: {
                    ...page,
                    first: page.first ?? page.pageNumber === 0,
                    last: page.last ?? page.pageNumber + 1 >= page.totalPages,
                    hasNext: page.hasNext ?? page.pageNumber + 1 < page.totalPages,
                    hasPrevious: page.hasPrevious ?? page.pageNumber > 0
                },
                isLoading: false
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải danh sách yêu cầu của bạn.'),
                isLoading: false
            });
        }
    },

    fetchAdminRequests: async (params) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AdminWorkflowService.getAdminRequests(params);
            const { content, ...page } = response.data;
            set({
                adminRequests: content,
                adminRequestsPage: {
                    ...page,
                    first: page.first ?? page.pageNumber === 0,
                    last: page.last ?? page.pageNumber + 1 >= page.totalPages,
                    hasNext: page.hasNext ?? page.pageNumber + 1 < page.totalPages,
                    hasPrevious: page.hasPrevious ?? page.pageNumber > 0
                },
                isLoading: false
            });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải danh sách yêu cầu.'),
                isLoading: false
            });
        }
    },

    getRequestDetail: async (id, role) => {
        set({ isLoading: true, error: null, currentRequestDetail: null });
        try {
            const response = role === 'admin'
                ? await AdminWorkflowService.getAdminRequestDetail(id)
                : await CustomerService.getRequestDetail(id);
            set({ currentRequestDetail: response.data, isLoading: false });
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể tải chi tiết yêu cầu.'),
                isLoading: false
            });
        }
    },

    createRequestAction: async (data) => {
        const fingerprint = JSON.stringify(data);
        const state = get();

        const idempotencyKey = state.pendingRequestFingerprint === fingerprint
        && state.pendingRequestKey
            ? state.pendingRequestKey
            : createIdempotencyKey();

        set({
            isSubmitting: true,
            error: null,
            pendingRequestKey: idempotencyKey,
            pendingRequestFingerprint: fingerprint
        });

        try {
            const response = await CustomerService.createRequest(data, idempotencyKey);
            set((state) => ({
                myRequests: [response.data, ...state.myRequests],
                myRequestsPage: {
                    ...state.myRequestsPage,
                    totalElements: state.myRequestsPage.totalElements + 1
                },
                currentRequestDetail: response.data,
                isSubmitting: false,
                pendingRequestKey: null,
                pendingRequestFingerprint: null
            }));
            return response.data;
        } catch (error) {
            const message = getErrorMessage(error, 'Tạo yêu cầu thất bại.');
            set({
                error: message,
                isSubmitting: false
            });
            throw new Error(message);
        }
    },

    updateRequestAction: async (id, data, role = 'user') => {
        set({ isSubmitting: true, error: null });
        try {
            const response = role === 'admin'
                ? await AdminWorkflowService.updateAdminRequest(id, data)
                : await CustomerService.updateRequest(id, data);
            set((state) => ({
                currentRequestDetail: response.data,
                myRequests: role === 'user'
                    ? state.myRequests.map((request) =>
                        request.id === response.data.id ? response.data : request
                    )
                    : state.myRequests,
                adminRequests: role === 'admin'
                    ? state.adminRequests.map((request) =>
                        request.id === response.data.id ? response.data : request
                    )
                    : state.adminRequests,
                isSubmitting: false
            }));
            return response.data;
        } catch (error) {
            const message = getErrorMessage(error, 'Cập nhật yêu cầu thất bại.');
            set({
                error: message,
                isSubmitting: false
            });
            throw new Error(message);
        }
    },

    processRequestAction: async (id, data) => {
        set({ isSubmitting: true, error: null });
        try {
            const response = await AdminWorkflowService.processRequest(id, data);
            set((state) => ({
                currentRequestDetail: response.data,
                adminRequests: state.adminRequests.map((request) =>
                    request.id === response.data.id ? response.data : request
                ),
                isSubmitting: false
            }));
            return response.data;
        } catch (error) {
            const message = getErrorMessage(error, 'Xử lý yêu cầu thất bại.');
            set({
                error: message,
                isSubmitting: false
            });
            throw new Error(message);
        }
    },

    clearError: () => set({ error: null }),
    clearCurrentDetail: () => set({ currentRequestDetail: null })
}));
