package com.vn.sodu.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements StorageService {

    private final Path rootLocation;

    public LocalStorageServiceImpl(StorageProperties storageProperties) {
        this.rootLocation = Paths.get(storageProperties.getLocal().getDir());
        init();
    }

    private void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
            
            String generatedFilename = UUID.randomUUID().toString() + extension;
            
            Path destinationDir = rootLocation;
            if (subDirectory != null && !subDirectory.isEmpty()) {
                destinationDir = rootLocation.resolve(subDirectory);
                Files.createDirectories(destinationDir);
            }
            
            Path destinationFile = destinationDir.resolve(Paths.get(generatedFilename))
                    .normalize().toAbsolutePath();
                    
            if (!destinationFile.getParent().equals(destinationDir.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativePath = subDirectory != null && !subDirectory.isEmpty() ? 
                subDirectory + "/" + generatedFilename : generatedFilename;

            // Generate full URL
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/public/files/")
                    .path(relativePath)
                    .toUriString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot access file outside storage directory");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return;
            }
            // Extract filename from URL (e.g., http://localhost:8081/api/public/files/abc.png -> abc.png)
            String filename = fileUrl.substring(fileUrl.indexOf("/api/public/files/") + "/api/public/files/".length());
            Path file = rootLocation.resolve(filename).normalize();
            
            if (file.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                Files.deleteIfExists(file);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete file " + fileUrl + ": " + e.getMessage());
        }
    }
}
