import {create} from 'zustand';

export type ThemeMode = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'sobu-theme';

const getStoredTheme = (): ThemeMode | null => {
    if (typeof window === 'undefined') return null;
    const value = window.localStorage.getItem(THEME_STORAGE_KEY);
    return value === 'light' || value === 'dark' ? value : null;
};

const getSystemTheme = (): ThemeMode => {
    if (typeof window === 'undefined') return 'light';
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

export const applyTheme = (theme: ThemeMode) => {
    if (typeof document === 'undefined') return;
    const root = document.documentElement;
    root.classList.toggle('dark', theme === 'dark');
    root.classList.toggle('light', theme === 'light');
    root.dataset.theme = theme;
};

type ThemeState = {
    theme: ThemeMode;
    setTheme: (theme: ThemeMode) => void;
    toggleTheme: () => void;
    syncSystemTheme: (theme: ThemeMode) => void;
};

const initialTheme = getStoredTheme() || getSystemTheme();

export const useThemeStore = create<ThemeState>((set, get) => ({
    theme: initialTheme,
    setTheme: (theme) => {
        window.localStorage.setItem(THEME_STORAGE_KEY, theme);
        applyTheme(theme);
        set({theme});
    },
    toggleTheme: () => get().setTheme(get().theme === 'dark' ? 'light' : 'dark'),
    syncSystemTheme: (theme) => {
        if (getStoredTheme()) return;
        applyTheme(theme);
        set({theme});
    }
}));

export const initializeTheme = () => {
    const theme = getStoredTheme() || getSystemTheme();
    useThemeStore.setState({theme});
    applyTheme(theme);
};

export const listenToSystemTheme = () => {
    if (typeof window === 'undefined' || !window.matchMedia) return () => undefined;
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const listener = (event: MediaQueryListEvent) => {
        useThemeStore.getState().syncSystemTheme(event.matches ? 'dark' : 'light');
    };
    media.addEventListener?.('change', listener);
    return () => media.removeEventListener?.('change', listener);
};
