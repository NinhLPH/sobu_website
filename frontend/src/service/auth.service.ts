import apiClient from '../api/api-client';
import {
    ActivateAccountResponse,
    ApiResponseDTO,
    ApiResponseWithoutDataDTO,
    LoginResponse,
    RefreshTokenResponse,
    RegisterResponse
} from '../interface/api-response';
import {
    LoginRequest,
    LogoutRequest,
    RefreshTokenRequest,
    RegisterRequest,
    ResendActivationRequest
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

    resendActivation: (
        data: ResendActivationRequest
    ): Promise<ApiResponseWithoutDataDTO> => {
        return apiClient.post('/api/auth/resend-activation', data);
    },

    activateAccount: (
        token: string
    ): Promise<ApiResponseDTO<ActivateAccountResponse>> => {
        return apiClient.get('/api/auth/activate', { params: { token } });
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
    }
};
