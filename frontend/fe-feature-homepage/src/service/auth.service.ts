import {LoginResponse, RegisterResponse} from "../interface/api-response";
import apiClient from "../api/api-client";
import {Login, Register} from "../interface/auth.model";
import { authStorage } from "../utils/auth-storage";

export const AuthService = {
    login: (data: Login): Promise<LoginResponse> => {
        return apiClient.post('/api/auth/login', data);
    },

    register: (data: Register): Promise<RegisterResponse> => {
        return apiClient.post('/api/auth/register', data);
    },

    refreshToken: (refreshToken: string): Promise<LoginResponse> => {
        return apiClient.post('/api/auth/refresh-token', { refreshToken });
    },

    activate: (token: string): Promise<any> => {
        return apiClient.get('/api/auth/activate', { params: { token } });
    },

    resendActivationEmail: (email: string): Promise<any> => {
        return apiClient.post('/api/auth/resend-activation', { email });
    },

    logout: (): Promise<any> => {
        const accessToken = authStorage.getAccessToken();
        const refreshToken = authStorage.getRefreshToken();

        return apiClient.post(
            '/api/auth/logout',
            { refreshToken },
            accessToken
                ? {
                    headers: {
                        Authorization: `Bearer ${accessToken}`,
                    },
                }
                : undefined
        );
    },
};
