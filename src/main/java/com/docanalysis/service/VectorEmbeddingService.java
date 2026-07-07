package com.docanalysis.service;

import com.docanalysis.domain.DocumentChunk;
import com.docanalysis.domain.EmbeddingVector;
import com.docanalysis.exception.DocumentProcessingException;
import com.docanalysis.repository.EmbeddingVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorEmbeddingService {
    
    private final EmbeddingVectorRepository embeddingVectorRepository;
    private final RestTemplate restTemplate;
    
    @Value("${app.chroma.url:http://localhost:8000}")
    private String chromaUrl;
    
    @Value("${app.chroma.embedding-model:all-MiniLM-L6-v2}")
    private String embeddingModel;
    
    public void generateAndStoreEmbedding(DocumentChunk chunk) {
        try {
            log.debug("Generating embedding for chunk: {}", chunk.getId());
            
            float[] vector = getEmbeddingVector(chunk.getChunkText());
            
            EmbeddingVector embeddingVector = EmbeddingVector.builder()
                    .documentChunk(chunk)
                    .vectorData(vector)
                    .embeddingModel(embeddingModel)
                    .dimension(vector.length)
                    .build();
            
            embeddingVectorRepository.save(embeddingVector);
            log.debug("Embedding stored successfully for chunk: {}", chunk.getId());
        } catch (Exception e) {
            log.error("Failed to generate embedding for chunk: {}", chunk.getId(), e);
            throw new DocumentProcessingException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    public float[] getEmbeddingVector(String text) {
        try {
            // For demonstration, using a local mock embedding service
            // In production, integrate with Ollama or OpenAI
            return generateMockEmbedding(text);
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new DocumentProcessingException("Embedding generation failed: " + e.getMessage(), e);
        }
    }
    
    private float[] generateMockEmbedding(String text) {
        // Mock embedding: 384-dim vector based on text hash
        int dimension = 384;
        float[] vector = new float[dimension];
        
        long hashValue = text.hashCode();
        for (int i = 0; i < dimension; i++) {
            hashValue = (hashValue * 1103515245 + 12345) & 0x7fffffff;
            vector[i] = (float) ((hashValue % 1000) / 1000.0 - 0.5);
        }
        
        // Normalize vector
        float norm = 0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
    }
    
    public float[] embedQuery(String query) {
        return getEmbeddingVector(query);
    }
}
