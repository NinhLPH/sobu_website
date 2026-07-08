import apiClient from '../api/api-client';
import {
    ApiResponseDTO,
    ApiResponseWithoutDataDTO,
    LoginResponse,
    RefreshTokenResponse,
    RegisterResponse
} from '../interface/api-response';
import { AccountDTO } from '../interface/account.model';
import {
    GoogleLoginRequest,
    LoginRequest,
    LogoutRequest,
    RefreshTokenRequest,
    RegisterRequest,
    UpdatePhoneRequest
} from '../interface/auth.model';
import { authStorage } from '../utils/auth-storage';

export const AuthService = {
    login: (
        data: LoginRequest
    ): Promise<ApiResponseDTO<LoginResponse>> => {
        return apiClient.post('/api/auth/login', data);
    },

    register: (
        data: RegisterRequest
    ): Promise<ApiResponseDTO<RegisterResponse>> => {
        return apiClient.post('/api/auth/register', data);
    },

    googleLogin: (
        data: GoogleLoginRequest
    ): Promise<ApiResponseDTO<LoginResponse>> => {
        return apiClient.post('/api/auth/oauth/google', data);
    },

    refreshToken: (
        data: RefreshTokenRequest
    ): Promise<ApiResponseDTO<RefreshTokenResponse>> => {
        return apiClient.post('/api/auth/refresh-token', data);
    },

    logout: (
        data: LogoutRequest = { refreshToken: authStorage.getRefreshToken() || undefined }
    ): Promise<ApiResponseWithoutDataDTO> => {
        const accessToken = authStorage.getAccessToken();

        if (!accessToken) {
            return Promise.reject(new Error('Access token is required to log out.'));
        }

        return apiClient.post('/api/auth/logout', data, {
            headers: { Authorization: `Bearer ${accessToken}` }
        });
    },

    updatePhone: (
        data: UpdatePhoneRequest
    ): Promise<ApiResponseDTO<AccountDTO>> => {
        return apiClient.patch('/api/auth/me/phone', data);
    }
};
