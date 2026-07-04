import apiClient from '../api/api-client';
import { ApiResponseDTO } from '../interface/api-response';
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
    }
};
