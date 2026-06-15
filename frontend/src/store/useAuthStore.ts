import { create } from 'zustand';
import { AuthService } from '../service/auth.service';
import { AccountDTO } from '../interface/account.model';
import {
    LoginResponse,
    RefreshTokenResponse,
    RegisterResponse
} from '../interface/api-response';
import { RegisterRequest } from '../interface/auth.model';
import { authStorage } from '../utils/auth-storage';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

const assertSuccess = <T>(
    response: { success: boolean; message: string; data: T },
    fallback: string
): T => {
    if (!response.success) {
        throw new Error(response.message || fallback);
    }
    return response.data;
};

interface AuthState {
    user: AccountDTO | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    loginAction: (email: string, password: string) => Promise<LoginResponse>;
    registerAction: (data: RegisterRequest) => Promise<RegisterResponse>;
    resendActivationAction: (email: string) => Promise<string>;
    activateAccountAction: (token: string) => Promise<string>;
    refreshTokenAction: () => Promise<RefreshTokenResponse>;
    logoutAction: () => Promise<void>;
    clearError: () => void;
}

const initialUser = authStorage.getUser();
const initialAccessToken = authStorage.getAccessToken();

export const useAuthStore = create<AuthState>((set, get) => ({
    user: initialUser,
    isAuthenticated: Boolean(initialAccessToken && initialUser),
    isLoading: false,
    error: null,

    loginAction: async (email, password) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.login({ email, password });
            const session = assertSuccess(response, 'Đăng nhập thất bại.');

            authStorage.setSession(
                session.accessToken,
                session.refreshToken,
                session.account
            );
            set({
                user: session.account,
                isAuthenticated: true,
                isLoading: false,
                error: null
            });
            return session;
        } catch (error) {
            const message = getErrorMessage(
                error,
                'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.'
            );
            authStorage.clear();
            set({
                error: message,
                isLoading: false,
                isAuthenticated: false,
                user: null
            });
            throw error;
        }
    },

    registerAction: async (data) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.register(data);
            const account = assertSuccess(response, 'Đăng ký thất bại.');
            set({ isLoading: false, error: null });
            return account;
        } catch (error) {
            const message = getErrorMessage(
                error,
                'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.'
            );
            set({ error: message, isLoading: false });
            throw error;
        }
    },

    resendActivationAction: async (email) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.resendActivation({ email });
            if (!response.success) {
                throw new Error(response.message || 'Không thể gửi lại email kích hoạt.');
            }
            set({ isLoading: false, error: null });
            return response.message || 'Email kích hoạt mới đã được gửi.';
        } catch (error) {
            const message = getErrorMessage(
                error,
                'Không thể gửi lại email kích hoạt lúc này.'
            );
            set({ error: message, isLoading: false });
            throw error;
        }
    },

    activateAccountAction: async (token) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.activateAccount(token);
            const account = assertSuccess(response, 'Kích hoạt tài khoản thất bại.');
            set({ isLoading: false, error: null });
            return account.message || response.message || 'Kích hoạt tài khoản thành công.';
        } catch (error) {
            const message = getErrorMessage(
                error,
                'Mã kích hoạt không hợp lệ hoặc đã hết hạn.'
            );
            set({ error: message, isLoading: false });
            throw error;
        }
    },

    refreshTokenAction: async () => {
        const refreshToken = authStorage.getRefreshToken();
        if (!refreshToken) {
            const error = new Error('Phiên đăng nhập đã hết hạn.');
            authStorage.clear();
            set({
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: error.message
            });
            throw error;
        }

        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.refreshToken({ refreshToken });
            const session = assertSuccess(response, 'Không thể làm mới phiên đăng nhập.');
            const user = session.account || get().user || authStorage.getUser();

            if (!user) {
                throw new Error('Không thể khôi phục thông tin tài khoản.');
            }

            authStorage.setSession(session.accessToken, session.refreshToken, user);
            set({
                user,
                isAuthenticated: true,
                isLoading: false,
                error: null
            });
            return session;
        } catch (error) {
            const message = getErrorMessage(error, 'Phiên đăng nhập đã hết hạn.');
            authStorage.clear();
            set({
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: message
            });
            throw error;
        }
    },

    logoutAction: async () => {
        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.logout();
            if (!response.success) {
                throw new Error(response.message || 'Đăng xuất thất bại.');
            }
        } catch (error) {
            set({
                error: getErrorMessage(error, 'Không thể đăng xuất trên máy chủ.')
            });
        } finally {
            authStorage.clear();
            set({
                user: null,
                isAuthenticated: false,
                isLoading: false
            });
        }
    },

    clearError: () => set({ error: null })
}));
