import { AccountDTO } from './account.model';

export interface ApiResponseDTO<T> {
    success: boolean;
    statusCode?: number;
    message: string;
    data: T;
    error?: string;
    timestamp?: string;
}

export interface ApiResponseWithoutDataDTO {
    success: boolean;
    statusCode?: number;
    message: string;
    error?: string;
    timestamp?: string;
}

export interface PageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    first?: boolean;
    last?: boolean;
    hasNext?: boolean;
    hasPrevious?: boolean;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    account: AccountDTO;
}

export interface RefreshTokenResponse {
    accessToken: string;
    refreshToken: string;
    tokenType?: string;
    expiresIn?: number;
    account?: AccountDTO;
}

export interface RegisterResponse extends AccountDTO {
    message: string;
}
