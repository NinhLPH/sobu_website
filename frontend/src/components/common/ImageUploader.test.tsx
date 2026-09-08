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

    it('replaces the existing image in single-image mode', async () => {
        const onChange = jest.fn();
        const file = new File(['new image'], 'new-avatar.png', { type: 'image/png' });
        const uploadedUrl = '/api/public/files/products/new-avatar.png';
        mockedFileService.uploadFile.mockResolvedValue({ url: uploadedUrl });

        render(
            <ImageUploader
                uploadedUrls={['/api/public/files/products/old-avatar.png']}
                onChange={onChange}
                subDirectory="products"
                multiple={false}
                inputAriaLabel="Chọn ảnh đại diện tải lên"
            />
        );

        fireEvent.change(screen.getByLabelText('Chọn ảnh đại diện tải lên'), {
            target: { files: [file] }
        });

        await waitFor(() => {
            expect(mockedFileService.uploadFile).toHaveBeenCalledWith(file, 'products');
            expect(onChange).toHaveBeenCalledWith([uploadedUrl]);
        });
    });

    it('rejects multiple files in single-image mode without uploading', async () => {
        const onChange = jest.fn();
        const first = new File(['first'], 'first.png', { type: 'image/png' });
        const second = new File(['second'], 'second.png', { type: 'image/png' });

        render(<ImageUploader uploadedUrls={[]} onChange={onChange} multiple={false} />);

        const dropZone = screen.getByText('Chọn hoặc kéo thả một ảnh').closest('label');
        fireEvent.drop(dropZone as HTMLLabelElement, {
            dataTransfer: { files: [first, second] }
        });

        expect((await screen.findByRole('alert')).textContent).toContain('Vui lòng chỉ chọn một ảnh.');
        expect(mockedFileService.uploadFile).not.toHaveBeenCalled();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('rejects an oversized image without changing existing images', async () => {
        const onChange = jest.fn();
        const file = new File(['oversized'], 'large.png', { type: 'image/png' });
        Object.defineProperty(file, 'size', { value: 10 * 1024 * 1024 + 1 });

        render(
            <ImageUploader
                uploadedUrls={['/api/public/files/products/current.png']}
                onChange={onChange}
                maxFileSizeBytes={10 * 1024 * 1024}
            />
        );

        fireEvent.change(screen.getByLabelText('Chọn ảnh tải lên'), {
            target: { files: [file] }
        });

        expect((await screen.findByRole('alert')).textContent).toContain('Mỗi ảnh không được vượt quá 10 MB.');
        expect(mockedFileService.uploadFile).not.toHaveBeenCalled();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('keeps existing images when an upload fails', async () => {
        const onChange = jest.fn();
        const file = new File(['image'], 'failed.png', { type: 'image/png' });
        mockedFileService.uploadFile.mockRejectedValue(new Error('network error'));

        render(
            <ImageUploader
                uploadedUrls={['/api/public/files/products/current.png']}
                onChange={onChange}
            />
        );

        fireEvent.change(screen.getByLabelText('Chọn ảnh tải lên'), {
            target: { files: [file] }
        });

        expect((await screen.findByRole('alert')).textContent).toContain('Tải ảnh lên thất bại. Vui lòng thử lại.');
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByAltText('Ảnh đính kèm 1')).not.toBeNull();
    });

    it('rejects non-image files without uploading', async () => {
        const onChange = jest.fn();
        const file = new File(['document'], 'document.pdf', { type: 'application/pdf' });

        render(<ImageUploader uploadedUrls={[]} onChange={onChange} />);
        fireEvent.change(screen.getByLabelText('Chọn ảnh tải lên'), {
            target: { files: [file] }
        });

        expect((await screen.findByRole('alert')).textContent).toContain('Vui lòng chỉ chọn tệp hình ảnh.');
        expect(mockedFileService.uploadFile).not.toHaveBeenCalled();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('removes only the selected image from the form value', () => {
        const onChange = jest.fn();
        const first = '/api/public/files/products/first.png';
        const second = '/api/public/files/products/second.png';

        render(<ImageUploader uploadedUrls={[first, second]} onChange={onChange} />);
        fireEvent.click(screen.getAllByTitle('Xóa ảnh')[0]);

        expect(onChange).toHaveBeenCalledWith([second]);
        expect(mockedFileService.uploadFile).not.toHaveBeenCalled();
    });
});
