package com.docanalysis.service;

import com.docanalysis.domain.DocumentChunk;
import com.docanalysis.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Retrieves relevant document chunks using Chroma vector search.
 * Implements neighbor chunk expansion for improved context continuity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorRepositoryService {
    
    private final DocumentChunkRepository documentChunkRepository;
    private final ChromaDbService chromaDbService;
    
    @Value("${app.search.top-k:5}")
    private int topK;
    
    @Value("${app.search.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    /**
     * Search for relevant chunks and expand with neighbors for context continuity.
     * @param queryVector The embedding vector to search for
     * @return List of document chunks with neighbor expansion
     */
    public List<DocumentChunk> searchRelevantContexts(float[] queryVector) {
        log.debug("Searching for relevant contexts with query vector of dimension: {}", queryVector.length);
        
        List<ScoredChunk> scoredChunks = searchWithScores(queryVector);
        
        // Expand with neighbor chunks for context continuity (MVP Phase 2 feature)
        List<DocumentChunk> expandedChunks = expandWithNeighbors(scoredChunks);
        
        log.debug("After neighbor expansion: {} chunks", expandedChunks.size());
        return expandedChunks;
    }
    
    /**
     * Search Chroma and return scored results.
     * @param queryVector The embedding vector
     * @return List of scored chunks
     */
    public List<ScoredChunk> searchWithScores(float[] queryVector) {
        try {
            // Query Chroma for top-k similar embeddings
            List<ChromaDbService.ChromaQueryResult> chromaResults = chromaDbService.query("documents", queryVector, topK);
            
            if (chromaResults.isEmpty()) {
                log.warn("Chroma returned no results for query");
                return new ArrayList<>();
            }
            
            List<ScoredChunk> scoredChunks = new ArrayList<>();
            for (ChromaDbService.ChromaQueryResult result : chromaResults) {
                String chunkId = result.getId();
                DocumentChunk chunk = documentChunkRepository.findById(chunkId).orElse(null);
                if (chunk != null) {
                    // Chroma returns distances; convert to similarity (1 - distance for normalized vectors)
                    double similarity = 1.0 - (result.getDistance() != null ? result.getDistance() : 1.0);
                    scoredChunks.add(new ScoredChunk(chunk, similarity));
                }
            }
            
            log.debug("Found {} relevant chunks via Chroma search", scoredChunks.size());
            return scoredChunks;
        } catch (Exception e) {
            log.warn("Error querying Chroma", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Expand retrieved chunks with neighboring chunks for better context.
     * Example: if chunk 20 retrieved, also include chunks 19, 20, 21.
     * @param scoredChunks Initial retrieved chunks
     * @return Expanded chunk list with neighbors, maintaining order
     */
    private List<DocumentChunk> expandWithNeighbors(List<ScoredChunk> scoredChunks) {
        Set<String> includedChunkIds = new HashSet<>();
        List<DocumentChunk> expandedChunks = new ArrayList<>();
        
        for (ScoredChunk scored : scoredChunks) {
            DocumentChunk chunk = scored.chunk;
            String documentId = chunk.getDocument().getId();
            int sequenceOrder = chunk.getSequenceOrder();
            
            // Add previous neighbor
            Optional<DocumentChunk> prevChunk = documentChunkRepository
                    .findByDocumentIdAndSequenceOrder(documentId, sequenceOrder - 1);
            if (prevChunk.isPresent() && !includedChunkIds.contains(prevChunk.get().getId())) {
                expandedChunks.add(prevChunk.get());
                includedChunkIds.add(prevChunk.get().getId());
                log.debug("Added neighbor chunk {}-{}", documentId, sequenceOrder - 1);
            }
            
            // Add main chunk
            if (!includedChunkIds.contains(chunk.getId())) {
                expandedChunks.add(chunk);
                includedChunkIds.add(chunk.getId());
            }
            
            // Add next neighbor
            Optional<DocumentChunk> nextChunk = documentChunkRepository
                    .findByDocumentIdAndSequenceOrder(documentId, sequenceOrder + 1);
            if (nextChunk.isPresent() && !includedChunkIds.contains(nextChunk.get().getId())) {
                expandedChunks.add(nextChunk.get());
                includedChunkIds.add(nextChunk.get().getId());
                log.debug("Added neighbor chunk {}-{}", documentId, sequenceOrder + 1);
            }
        }
        
        return expandedChunks;
    }
    
    /**
     * Scored chunk with similarity score from vector search.
     */
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
