package com.docanalysis.controller;

import com.docanalysis.dto.DocumentUploadResponse;
import com.docanalysis.service.DocumentProcessingService;
import com.docanalysis.service.FileStorageService;
import com.docanalysis.domain.Document;
import com.docanalysis.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class DocumentIngestionController {
    
    private final DocumentProcessingService documentProcessingService;
    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    
    @GetMapping("/list")
    public ResponseEntity<?> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("Fetching documents: page={}, size={}", page, size);
            
            Page<Document> docs = documentRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"))
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("documents", docs.getContent().stream().map(doc -> Map.of(
                    "documentId", doc.getId().toString(),
                    "id", doc.getId().toString(),
                    "fileName", doc.getFileName(),
                    "title", doc.getTitle() != null ? doc.getTitle() : doc.getFileName(),
                    "status", doc.getStatus() != null ? doc.getStatus().toString() : "INDEXED",
                    "chunkCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                    "uploadedAt", System.currentTimeMillis()
            )).toList());
            response.put("totalElements", docs.getTotalElements());
            response.put("totalPages", docs.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("hasMore", docs.hasNext());
            
            log.info("Returning {} documents, page {}/{}", docs.getContent().size(), page, docs.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "status", "error"));
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        try {
            log.info("Document upload initiated: {}", file.getOriginalFilename());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(DocumentUploadResponse.builder()
                                .message("File is empty")
                                .status("FAILED")
                                .build());
            }
            
            if (!isValidFileType(file.getOriginalFilename())) {
                return ResponseEntity.badRequest()
                        .body(DocumentUploadResponse.builder()
                                .message("Invalid file type. Supported: PDF, TXT")
                                .status("FAILED")
                                .build());
            }
            
            String filePath = fileStorageService.storeFile(file);
            log.debug("File stored successfully at: {}", filePath);
            
            Document document = documentProcessingService.processDocument(
                    file.getOriginalFilename(),
                    title != null ? title : file.getOriginalFilename(),
                    filePath
            );
            
            return ResponseEntity.ok(DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .title(document.getTitle())
                    .status(document.getStatus().toString())
                    .chunkCount(document.getChunkCount())
                    .uploadedAtMillis(System.currentTimeMillis())
                    .message("Document processed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentUploadResponse.builder()
                            .message("Upload failed: " + e.getMessage())
                            .status("FAILED")
                            .build());
        }
    }
    
    private boolean isValidFileType(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".txt");
    }
}
