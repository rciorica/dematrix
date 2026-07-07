package com.docanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    
    private String id;
    private String message;
    private String role; // "assistant" or "user"
    private LocalDateTime timestamp;
    private List<SourceDocument> sources;
    private String model;
    private Integer tokensUsed;
    private Double confidenceScore;
    private String conversationId;
    private Boolean isStreaming;
    private String error;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceDocument {
        private String documentId;
        private String documentName;
        private String chunkId;
        private String content;
        private Double relevanceScore;
        private Integer pageNumber;
        private Long startOffset;
        private Long endOffset;
    }
    
    public static ChatResponse error(String error) {
        return ChatResponse.builder()
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatResponse success(String message, String conversationId) {
        return ChatResponse.builder()
                .message(message)
                .role("assistant")
                .timestamp(LocalDateTime.now())
                .conversationId(conversationId)
                .build();
    }
}