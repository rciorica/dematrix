package com.docanalysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Arrays;

@Entity
@Table(name = "embedding_vectors", indexes = {
    @Index(name = "idx_chunk_id", columnList = "document_chunk_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingVector {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_chunk_id", nullable = false)
    private DocumentChunk documentChunk;
    
    @Column(name = "vector_data", columnDefinition = "FLOAT8[]")
    private float[] vectorData;
    
    @Column(name = "embedding_model")
    private String embeddingModel;
    
    @Column(name = "dimension")
    private Integer dimension;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public double cosineSimilarity(float[] otherVector) {
        if (vectorData == null || otherVector == null) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorData.length; i++) {
            dotProduct += vectorData[i] * otherVector[i];
            normA += vectorData[i] * vectorData[i];
            normB += otherVector[i] * otherVector[i];
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    @Override
    public String toString() {
        return "EmbeddingVector{" +
                "id='" + id + '\'' +
                ", documentChunkId='" + (documentChunk != null ? documentChunk.getId() : "null") + '\'' +
                ", embeddingModel='" + embeddingModel + '\'' +
                ", dimension=" + dimension +
                ", createdAt=" + createdAt +
                '}';
    }
}