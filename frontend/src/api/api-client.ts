import axios from 'axios';
import {authStorage} from '../utils/auth-storage';

export const BASE_URL = 'https://7bb7-58-186-240-2.ngrok-free.app';

const PUBLIC_ROUTES = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh-token',
    '/api/auth/resend-activation',
    '/api/auth/activate',
    '/api/public',
    '/api/v1/public',
    '/api/nhanh'
];

const isPublicRoute = (url: string): boolean => {
    return PUBLIC_ROUTES.some(route => url.startsWith(route));
};

const WRAPPED_RESPONSE_ROUTES = [
    '/api/auth',
    '/api/requests',
    '/api/orders',
    '/api/admin/requests',
    '/api/admin/orders',
    '/api/public/locations',
    '/v1/api/admin/payments'
];

const isWrappedResponseRoute = (url: string): boolean => {
    return WRAPPED_RESPONSE_ROUTES.some(route => url.startsWith(route));
};

const apiClient = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use(
    (config) => {
        const url = config.url || '';

        if (!isPublicRoute(url)) {
            const token = authStorage.getAccessToken();
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        }

        return config;
    },
    (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
    (response) => {
        const url = response.config.url || '';

        if (isWrappedResponseRoute(url)) {
            return response.data;
        }

        if (response.data && response.data.hasOwnProperty('data')) {
            return response.data.data;
        }

        return response.data;
    },
    async (error) => {
        const originalRequest = error.config;
        if (!originalRequest) {
            return Promise.reject(error);
        }

        const requestUrl = originalRequest.url || '';
        const isLoginRequest = requestUrl.includes('/login');
        const isLogoutRequest = requestUrl.includes('/logout');
        const isRefreshRequest = requestUrl.includes('/refresh-token');
        const isPublicRequest = isPublicRoute(requestUrl);

        if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
            if (isPublicRequest) {
                return Promise.reject(error);
            }

            if (isLoginRequest || isRefreshRequest) {
                if (isRefreshRequest) {
                    authStorage.clear();
                }
                return Promise.reject(error);
            }

            if (isLogoutRequest) {
                authStorage.clear();
                window.location.href = '/login';
                return Promise.reject(error);
            }

            originalRequest._retry = true;
            try {
                const refreshToken = authStorage.getRefreshToken();
                // Gọi API refresh token
                const res = await axios.post(`${BASE_URL}/api/auth/refresh-token`, {refreshToken});

                const session = res.data.data;
                const newAccessToken = session.accessToken;

                if (session.account && session.refreshToken) {
                    authStorage.setSession(
                        session.accessToken,
                        session.refreshToken,
                        session.account
                    );
                } else {
                    authStorage.setAccessToken(session.accessToken);
                    if (session.refreshToken) {
                        authStorage.setRefreshToken(session.refreshToken);
                    }
                }

                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                return apiClient(originalRequest);
            } catch (refreshError) {
                authStorage.clear();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default apiClient;
