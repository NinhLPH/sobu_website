import {applyTheme, THEME_STORAGE_KEY, useThemeStore} from './useThemeStore';
import {beforeEach, describe, expect, it} from '@jest/globals';

describe('theme store', () => {
    beforeEach(() => {
        localStorage.clear();
        document.documentElement.classList.remove('light', 'dark');
        document.documentElement.removeAttribute('data-theme');
        useThemeStore.setState({theme: 'light'});
    });

    it('persists and applies the selected theme', () => {
        useThemeStore.getState().setTheme('dark');
        expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
        expect(document.documentElement.classList.contains('dark')).toBe(true);
        expect(document.documentElement.dataset.theme).toBe('dark');
    });

    it('keeps light and dark classes mutually exclusive', () => {
        applyTheme('dark');
        applyTheme('light');
        expect(document.documentElement.classList.contains('light')).toBe(true);
        expect(document.documentElement.classList.contains('dark')).toBe(false);
    });
});
