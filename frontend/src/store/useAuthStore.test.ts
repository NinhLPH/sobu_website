import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { AccountDTO } from '../interface/account.model';

const mockAuthService = {
    login: jest.fn(),
    register: jest.fn(),
    googleLogin: jest.fn(),
    refreshToken: jest.fn(),
    updatePhone: jest.fn(),
    logout: jest.fn(),
};

jest.mock('../service/auth.service', () => ({
    AuthService: mockAuthService,
}));

const account = {
    id: 1,
    email: 'user@example.com',
    fullName: 'Test User',
    phone: '0900000000',
    status: 'ACTIVE',
    role: { id: 1, name: 'USER' },
} as AccountDTO;

const loginSession = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    account,
};

const successfulLoginResponse = {
    success: true,
    message: 'Login successful',
    data: loginSession,
};

describe('useAuthStore', () => {
    beforeEach(() => {
        jest.resetModules();
        jest.clearAllMocks();
        sessionStorage.clear();
        localStorage.clear();
    });

    it('initializes authenticated state from session auth storage', async () => {
        sessionStorage.setItem('accessToken', 'access-token');
        sessionStorage.setItem('user', JSON.stringify(account));

        const { useAuthStore } = await import('./useAuthStore');

        expect(useAuthStore.getState().isAuthenticated).toBe(true);
        expect(useAuthStore.getState().user).toEqual(account);
        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(localStorage.getItem('user')).toBeNull();
    });

    it('registers then logs in and stores the returned session', async () => {
        mockAuthService.register.mockResolvedValue({
            success: true,
            message: 'Registration successful',
            data: {
                ...account,
                message: 'Registration successful',
            },
        } as never);
        mockAuthService.login.mockResolvedValue(successfulLoginResponse as never);

        const { useAuthStore } = await import('./useAuthStore');
        const session = await useAuthStore.getState().registerAction({
            email: 'user@example.com',
            password: 'password123',
            fullName: 'Test User',
            phone: '0900000000',
        });

        expect(mockAuthService.register).toHaveBeenCalledWith({
            email: 'user@example.com',
            password: 'password123',
            fullName: 'Test User',
            phone: '0900000000',
        });
        expect(mockAuthService.login).toHaveBeenCalledWith({
            email: 'user@example.com',
            password: 'password123',
        });
        expect(session).toEqual(loginSession);
        expect(useAuthStore.getState().isAuthenticated).toBe(true);
        expect(useAuthStore.getState().user).toEqual(account);
        expect(sessionStorage.getItem('accessToken')).toBe('access-token');
        expect(sessionStorage.getItem('refreshToken')).toBe('refresh-token');
        expect(JSON.parse(sessionStorage.getItem('user') || '{}')).toEqual(account);
    });

    it('stores a session returned by Google login', async () => {
        mockAuthService.googleLogin.mockResolvedValue(successfulLoginResponse as never);

        const { useAuthStore } = await import('./useAuthStore');
        const session = await useAuthStore.getState().googleLoginAction('google-id-token');

        expect(mockAuthService.googleLogin).toHaveBeenCalledWith({ idToken: 'google-id-token' });
        expect(session).toEqual(loginSession);
        expect(useAuthStore.getState().isAuthenticated).toBe(true);
        expect(useAuthStore.getState().user).toEqual(account);
        expect(sessionStorage.getItem('accessToken')).toBe('access-token');
        expect(sessionStorage.getItem('refreshToken')).toBe('refresh-token');
    });

    it('updates the stored user after changing phone number', async () => {
        const updatedAccount = {
            ...account,
            phone: '0987654321',
        };
        mockAuthService.updatePhone.mockResolvedValue({
            success: true,
            message: 'Phone number updated successfully',
            data: updatedAccount,
        } as never);
        sessionStorage.setItem('accessToken', 'access-token');
        sessionStorage.setItem('refreshToken', 'refresh-token');
        sessionStorage.setItem('user', JSON.stringify(account));

        const { useAuthStore } = await import('./useAuthStore');
        const updatedUser = await useAuthStore.getState().updatePhoneAction('0987654321');

        expect(mockAuthService.updatePhone).toHaveBeenCalledWith({ phone: '0987654321' });
        expect(updatedUser).toEqual(updatedAccount);
        expect(useAuthStore.getState().user).toEqual(updatedAccount);
        expect(useAuthStore.getState().isAuthenticated).toBe(true);
        expect(sessionStorage.getItem('accessToken')).toBe('access-token');
        expect(sessionStorage.getItem('refreshToken')).toBe('refresh-token');
        expect(JSON.parse(sessionStorage.getItem('user') || '{}')).toEqual(updatedAccount);
    });

    it('does not expose removed email activation actions', async () => {
        const { useAuthStore } = await import('./useAuthStore');
        const state = useAuthStore.getState();

        expect(Object.prototype.hasOwnProperty.call(state, 'resendActivationAction')).toBe(false);
        expect(Object.prototype.hasOwnProperty.call(state, 'activateAccountAction')).toBe(false);
    });
});
