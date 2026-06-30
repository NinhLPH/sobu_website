import { create } from 'zustand';
import { AuthService } from '../service/auth.service';
import { AccountDTO } from '../interface/account.model';
import {
    LoginResponse,
    RefreshTokenResponse
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

const getSessionState = (session: LoginResponse) => {
    authStorage.setSession(
        session.accessToken,
        session.refreshToken,
        session.account
    );

    return {
        user: session.account,
        isAuthenticated: true,
        isLoading: false,
        error: null
    };
};

interface AuthState {
    user: AccountDTO | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    loginAction: (email: string, password: string) => Promise<LoginResponse>;
    registerAction: (data: RegisterRequest) => Promise<LoginResponse>;
    googleLoginAction: (idToken: string) => Promise<LoginResponse>;
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

            set(getSessionState(session));
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
            const registerResponse = await AuthService.register(data);
            assertSuccess(registerResponse, 'Đăng ký thất bại.');

            const loginResponse = await AuthService.login({
                email: data.email,
                password: data.password
            });
            const session = assertSuccess(loginResponse, 'Đăng nhập sau đăng ký thất bại.');

            set(getSessionState(session));
            return session;
        } catch (error) {
            const message = getErrorMessage(
                error,
                'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.'
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

    googleLoginAction: async (idToken) => {
        set({ isLoading: true, error: null });
        try {
            const response = await AuthService.googleLogin({ idToken });
            const session = assertSuccess(response, 'Đăng nhập Google thất bại.');

            set(getSessionState(session));
            return session;
        } catch (error) {
            const message = getErrorMessage(
                error,
                'Không thể đăng nhập bằng Google lúc này.'
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
