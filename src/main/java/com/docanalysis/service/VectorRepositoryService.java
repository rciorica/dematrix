package com.docanalysis.service;

import com.docanalysis.domain.DocumentChunk;
import com.docanalysis.domain.EmbeddingVector;
import com.docanalysis.repository.DocumentChunkRepository;
import com.docanalysis.repository.EmbeddingVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorRepositoryService {
    
    private final EmbeddingVectorRepository embeddingVectorRepository;
    private final DocumentChunkRepository documentChunkRepository;
    
    @Value("${app.search.top-k:5}")
    private int topK;
    
    @Value("${app.search.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    public List<DocumentChunk> searchRelevantContexts(float[] queryVector) {
        log.debug("Searching for relevant contexts with query vector of dimension: {}", queryVector.length);
        
        List<EmbeddingVector> allEmbeddings = embeddingVectorRepository.findAll();
        
        // Calculate cosine similarity for all embeddings
        List<ScoredChunk> scoredChunks = allEmbeddings.stream()
                .map(embedding -> {
                    double similarity = cosineSimilarity(queryVector, embedding.getVectorData());
                    return new ScoredChunk(embedding.getDocumentChunk(), similarity);
                })
                .filter(sc -> sc.score >= similarityThreshold)
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        
        log.debug("Found {} relevant chunks above threshold {}", scoredChunks.size(), similarityThreshold);
        
        return scoredChunks.stream()
                .map(sc -> sc.chunk)
                .collect(Collectors.toList());
    }
    
    public List<ScoredChunk> searchWithScores(float[] queryVector) {
        List<EmbeddingVector> allEmbeddings = embeddingVectorRepository.findAll();
        
        List<ScoredChunk> scoredChunks = allEmbeddings.stream()
                .map(embedding -> {
                    double similarity = cosineSimilarity(queryVector, embedding.getVectorData());
                    return new ScoredChunk(embedding.getDocumentChunk(), similarity);
                })
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        
        return scoredChunks;
    }
    
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            normA += vec1[i] * vec1[i];
            normB += vec2[i] * vec2[i];
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    public static class ScoredChunk {
        public DocumentChunk chunk;
        public double score;
        
        public ScoredChunk(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
        
        public double getScore() {
            return score;
        }
    }
}
