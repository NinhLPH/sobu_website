import { create } from 'zustand';
import { CustomerService } from '../service/custom.service';
import { AdminWorkflowService } from '../service/admin.service';
import { RequestResponseDto, CreateRequestDto, UpdateRequestDto, ProcessRequestDto } from '../interface/customer-request.model';

interface RequestState {
    myRequests: RequestResponseDto[];
    adminRequests: RequestResponseDto[];
    currentRequestDetail: RequestResponseDto | null;
    isLoading: boolean;
    isSubmitting: boolean;
    error: string | null;

    fetchMyRequests: (params?: any) => Promise<void>;
    fetchAdminRequests: (params?: any) => Promise<void>;
    getRequestDetail: (id: string | number, role: 'user' | 'admin') => Promise<void>;
    createRequestAction: (data: CreateRequestDto) => Promise<RequestResponseDto>;
    updateRequestAction: (id: string | number, data: UpdateRequestDto) => Promise<RequestResponseDto>;
    processRequestAction: (id: string | number, data: ProcessRequestDto) => Promise<RequestResponseDto>;
    clearError: () => void;
    clearCurrentDetail: () => void;
}

export const useRequestStore = create<RequestState>((set) => ({
    myRequests: [],
    adminRequests: [],
    currentRequestDetail: null,
    isLoading: false,
    isSubmitting: false,
    error: null,

    fetchMyRequests: async (params) => {
        set({ isLoading: true, error: null });
        try {
            const data = await CustomerService.getMyRequests(params);
            const list = data && 'content' in data ? (data as any).content : (Array.isArray(data) ? data : []);
            set({ myRequests: list, isLoading: false });
        } catch (err: any) {
            set({ error: err?.message || 'Không thể tải danh sách yêu cầu của tôi!', isLoading: false });
        }
    },

    fetchAdminRequests: async (params) => {
        set({ isLoading: true, error: null });
        try {
            const data = await AdminWorkflowService.getRequests(params);
            const list = data && 'content' in data ? (data as any).content : (Array.isArray(data) ? data : []);
            set({ adminRequests: list, isLoading: false });
        } catch (err: any) {
            set({ error: err?.message || 'Không thể tải danh sách yêu cầu của quản trị!', isLoading: false });
        }
    },

    getRequestDetail: async (id, role) => {
        set({ isLoading: true, error: null, currentRequestDetail: null });
        try {
            let detail: RequestResponseDto;
            if (role === 'admin') {
                detail = await AdminWorkflowService.getRequestDetail(id);
            } else {
                detail = await CustomerService.getMyRequestDetail(id);
            }
            set({ currentRequestDetail: detail, isLoading: false });
        } catch (err: any) {
            set({ error: err?.message || 'Không thể tải chi tiết yêu cầu!', isLoading: false });
        }
    },

    createRequestAction: async (data) => {
        set({ isSubmitting: true, error: null });
        try {
            const response = await CustomerService.createRequest(data);
            set({ isSubmitting: false });
            return response;
        } catch (err: any) {
            const msg = err?.response?.data?.message || err?.message || 'Tạo yêu cầu thất bại!';
            set({ error: msg, isSubmitting: false });
            throw err;
        }
    },

    updateRequestAction: async (id, data) => {
        set({ isSubmitting: true, error: null });
        try {
            const response = await CustomerService.updateRequest(id, data);
            set({ currentRequestDetail: response, isSubmitting: false });
            return response;
        } catch (err: any) {
            const msg = err?.response?.data?.message || err?.message || 'Cập nhật yêu cầu thất bại!';
            set({ error: msg, isSubmitting: false });
            throw err;
        }
    },

    processRequestAction: async (id, data) => {
        set({ isSubmitting: true, error: null });
        try {
            const response = await AdminWorkflowService.processRequest(id, data);
            set({ currentRequestDetail: response, isSubmitting: false });
            return response;
        } catch (err: any) {
            const msg = err?.response?.data?.message || err?.message || 'Xử lý yêu cầu thất bại!';
            set({ error: msg, isSubmitting: false });
            throw err;
        }
    },

    clearError: () => set({ error: null }),
    clearCurrentDetail: () => set({ currentRequestDetail: null }),
}));
