import { BASE_URL } from '../api/api-client';

const PUBLIC_FILE_PREFIX = '/api/public/files/';

export const getPublicImageUrl = (path: string): string => {
    const normalizedPath = path.trim();

    if (!normalizedPath) {
        return '';
    }

    if (/^https?:\/\//i.test(normalizedPath)) {
        return normalizedPath;
    }

    const normalizedBaseUrl = BASE_URL.replace(/\/+$/, '');

    if (normalizedPath.startsWith(PUBLIC_FILE_PREFIX)) {
        return `${normalizedBaseUrl}${normalizedPath}`;
    }

    if (normalizedPath.startsWith(PUBLIC_FILE_PREFIX.slice(1))) {
        return `${normalizedBaseUrl}/${normalizedPath}`;
    }

    return `${normalizedBaseUrl}${PUBLIC_FILE_PREFIX}${normalizedPath.replace(/^\/+/, '')}`;
};
