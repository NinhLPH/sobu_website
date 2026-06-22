import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import ImageUploader from './ImageUploader';
import { FileService } from '../../service/file.service';

jest.mock('../../service/file.service');

const mockedFileService = jest.mocked(FileService);

describe('ImageUploader', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('uploads selected images and returns their stored URLs', async () => {
        const onChange = jest.fn();
        const file = new File(['image'], 'request.png', { type: 'image/png' });
        const uploadedUrl = '/api/public/files/requests/request.png';
        mockedFileService.uploadFile.mockResolvedValue({ url: uploadedUrl });

        render(
            <ImageUploader
                uploadedUrls={[]}
                onChange={onChange}
                subDirectory="requests"
            />
        );

        fireEvent.change(screen.getByLabelText('Chọn ảnh tải lên'), {
            target: { files: [file] }
        });

        await waitFor(() => {
            expect(mockedFileService.uploadFile).toHaveBeenCalledWith(file, 'requests');
            expect(onChange).toHaveBeenCalledWith([uploadedUrl]);
        });
    });

    it('uploads images dropped onto the drop zone', async () => {
        const onChange = jest.fn();
        const file = new File(['image'], 'dropped.png', { type: 'image/png' });
        const uploadedUrl = '/api/public/files/requests/dropped.png';
        mockedFileService.uploadFile.mockResolvedValue({ url: uploadedUrl });

        render(<ImageUploader uploadedUrls={[]} onChange={onChange} />);

        const dropZone = screen.getByText('Chọn hoặc kéo thả ảnh').closest('label');
        expect(dropZone).not.toBeNull();

        fireEvent.drop(dropZone as HTMLLabelElement, {
            dataTransfer: { files: [file] }
        });

        await waitFor(() => {
            expect(onChange).toHaveBeenCalledWith([uploadedUrl]);
        });
    });
});
