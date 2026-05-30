import {LoginResponse, RegisterResponse} from "../interface/api-response";
import apiClient from "../api/api-client";
import {Login, Register} from "../interface/auth.model";

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

    logout: (): Promise<any> => {
        return apiClient.post('/api/auth/logout');
    },
};