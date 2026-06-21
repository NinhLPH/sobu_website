import { RequestStatus, RequestType } from '../enum/union-types';

export type RequestWorkflowType = Exclude<RequestType, 'NORMAL'>;

export interface RequestItemResponseDto {
    id: number;
    nhanhProductId?: string;
    name: string;
    note?: string;
    metadataJson?: string;
    price: number;
    quantity: number;
}

export interface RequestAttachmentDto {
    id: number;
    url: string;
    type: string;
    mimeType?: string;
    size?: number;
    sortOrder?: number;
    uploadedBy?: string;
    createdAt?: string;
}

export interface RequestResponseDto {
    id: number;
    requestCode: string;
    customerPhone: string;
    type: RequestType;
    status: RequestStatus;
    totalAmount: number;
    depositAmount: number;
    customRequirements?: string | null;
    nhanhOrderId?: string | null;
    nhanhOrderCode?: string | null;
    items: RequestItemResponseDto[];
    attachments: RequestAttachmentDto[];
    createdAt: string;
    updatedAt: string;
}

export interface RequestItemDto {
    nhanhProductId?: string;
    name: string;
    note?: string;
    price?: number;
    quantity: number;
    metadataJson?: string;
}

export interface CreateRequestDto {
    customerPhone: string;
    type: RequestWorkflowType;
    customRequirements?: string;
    uploadedImageUrls?: string[];
    items: RequestItemDto[];
}

export interface ProcessRequestDto {
    targetStatus: RequestStatus;
    note?: string;
    depositAmount?: number;
}

export interface UpdateRequestDto {
    customerPhone?: string;
    type?: RequestWorkflowType;
    customRequirements?: string;
    totalAmount?: number;
    depositAmount?: number;
    uploadedImageUrls?: string[];
    items?: RequestItemDto[];
}
