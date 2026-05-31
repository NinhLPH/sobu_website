import {AccountDTO} from "./account.model";
import {RoleDTO} from "./role.model";

export interface ApiResponseDTO<T> {
    success: boolean;
    statusCode: number;
    message: string;
    data: T;
    error?: string;
    timestamp?: string;
}

export interface PageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    account: AccountDTO;
}

export interface RegisterResponse {
    id: number;
    email: string;
    fullName: string;
    phone: string;
    role: RoleDTO;
    status: string;
    message: string;
}