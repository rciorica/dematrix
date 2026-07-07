package com.docanalysis.service;

import com.docanalysis.domain.DocumentChunk;
import com.docanalysis.dto.ChatStreamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGGenerationService {
    
    private final VectorRepositoryService vectorRepositoryService;
    private final VectorEmbeddingService vectorEmbeddingService;
    private final LLMService llmService;
    
    @Value("${app.search.top-k:5}")
    private int contextLimit;
    
    public Flux<ChatStreamResponse> generateStreamingResponse(String userQuery, String conversationId) {
        log.info("Starting RAG generation for query: {}", userQuery);
        
        try {
            // Step 1: Embed the user query
            float[] queryVector = vectorEmbeddingService.embedQuery(userQuery);
            
            // Step 2: Retrieve relevant contexts
            List<VectorRepositoryService.ScoredChunk> scoredChunks = vectorRepositoryService.searchWithScores(queryVector);
            log.debug("Retrieved {} context chunks", scoredChunks.size());
            
            // Step 3: Build augmented prompt with context
            String augmentedPrompt = buildAugmentedPrompt(userQuery, scoredChunks);
            
            // Step 4: Stream response from LLM
            return llmService.streamCompletion(augmentedPrompt)
                    .map(chunk -> buildStreamingResponse(chunk, scoredChunks, conversationId, false))
                    .concatWith(Mono.fromCallable(() -> 
                            buildStreamingResponse("", scoredChunks, conversationId, true)
                    ));
        } catch (Exception e) {
            log.error("Error in RAG generation", e);
            return Flux.just(ChatStreamResponse.builder()
                    .status("error")
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
    
    private String buildAugmentedPrompt(String userQuery, List<VectorRepositoryService.ScoredChunk> scoredChunks) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("You are an enterprise document analysis assistant. ")
                .append("Answer the following query based on the provided document context.\n\n");
        
        // Add context sections
        promptBuilder.append("DOCUMENT CONTEXT:\n");
        promptBuilder.append("---\n");
        
        for (int i = 0; i < scoredChunks.size(); i++) {
            VectorRepositoryService.ScoredChunk scored = scoredChunks.get(i);
            DocumentChunk chunk = scored.chunk;
            
            promptBuilder.append(String.format("[Source %d - Relevance: %.2f]\n", i + 1, scored.score));
            promptBuilder.append("Document: ").append(chunk.getDocument().getFileName()).append("\n");
            if (chunk.getPageNumber() != null) {
                promptBuilder.append("Page: ").append(chunk.getPageNumber()).append("\n");
            }
            promptBuilder.append(chunk.getChunkText()).append("\n");
            promptBuilder.append("---\n");
        }
        
        promptBuilder.append("\nUSER QUERY:\n");
        promptBuilder.append(userQuery).append("\n\n");
        promptBuilder.append("INSTRUCTIONS:\n");
        promptBuilder.append("1. Answer based on the provided context.\n");
        promptBuilder.append("2. Cite sources using [Source X] notation.\n");
        promptBuilder.append("3. Be precise and factual.\n");
        promptBuilder.append("4. If information is not in the context, state that clearly.\n\n");
        promptBuilder.append("RESPONSE:\n");
        
        return promptBuilder.toString();
    }
    
    private ChatStreamResponse buildStreamingResponse(String chunk, 
                                                     List<VectorRepositoryService.ScoredChunk> scoredChunks,
                                                     String conversationId,
                                                     boolean isFinished) {
        List<ChatStreamResponse.Citation> citations = scoredChunks.stream()
                .map(scored -> ChatStreamResponse.Citation.builder()
                        .documentId(scored.chunk.getDocument().getId())
                        .documentName(scored.chunk.getDocument().getFileName())
                        .chunkId(scored.chunk.getId())
                        .contentSnippet(truncate(scored.chunk.getChunkText(), 100))
                        .relevanceScore(scored.score)
                        .pageNumber(scored.chunk.getPageNumber())
                        .tableCoordinates(scored.chunk.getIsTableData() ? "table-data" : null)
                        .build())
                .collect(Collectors.toList());
        
        return ChatStreamResponse.builder()
                .chunk(chunk)
                .role("assistant")
                .status(isFinished ? "completed" : "streaming")
                .citations(isFinished ? citations : Collections.emptyList())
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .isFinished(isFinished)
                .build();
    }
    
    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
