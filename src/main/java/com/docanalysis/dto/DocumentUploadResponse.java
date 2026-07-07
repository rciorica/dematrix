package com.docanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {
    
    private String documentId;
    private String fileName;
    private String title;
    private String status;
    private Integer chunkCount;
    private Long uploadedAtMillis;
    private String message;
}
