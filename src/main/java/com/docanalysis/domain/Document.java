package com.docanalysis.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_owner_id", columnList = "owner_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 500)
    private String fileName;
    
    @Column(nullable = false)
    private String filePathRef;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(name = "owner_id")
    private String ownerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime processedAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Long fileSize = 0L;
    
    @Column(length = 50)
    private String fileType;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<DocumentChunk> chunks = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String extractedText;
    
    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
    
    public enum DocumentStatus {
        PENDING, PROCESSING, INDEXED, FAILED
    }
    
    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", status=" + status +
                ", uploadedAt=" + uploadedAt +
                ", chunkCount=" + chunkCount +
                '}';
    }
}
