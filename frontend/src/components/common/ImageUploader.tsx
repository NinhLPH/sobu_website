import React, { DragEvent, useRef, useState } from 'react';
import { Loader2, Trash2, Upload } from 'lucide-react';
import { FileService } from '../../service/file.service';
import { getPublicImageUrl } from '../../utils/file-url';

interface ImageUploaderProps {
    uploadedUrls: string[];
    onChange: (uploadedUrls: string[]) => void;
    disabled?: boolean;
    subDirectory?: string;
    onUploadingChange?: (isUploading: boolean) => void;
}

export default function ImageUploader({
    uploadedUrls,
    onChange,
    disabled = false,
    subDirectory = 'requests',
    onUploadingChange
}: ImageUploaderProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [isDragging, setIsDragging] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState<string | null>(null);

    const uploadFiles = async (files: File[]) => {
        if (disabled || isUploading || files.length === 0) {
            return;
        }

        const imageFiles = files.filter((file) => file.type.startsWith('image/'));
        if (imageFiles.length !== files.length) {
            setUploadError('Vui lòng chỉ chọn tệp hình ảnh.');
            return;
        }

        setUploadError(null);
        setIsUploading(true);
        onUploadingChange?.(true);

        const newUrls: string[] = [];

        try {
            for (const file of imageFiles) {
                const response = await FileService.uploadFile(file, subDirectory);
                if (!response?.url) {
                    throw new Error('Upload response did not include a file URL.');
                }
                newUrls.push(response.url);
            }

            onChange([...uploadedUrls, ...newUrls]);
        } catch {
            if (newUrls.length > 0) {
                onChange([...uploadedUrls, ...newUrls]);
            }
            setUploadError('Tải ảnh lên thất bại. Vui lòng thử lại.');
        } finally {
            setIsUploading(false);
            onUploadingChange?.(false);
            if (inputRef.current) {
                inputRef.current.value = '';
            }
        }
    };

    const handleDrop = (event: DragEvent<HTMLLabelElement>) => {
        event.preventDefault();
        setIsDragging(false);
        void uploadFiles(Array.from(event.dataTransfer.files));
    };

    const handleRemove = (index: number) => {
        if (disabled || isUploading) {
            return;
        }
        onChange(uploadedUrls.filter((_, itemIndex) => itemIndex !== index));
    };

    return (
        <div className="space-y-3">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                {uploadedUrls.map((url, index) => (
                    <div
                        key={`${url}-${index}`}
                        className="group relative aspect-square overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container p-1"
                    >
                        <a
                            href={getPublicImageUrl(url)}
                            target="_blank"
                            rel="noreferrer"
                            className="block h-full w-full"
                            title="Mở ảnh"
                        >
                            <img
                                src={getPublicImageUrl(url)}
                                alt={`Ảnh đính kèm ${index + 1}`}
                                className="h-full w-full rounded-lg object-contain"
                            />
                        </a>
                        {!disabled && (
                            <button
                                type="button"
                                onClick={() => handleRemove(index)}
                                disabled={isUploading}
                                className="absolute right-1.5 top-1.5 z-10 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-white shadow-md transition-colors hover:bg-error disabled:cursor-not-allowed disabled:opacity-50"
                                title="Xóa ảnh"
                            >
                                <Trash2 className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>
                ))}

                {!disabled && (
                    <label
                        onDragEnter={(event) => {
                            event.preventDefault();
                            setIsDragging(true);
                        }}
                        onDragOver={(event) => event.preventDefault()}
                        onDragLeave={() => setIsDragging(false)}
                        onDrop={handleDrop}
                        className={`relative flex aspect-square cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed transition-all ${
                            isDragging
                                ? 'border-primary bg-primary/10'
                                : 'border-outline-variant/30 bg-surface-container hover:border-primary/50 hover:bg-surface-container-high'
                        } ${isUploading ? 'pointer-events-none opacity-70' : ''}`}
                    >
                        <input
                            ref={inputRef}
                            type="file"
                            multiple
                            accept="image/*"
                            aria-label="Chọn ảnh tải lên"
                            onChange={(event) => {
                                void uploadFiles(Array.from(event.target.files || []));
                            }}
                            disabled={isUploading}
                            className="hidden"
                        />
                        {isUploading ? (
                            <>
                                <Loader2 className="mb-1.5 h-6 w-6 animate-spin text-primary" />
                                <span className="text-[10px] font-bold text-outline">Đang tải ảnh...</span>
                            </>
                        ) : (
                            <>
                                <Upload className="mb-1.5 h-6 w-6 text-outline/80" />
                                <span className="px-2 text-center text-[10px] font-bold text-outline">
                                    Chọn hoặc kéo thả ảnh
                                </span>
                            </>
                        )}
                    </label>
                )}
            </div>

            {uploadError && (
                <p className="text-xs font-bold text-error" role="alert">
                    {uploadError}
                </p>
            )}
        </div>
    );
}
