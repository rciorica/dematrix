package com.docanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;
    
    public String storeFile(MultipartFile file) throws IOException {
        log.debug("Storing file: {}", file.getOriginalFilename());
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        
        // Generate unique filename
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "." + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        
        // Store file
        Files.copy(file.getInputStream(), filePath);
        
        log.debug("File stored at: {}", filePath);
        return filePath.toString();
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "tmp";
    }
}
