import { PageResponse } from './api-response';

export interface SupportConversation {
    id: number;
    status: string;
    lastMessageAt: string;
    createdAt: string;
    customerEmail: string;
    customerName: string;
}

export interface SupportMessage {
    id: number;
    conversationId: number;
    senderId: number;
    senderEmail: string;
    senderRole: string;
    content: string;
    createdAt: string;
}

export type SupportMessagePage = PageResponse<SupportMessage>;

export type SupportWebSocketEventType = 'AUTH_SUCCESS' | 'MESSAGE_CREATED' | 'ERROR';

export interface SupportWebSocketEvent {
    type?: SupportWebSocketEventType | string;
    message?: SupportMessage;
    data?: {
        message?: SupportMessage;
    };
    error?: string;
}
