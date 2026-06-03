import { describe, expect, it, jest } from '@jest/globals';
import { AccountDTO } from '../interface/account.model';

const account = {
    id: 1,
    email: 'user@example.com',
    fullName: 'Test User',
    phone: '0900000000',
    status: 'ACTIVE',
    role: { id: 1, name: 'USER' },
} as AccountDTO;

describe('useAuthStore', () => {
    it('initializes authenticated state from session auth storage', async () => {
        jest.resetModules();
        sessionStorage.clear();
        localStorage.clear();
        sessionStorage.setItem('accessToken', 'access-token');
        sessionStorage.setItem('user', JSON.stringify(account));

        const { useAuthStore } = await import('./useAuthStore');

        expect(useAuthStore.getState().isAuthenticated).toBe(true);
        expect(useAuthStore.getState().user).toEqual(account);
        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(localStorage.getItem('user')).toBeNull();
    });
});
