import apiClient from "../api/api-client";

export const FileService = {
    uploadFile: (file: File, subDirectory?: string): Promise<{ url: string }> => {
        const formData = new FormData();
        formData.append('file', file);
        if (subDirectory) {
            formData.append('subDirectory', subDirectory);
        }

        return apiClient.post('/api/admin/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },
};