package com.docanalysis.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_sequence", columnList = "document_id, sequence_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;
    
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;
    
    @Column(name = "page_number")
    private Integer pageNumber;
    
    @Column(name = "start_offset")
    private Long startOffset;
    
    @Column(name = "end_offset")
    private Long endOffset;
    
    @ElementCollection
    @CollectionTable(name = "chunk_references", joinColumns = @JoinColumn(name = "chunk_id"))
    @Column(name = "reference_value")
    @Builder.Default
    private Set<String> references = new HashSet<>();
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column(name = "is_table_data")
    @Builder.Default
    private Boolean isTableData = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "DocumentChunk{" +
                "id='" + id + '\'' +
                ", documentId='" + (document != null ? document.getId() : "null") + '\'' +
                ", sequenceOrder=" + sequenceOrder +
                ", pageNumber=" + pageNumber +
                ", chunkTextLength=" + (chunkText != null ? chunkText.length() : 0) +
                '}';
    }
}
