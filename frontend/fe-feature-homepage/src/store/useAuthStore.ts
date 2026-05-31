import { create } from 'zustand';
import { AuthService } from '../service/auth.service';
import { AccountDTO } from '../interface/account.model';
import { Register } from '../interface/auth.model';

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
    try {
        const storedUser = localStorage.getItem('user');
        return storedUser ? JSON.parse(storedUser) : null;
    } catch {
        return null;
    }
};

export const useAuthStore = create<AuthState>((set) => ({
    user: getInitialUser(),
    isAuthenticated: !!localStorage.getItem('accessToken'),
    isLoading: false,
    error: null,

    loginAction: async (email, password) => {
        set({ isLoading: true, error: null });
        try {
            const data = await AuthService.login({ email, password });
            
            // Save tokens to localStorage
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            localStorage.setItem('user', JSON.stringify(data.account));

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
            
            localStorage.setItem('accessToken', loginData.accessToken);
            localStorage.setItem('refreshToken', loginData.refreshToken);
            localStorage.setItem('user', JSON.stringify(loginData.account));

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
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');

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
