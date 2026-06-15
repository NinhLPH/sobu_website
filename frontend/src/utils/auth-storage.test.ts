import { beforeEach, describe, expect, it } from '@jest/globals';
import { AccountDTO } from '../interface/account.model';
import { authStorage } from './auth-storage';

const account = {
    id: 1,
    email: 'user@example.com',
    fullName: 'Test User',
    phone: '0900000000',
    status: 'ACTIVE',
    role: { id: 1, name: 'USER' },
} as AccountDTO;

describe('authStorage', () => {
    beforeEach(() => {
        sessionStorage.clear();
        localStorage.clear();
    });

    it('stores auth data in sessionStorage and removes legacy localStorage values', () => {
        localStorage.setItem('accessToken', 'legacy-access-token');
        localStorage.setItem('refreshToken', 'legacy-refresh-token');
        localStorage.setItem('user', JSON.stringify(account));

        authStorage.setSession('access-token', 'refresh-token', account);

        expect(sessionStorage.getItem('accessToken')).toBe('access-token');
        expect(sessionStorage.getItem('refreshToken')).toBe('refresh-token');
        expect(JSON.parse(sessionStorage.getItem('user') || '{}')).toEqual(account);
        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(localStorage.getItem('refreshToken')).toBeNull();
        expect(localStorage.getItem('user')).toBeNull();
    });

    it('migrates existing localStorage auth data into the current session', () => {
        localStorage.setItem('accessToken', 'legacy-access-token');

        expect(authStorage.getAccessToken()).toBe('legacy-access-token');
        expect(sessionStorage.getItem('accessToken')).toBe('legacy-access-token');
        expect(localStorage.getItem('accessToken')).toBeNull();
    });

    it('clears auth data from sessionStorage and legacy localStorage', () => {
        sessionStorage.setItem('accessToken', 'access-token');
        sessionStorage.setItem('refreshToken', 'refresh-token');
        sessionStorage.setItem('user', JSON.stringify(account));
        localStorage.setItem('accessToken', 'legacy-access-token');
        localStorage.setItem('refreshToken', 'legacy-refresh-token');
        localStorage.setItem('user', JSON.stringify(account));

        authStorage.clear();

        expect(sessionStorage.getItem('accessToken')).toBeNull();
        expect(sessionStorage.getItem('refreshToken')).toBeNull();
        expect(sessionStorage.getItem('user')).toBeNull();
        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(localStorage.getItem('refreshToken')).toBeNull();
        expect(localStorage.getItem('user')).toBeNull();
    });
});
