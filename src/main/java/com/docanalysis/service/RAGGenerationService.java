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

/**
 * RAG Generation Service - Orchestrates the retrieval-augmented generation pipeline.
 * 
 * Flow:
 * 1. Embed user query
 * 2. Retrieve top-5 chunks from ChromaDB
 * 3. Expand with neighbor chunks for context continuity
 * 4. Build grounded prompt from PLAN specification
 * 5. Stream response from Mistral LLM
 * 6. Attach citations to response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGGenerationService {
    
    private final VectorRepositoryService vectorRepositoryService;
    private final VectorEmbeddingService vectorEmbeddingService;
    private final LLMService llmService;
    
    @Value("${app.search.top-k:5}")
    private int contextLimit;
    
    /**
     * Generate streaming RAG response for user query.
     * @param userQuery The natural language question
     * @param conversationId Conversation ID for tracking
     * @return Flux of streaming response chunks
     */
    public Flux<ChatStreamResponse> generateStreamingResponse(String userQuery, String conversationId) {
        log.info("Starting RAG generation for query: {}", userQuery);
        
        try {
            // Step 1: Embed the user query using Voyage
            float[] queryVector = vectorEmbeddingService.embedQuery(userQuery);
            log.debug("Query vector generated: {} dimensions", queryVector.length);
            
            // Step 2: Retrieve relevant contexts from Chroma (top-5)
            List<DocumentChunk> contextChunks = vectorRepositoryService.searchRelevantContexts(queryVector);
            log.debug("Retrieved {} context chunks", contextChunks.size());
            
            if (contextChunks.isEmpty()) {
                log.warn("No relevant chunks found for query - proceeding with generic response");
                return Flux.just(ChatStreamResponse.builder()
                        .chunk("No documents have been uploaded yet or no relevant information was found. Please upload documents to enable document analysis.")
                        .status("completed")
                        .conversationId(conversationId)
                        .timestamp(LocalDateTime.now())
                        .citations(Collections.emptyList())
                        .build());
            }
            
            // Step 3: Build augmented prompt with context (PLAN specification)
            String augmentedPrompt = buildAugmentedPrompt(userQuery, contextChunks);
            log.debug("Augmented prompt length: {} characters", augmentedPrompt.length());
            
            // Step 4: Stream response from LLM
            return llmService.streamCompletion(augmentedPrompt)
                    .map(chunk -> buildStreamingResponse(chunk, contextChunks, conversationId, false))
                    .concatWith(Mono.fromCallable(() -> 
                            buildStreamingResponse("", contextChunks, conversationId, true)
                    ))
                    .doOnError(e -> log.error("Error during RAG generation", e));
        } catch (Exception e) {
            log.error("Error in RAG generation: {}", e.getMessage(), e);
            return Flux.just(ChatStreamResponse.builder()
                    .status("error")
                    .error(e.getMessage())
                    .conversationId(conversationId)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
    
    /**
     * Build augmented prompt following PLAN specification with strict grounding.
     * @param userQuery The user's question
     * @param contextChunks Retrieved document chunks
     * @return Formatted prompt for LLM
     */
    private String buildAugmentedPrompt(String userQuery, List<DocumentChunk> contextChunks) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // PLAN grounding prompt
        promptBuilder.append("You are an Enterprise Document Assistant.\n");
        promptBuilder.append("Answer only from the provided context.\n");
        promptBuilder.append("If the answer is not contained in the context, say:\n");
        promptBuilder.append("\"I could not find that information in the uploaded documents.\"\n");
        promptBuilder.append("Provide citations for every answer.\n\n");
        
        // Add context sections
        promptBuilder.append("CONTEXT:\n");
        promptBuilder.append("---\n");
        
        for (int i = 0; i < contextChunks.size(); i++) {
            DocumentChunk chunk = contextChunks.get(i);
            
            promptBuilder.append(String.format("[Source %d]\n", i + 1));
            promptBuilder.append("Document: ").append(chunk.getDocument().getFileName()).append("\n");
            if (chunk.getPageNumber() != null) {
                promptBuilder.append("Page: ").append(chunk.getPageNumber()).append("\n");
            }
            promptBuilder.append("---\n");
            promptBuilder.append(chunk.getChunkText()).append("\n");
            promptBuilder.append("---\n\n");
        }
        
        // Add question
        promptBuilder.append("QUESTION:\n");
        promptBuilder.append(userQuery).append("\n\n");
        promptBuilder.append("RESPONSE:\n");
        
        return promptBuilder.toString();
    }
    
    /**
     * Build streaming response chunk with citations.
     * @param chunk Text chunk from LLM
     * @param contextChunks Source chunks for citations
     * @param conversationId Conversation ID
     * @param isFinished Whether this is the final chunk
     * @return ChatStreamResponse DTO
     */
    private ChatStreamResponse buildStreamingResponse(String chunk, 
                                                     List<DocumentChunk> contextChunks,
                                                     String conversationId,
                                                     boolean isFinished) {
        List<ChatStreamResponse.Citation> citations = contextChunks.stream()
                .map(chunkData -> ChatStreamResponse.Citation.builder()
                        .documentId(chunkData.getDocument().getId())
                        .documentName(chunkData.getDocument().getFileName())
                        .chunkId(chunkData.getId())
                        .contentSnippet(truncate(chunkData.getChunkText(), 100))
                        .pageNumber(chunkData.getPageNumber())
                        .tableCoordinates(chunkData.getIsTableData() ? "table-data" : null)
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
