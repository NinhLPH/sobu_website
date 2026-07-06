import { BASE_URL } from '../api/api-client';

export const getSupportWebSocketUrl = (apiBaseUrl = BASE_URL) => {
    try {
        const fallbackOrigin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8081';
        const url = new URL(apiBaseUrl, fallbackOrigin);
        url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
        url.pathname = '/ws/support';
        url.search = '';
        url.hash = '';
        return url.toString();
    } catch {
        return 'ws://localhost:8081/ws/support';
    }
};
