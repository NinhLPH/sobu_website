import axios from 'axios';
import { authStorage } from '../utils/auth-storage';

const BASE_URL = 'http://localhost:8081';

const PUBLIC_ROUTES = [
    '/api/auth',
    '/api/public',
    '/api/v1/public',
    '/api/nhanh'
];

const isPublicRoute = (url: string): boolean => {
    return PUBLIC_ROUTES.some(route => url.startsWith(route));
};

const RAW_RESPONSE_ROUTES = [
    '/api/public/products',
    '/api/v1/public/products'
];

const isRawResponseRoute = (url: string): boolean => {
    return RAW_RESPONSE_ROUTES.some(route => url.startsWith(route));
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

        if (isRawResponseRoute(url)) {
            return response.data;
        }

        if (response.data && response.data.hasOwnProperty('data')) {
            return response.data.data;
        }

        return response.data;
    },
    async (error) => {
        const originalRequest = error.config;

        const isLoginRequest = originalRequest.url?.includes('/login');
        const isLogoutRequest = originalRequest.url?.includes('/logout');

        if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {

            if (isLoginRequest) {
                return Promise.reject(error);
            }

            if (isLogoutRequest) {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                window.location.href = '/login';
                return Promise.reject(error);
            }

            originalRequest._retry = true;
            try {
                const refreshToken = authStorage.getRefreshToken();
                // Gọi API refresh token
                const res = await axios.post(`${BASE_URL}/api/auth/refresh-token`, { refreshToken });

                const newAccessToken = res.data.data.accessToken;
                authStorage.setAccessToken(newAccessToken);

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
