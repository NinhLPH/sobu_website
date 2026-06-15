import { describe, expect, it } from '@jest/globals';
import { BASE_URL } from '../api/api-client';
import { getPublicImageUrl } from './file-url';

describe('getPublicImageUrl', () => {
    const baseUrl = BASE_URL.replace(/\/+$/, '');

    it('keeps absolute HTTP URLs unchanged', () => {
        const url = 'https://cdn.example.com/requests/image.jpg';

        expect(getPublicImageUrl(url)).toBe(url);
    });

    it('adds the public file endpoint to a storage path', () => {
        expect(getPublicImageUrl('requests/image.jpg')).toBe(
            `${baseUrl}/api/public/files/requests/image.jpg`
        );
    });

    it('does not duplicate a public file endpoint returned by the API', () => {
        expect(getPublicImageUrl('/api/public/files/requests/image.jpg')).toBe(
            `${baseUrl}/api/public/files/requests/image.jpg`
        );
    });

    it('returns an empty string for an empty path', () => {
        expect(getPublicImageUrl('   ')).toBe('');
    });
});
