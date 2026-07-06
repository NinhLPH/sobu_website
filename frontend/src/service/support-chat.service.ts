import apiClient from '../api/api-client';
import { ApiResponseDTO, PageResponse } from '../interface/api-response';
import { SupportConversation, SupportMessagePage } from '../interface/support-chat.model';

interface SupportMessagesParams {
    page: number;
    size: number;
}

export const SupportChatService = {
    getConversation: (): Promise<ApiResponseDTO<SupportConversation>> => {
        return apiClient.get('/api/support/conversation');
    },

    getMessages: (
        params: SupportMessagesParams
    ): Promise<ApiResponseDTO<SupportMessagePage>> => {
        return apiClient.get('/api/support/conversation/messages', { params });
    },

    getAdminConversations: (
        params: SupportMessagesParams
    ): Promise<ApiResponseDTO<PageResponse<SupportConversation>>> => {
        return apiClient.get('/api/admin/support/conversations', { params });
    },

    getAdminConversationMessages: (
        conversationId: number,
        params: SupportMessagesParams
    ): Promise<ApiResponseDTO<SupportMessagePage>> => {
        return apiClient.get(`/api/admin/support/conversations/${conversationId}/messages`, { params });
    }
};
