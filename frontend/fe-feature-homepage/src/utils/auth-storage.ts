import { AccountDTO } from '../interface/account.model';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'user';

const getSessionStorage = (): Storage | null => {
    return typeof window === 'undefined' ? null : window.sessionStorage;
};

const getLocalStorage = (): Storage | null => {
    return typeof window === 'undefined' ? null : window.localStorage;
};

const removeLegacyLocalAuth = () => {
    const legacyStorage = getLocalStorage();
    legacyStorage?.removeItem(ACCESS_TOKEN_KEY);
    legacyStorage?.removeItem(REFRESH_TOKEN_KEY);
    legacyStorage?.removeItem(USER_KEY);
};

const getSessionValue = (key: string): string | null => {
    const sessionStorage = getSessionStorage();
    const value = sessionStorage?.getItem(key) ?? null;

    if (value !== null) {
        return value;
    }

    const legacyValue = getLocalStorage()?.getItem(key) ?? null;

    if (legacyValue !== null) {
        sessionStorage?.setItem(key, legacyValue);
        getLocalStorage()?.removeItem(key);
    }

    return legacyValue;
};

export const authStorage = {
    getAccessToken: (): string | null => getSessionValue(ACCESS_TOKEN_KEY),

    getRefreshToken: (): string | null => getSessionValue(REFRESH_TOKEN_KEY),

    getUser: (): AccountDTO | null => {
        const storedUser = getSessionValue(USER_KEY);

        if (!storedUser) {
            return null;
        }

        try {
            return JSON.parse(storedUser);
        } catch {
            getSessionStorage()?.removeItem(USER_KEY);
            getLocalStorage()?.removeItem(USER_KEY);
            return null;
        }
    },

    setSession: (accessToken: string, refreshToken: string, user: AccountDTO) => {
        const sessionStorage = getSessionStorage();

        sessionStorage?.setItem(ACCESS_TOKEN_KEY, accessToken);
        sessionStorage?.setItem(REFRESH_TOKEN_KEY, refreshToken);
        sessionStorage?.setItem(USER_KEY, JSON.stringify(user));
        removeLegacyLocalAuth();
    },

    setAccessToken: (accessToken: string) => {
        getSessionStorage()?.setItem(ACCESS_TOKEN_KEY, accessToken);
        getLocalStorage()?.removeItem(ACCESS_TOKEN_KEY);
    },

    clear: () => {
        const sessionStorage = getSessionStorage();

        sessionStorage?.removeItem(ACCESS_TOKEN_KEY);
        sessionStorage?.removeItem(REFRESH_TOKEN_KEY);
        sessionStorage?.removeItem(USER_KEY);
        removeLegacyLocalAuth();
    },
};
