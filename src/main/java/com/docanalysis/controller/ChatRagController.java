package com.docanalysis.controller;

import com.docanalysis.dto.ChatQueryRequest;
import com.docanalysis.dto.ChatStreamResponse;
import com.docanalysis.service.RAGGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * Chat RAG Controller - Handles RAG queries.
 * Returns server-sent events for incremental chat updates.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRagController {
    
    private final RAGGenerationService ragGenerationService;
    
    /**
     * Chat endpoint - SSE streaming response.
     * @param request Chat query request with query and conversationId
     * @return Flux of streaming chat events
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChatResponse(@RequestBody ChatQueryRequest request) {
        log.info("Chat stream request received: {} (conversation: {})", 
                request.getQuery(), request.getConversationId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("Empty query received");
            return Flux.just(ServerSentEvent.<ChatStreamResponse>builder()
                .event("error")
                .data(ChatStreamResponse.builder()
                    .status("error")
                    .error("Query must not be empty")
                    .isFinished(true)
                    .build())
                .build());
        }
        
        String conversationId = request.getConversationId() != null ? 
                request.getConversationId() : generateConversationId();
        
        log.debug("Starting RAG generation for conversation: {}", conversationId);

        return ragGenerationService.generateStreamingResponse(request.getQuery(), conversationId)
            .map(response -> ServerSentEvent.builder(response).build())
            .onErrorResume(e -> {
                log.error("Error during chat: {}", e.getMessage(), e);
                ChatStreamResponse errorResponse = ChatStreamResponse.builder()
                    .chunk("Error: Could not generate response. " + e.getMessage())
                    .status("error")
                    .conversationId(conversationId)
                    .error(e.getMessage())
                    .isFinished(true)
                    .build();
                return Flux.just(ServerSentEvent.<ChatStreamResponse>builder()
                    .event("error")
                    .data(errorResponse)
                    .build());
            })
            .doOnComplete(() -> log.info("Stream completed for conversation: {}", conversationId));
    }
    
    /**
     * Health check endpoint.
     * @return Health status
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "chat-rag");
        return response;
    }
    
    private String generateConversationId() {
        return "conv-" + System.currentTimeMillis();
    }
}
