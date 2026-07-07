package com.docanalysis.controller;

import com.docanalysis.dto.ChatQueryRequest;
import com.docanalysis.dto.ChatStreamResponse;
import com.docanalysis.service.RAGGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatRagController {
    
    private final RAGGenerationService ragGenerationService;
    
    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> streamChatResponse(@RequestBody ChatQueryRequest request) {
        log.info("Chat request received: {} (conversation: {})", 
                request.getQuery(), request.getConversationId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Query cannot be empty");
            error.put("status", "error");
            return ResponseEntity.badRequest().body(error);
        }
        
        String conversationId = request.getConversationId() != null ? 
                request.getConversationId() : generateConversationId();
        
        try {
            // Collect all streaming chunks into a single response
            StringBuilder fullResponse = new StringBuilder();
            ragGenerationService.generateStreamingResponse(request.getQuery(), conversationId)
                    .subscribe(
                            chunk -> {
                                log.debug("Chunk received: {}", chunk.getChunk());
                                if (chunk.getChunk() != null) {
                                    fullResponse.append(chunk.getChunk());
                                }
                            },
                            error -> log.error("Error during streaming", error),
                            () -> log.info("Stream completed with response length: {}", fullResponse.length())
                    );
            
            // Wait a bit for async processing
            Thread.sleep(2000);
            
            Map<String, Object> response = new HashMap<>();
            response.put("response", fullResponse.toString());
            response.put("query", request.getQuery());
            response.put("conversationId", conversationId);
            response.put("status", "success");
            
            log.info("Returning response with {} characters", fullResponse.length());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private String generateConversationId() {
        return "conv-" + System.currentTimeMillis();
    }
}
