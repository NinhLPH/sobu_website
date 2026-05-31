import axios from 'axios';

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
            const token = localStorage.getItem('accessToken');
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
    (error) => {
        return Promise.reject(error);
    }
);

export default apiClient;