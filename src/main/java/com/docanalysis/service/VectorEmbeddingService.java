package com.docanalysis.service;

import com.docanalysis.domain.DocumentChunk;
import com.docanalysis.exception.DocumentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Generates embeddings using Voyage AI and stores them in ChromaDB.
 * Embeddings are stored ONLY in ChromaDB to avoid synchronization issues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorEmbeddingService {

    private final ChromaDbService chromaDbService;
    private final RestTemplate restTemplate;

    @Value("${app.voyage.api-key:}")
    private String voyageApiKey;

    @Value("${app.voyage.base-url:https://api.voyageai.com/v1/embeddings}")
    private String voyageBaseUrl;

    @Value("${app.voyage.model:voyage-3}")
    private String voyageModel;

    /**
     * Generate Voyage embedding and store in ChromaDB only.
     * @param chunk The document chunk to embed
     */
    public void generateAndStoreEmbedding(DocumentChunk chunk) {
        try {
            log.debug("Generating embedding for chunk: {}", chunk.getId());

            float[] vector = getEmbeddingVector(chunk.getChunkText());

            // Store ONLY in ChromaDB (never in PostgreSQL)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", chunk.getDocument().getId());
            metadata.put("document_name", chunk.getDocument().getFileName());
            metadata.put("chunk_id", chunk.getId());
            metadata.put("page_number", chunk.getPageNumber());
            metadata.put("is_table_data", chunk.getIsTableData());
            
            chromaDbService.upsert(
                    "documents",
                    List.of(chunk.getId()),
                    List.of(vector),
                    List.of(chunk.getChunkText()),
                    List.of(metadata)
            );
            
            log.info("Embedding stored successfully for chunk: {} in ChromaDB", chunk.getId());
        } catch (Exception e) {
            log.error("Failed to generate embedding for chunk: {}", chunk.getId(), e);
            throw new DocumentProcessingException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embedding vector for text using Voyage AI.
     * @param text The text to embed
     * @return float array representing the embedding
     */
    public float[] getEmbeddingVector(String text) {
        try {
            if (text == null || text.isBlank()) {
                return new float[0];
            }
            if (voyageApiKey != null && !voyageApiKey.isBlank()) {
                return callVoyageApi(text);
            }
            log.warn("Voyage API key is not configured; falling back to deterministic embedding");
            return generateMockEmbedding(text);
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new DocumentProcessingException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    private float[] callVoyageApi(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(voyageApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> request = new HashMap<>();
        request.put("input", List.of(text));
        request.put("model", voyageModel);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        VoyageEmbeddingResponse response = restTemplate.postForObject(voyageBaseUrl, entity, VoyageEmbeddingResponse.class);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new DocumentProcessingException("Voyage API returned an empty embedding response");
        }

        List<Double> embeddingValues = response.getData().get(0).getEmbedding();
        if (embeddingValues == null || embeddingValues.isEmpty()) {
            throw new DocumentProcessingException("Voyage API returned no embedding values");
        }

        float[] result = new float[embeddingValues.size()];
        for (int i = 0; i < embeddingValues.size(); i++) {
            result[i] = embeddingValues.get(i).floatValue();
        }
        return result;
    }

    private float[] generateMockEmbedding(String text) {
        int dimension = 384;
        float[] vector = new float[dimension];

        long hashValue = text.hashCode();
        for (int i = 0; i < dimension; i++) {
            hashValue = (hashValue * 1103515245 + 12345) & 0x7fffffff;
            vector[i] = (float) ((hashValue % 1000) / 1000.0 - 0.5);
        }

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

    /**
     * Embed a query for retrieval.
     * @param query The query text
     * @return float array embedding
     */
    public float[] embedQuery(String query) {
        return getEmbeddingVector(query);
    }

    static class VoyageEmbeddingResponse {
        private List<VoyageEmbeddingEntry> data;

        public List<VoyageEmbeddingEntry> getData() {
            return data;
        }

        public void setData(List<VoyageEmbeddingEntry> data) {
            this.data = data;
        }
    }

    static class VoyageEmbeddingEntry {
        private List<Double> embedding;

        public List<Double> getEmbedding() {
            return embedding;
        }

        public void setEmbedding(List<Double> embedding) {
            this.embedding = embedding;
        }
    }
}
