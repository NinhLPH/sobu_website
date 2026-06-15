package com.vn.sodu.storage;

import com.vn.sodu.global.dto.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /**
     * Admin endpoint to upload files.
     * Returns the access URL of the uploaded file.
     */
    @PostMapping("/api/admin/files/upload")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subDirectory", required = false) String subDirectory) {
        
        String fileUrl = storageService.store(file, subDirectory);
        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        
        return ResponseEntity.ok(ApiResponseDTO.success(response, "File uploaded successfully"));
    }

    /**
     * Public endpoint to view/download files.
     */
    @GetMapping("/api/public/files/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        // Extract the filename path component after /api/public/files/
        String filename = requestURI.substring(requestURI.indexOf("/api/public/files/") + "/api/public/files/".length());
        
        Resource file = storageService.loadAsResource(filename);
        
        String contentType = "application/octet-stream";
        try {
            contentType = request.getServletContext().getMimeType(file.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Fallback
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}
