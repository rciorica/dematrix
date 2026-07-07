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
public class ChatStreamResponse {
    
    private String id;
    private String chunk;
    private String role;
    private LocalDateTime timestamp;
    private List<Citation> citations;
    private String model;
    private Integer tokensUsed;
    private Double confidenceScore;
    private String conversationId;
    private String status; // "streaming", "completed", "error"
    private String error;
    private Boolean isFinished;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Citation {
        private String documentId;
        private String documentName;
        private String chunkId;
        private String contentSnippet;
        private Double relevanceScore;
        private Integer pageNumber;
        private String tableCoordinates;
    }
}
