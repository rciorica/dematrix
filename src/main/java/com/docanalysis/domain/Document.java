package com.docanalysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_path_ref", nullable = false)
    private String filePathRef;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "owner_id")
    private String ownerId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType;
    
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;
    
    @Column(name = "chunk_count")
    private Integer chunkCount;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        status = "PENDING";
    }
}
