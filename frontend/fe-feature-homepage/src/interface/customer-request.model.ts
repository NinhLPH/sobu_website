import {RequestStatus, RequestType} from "../enum/union-types";

export interface RequestItemResponseDto {
    id: number;
    name: string;
    nhanhProductId?: string;
    note?: string;
    price: number;
    quantity: number;
    metadataJson?: any;
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
    customRequirements?: any; // JSON Map
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    items: RequestItemResponseDto[];
    attachments: RequestAttachmentDto[];
    createdAt: string;
    updatedAt: string;
}

export interface CreateRequestDto {
    customerPhone: string;
    type: RequestType;
    customRequirements?: any;
    items: {
        name: string;
        quantity: number;
        price?: number;
        note?: string;
    }[];
    attachments?: string[];
}

export interface ProcessRequestDto {
    targetStatus: RequestStatus;
    note?: string;
    depositAmount?: number;
}

export interface UpdateRequestItemDto {
    name?: string;
    quantity?: number;
    price?: number;
    note?: string;
}

export interface UpdateRequestDto {
    customerPhone?: string;
    type?: RequestType;
    customRequirements?: any;
    items?: UpdateRequestItemDto[];
    attachments?: string[] | number[];
}
