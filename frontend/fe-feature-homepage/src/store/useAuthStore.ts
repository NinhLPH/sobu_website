import { create } from 'zustand';
import { AuthService } from '../service/auth.service';
import { AccountDTO } from '../interface/account.model';
import { Register } from '../interface/auth.model';
import { authStorage } from '../utils/auth-storage';

interface AuthState {
    user: AccountDTO | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    loginAction: (email: string, password: string) => Promise<void>;
    registerAction: (data: Register) => Promise<void>;
    logoutAction: () => Promise<void>;
    clearError: () => void;
}

const getInitialUser = (): AccountDTO | null => {
    return authStorage.getUser();
};

export const useAuthStore = create<AuthState>((set) => ({
    user: getInitialUser(),
    isAuthenticated: !!authStorage.getAccessToken(),
    isLoading: false,
    error: null,

    loginAction: async (email, password) => {
        set({ isLoading: true, error: null });
        try {
            const data = await AuthService.login({ email, password });
            
            authStorage.setSession(data.accessToken, data.refreshToken, data.account);

            set({
                user: data.account,
                isAuthenticated: true,
                isLoading: false,
                error: null,
            });
        } catch (err: any) {
            const errorMessage = err?.response?.data?.message || err?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!';
            set({
                error: errorMessage,
                isLoading: false,
                isAuthenticated: false,
                user: null,
            });
            throw err;
        }
    },

    registerAction: async (data) => {
        set({ isLoading: true, error: null });
        try {
            await AuthService.register(data);
            
            // Tự động đăng nhập sau khi đăng ký thành công
            const loginData = await AuthService.login({ email: data.email, password: data.password });
            
            authStorage.setSession(loginData.accessToken, loginData.refreshToken, loginData.account);

            set({
                user: loginData.account,
                isAuthenticated: true,
                isLoading: false,
                error: null,
            });
        } catch (err: any) {
            const errorMessage = err?.response?.data?.message || err?.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin!';
            set({
                error: errorMessage,
                isLoading: false,
            });
            throw err;
        }
    },

    logoutAction: async () => {
        set({ isLoading: true, error: null });
        try {
            await AuthService.logout();
        } catch (err) {
            console.error('Logout error on server:', err);
        } finally {
            authStorage.clear();

            set({
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: null,
            });
        }
    },

    clearError: () => set({ error: null }),
}));
