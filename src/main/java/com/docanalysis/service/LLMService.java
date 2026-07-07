package com.docanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {
    
    @Value("${app.llm.provider:mock}")
    private String provider;
    
    @Value("${app.llm.temperature:0.7}")
    private double temperature;
    
    public Flux<String> streamCompletion(String prompt) {
        log.debug("Generating streaming completion with provider: {}", provider);
        
        if ("mock".equals(provider)) {
            return generateMockStreamingResponse(prompt);
        }
        
        // In production, integrate with OpenAI, Ollama, etc.
        return generateMockStreamingResponse(prompt);
    }
    
    private Flux<String> generateMockStreamingResponse(String prompt) {
        String mockResponse = """
                Based on the provided document context, I can help you analyze and understand the information.
                
                The key findings from the relevant documents are:
                
                1. [Source 1] The document contains important information related to your query.
                2. [Source 2] Additional context shows that this point is significant.
                3. [Source 3] Further analysis reveals additional details worth noting.
                
                The documents collectively provide a comprehensive view of the topic, allowing for 
                informed decision-making based on verified facts and citations.
                """;
        
        // Simulate streaming by splitting into words with slight delay
        return Flux.fromIterable(Arrays.asList(mockResponse.split("\\s+")))
                .map(word -> word + " ");
    }
}
