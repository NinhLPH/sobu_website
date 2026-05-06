package com.vn.sodu.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Stores a file and returns its public access URL
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * Loads a file as a Resource
     */
    Resource loadAsResource(String filename);

    /**
     * Deletes a file by its URL or filename
     */
    void delete(String fileUrl);
}
