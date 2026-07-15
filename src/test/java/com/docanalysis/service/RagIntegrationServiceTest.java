package com.docanalysis.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RAG Integration Tests for LLM and Embedding Services.
 * Tests verify correct API integration and data flow.
 */
class RagIntegrationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void llmServiceUsesOllamaForGeneration() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ExchangeFunction exchangeFunction = request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE)
                .body("{\"response\":\"This is an answer from Mistral about the document context.\",\"done\":false}\n"
                    + "{\"done\":true}\n")
                .build()
        );
        WebClient webClient = WebClient.builder()
            .exchangeFunction(exchangeFunction)
            .build();
        LLMService llmService = new LLMService(restTemplate, webClient);
        ReflectionTestUtils.setField(llmService, "ollamaBaseUrl", "http://ollama:11434");
        ReflectionTestUtils.setField(llmService, "modelName", "mistral");
        ReflectionTestUtils.setField(llmService, "temperature", 0.7);

        var tagsResponse = new LLMService.OllamaTagsResponse();
        var modelInfo = new LLMService.OllamaModelInfo();
        modelInfo.setName("mistral");
        tagsResponse.setModels(List.of(modelInfo));
        when(restTemplate.getForObject(anyString(), any(Class.class))).thenReturn(tagsResponse);

        Flux<String> result = llmService.streamCompletion("What does the document say?");
        String combined = result.collectList().block().stream().reduce(String::concat).orElse("");

        assertThat(combined).contains("answer");
        assertThat(combined).contains("Mistral");
    }

    @Test
    @SuppressWarnings("unchecked")
    void embeddingServiceUsesVoyageApiWhenConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ChromaDbService chromaDbService = mock(ChromaDbService.class);
        VectorEmbeddingService service = new VectorEmbeddingService(chromaDbService, restTemplate);
        ReflectionTestUtils.setField(service, "voyageApiKey", "test-key");
        ReflectionTestUtils.setField(service, "voyageBaseUrl", "https://example.test/v1/embeddings");
        ReflectionTestUtils.setField(service, "voyageModel", "voyage-3");

        var response = new VectorEmbeddingService.VoyageEmbeddingResponse();
        var entry = new VectorEmbeddingService.VoyageEmbeddingEntry();
        entry.setEmbedding(List.of(0.1, 0.2, 0.3));
        response.setData(List.of(entry));
        when(restTemplate.postForObject(anyString(), any(), any(Class.class))).thenReturn(response);

        float[] embedding = service.getEmbeddingVector("hello world");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void embeddingServiceFallsBackToMockWhenNoApiKey() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ChromaDbService chromaDbService = mock(ChromaDbService.class);
        VectorEmbeddingService service = new VectorEmbeddingService(chromaDbService, restTemplate);
        ReflectionTestUtils.setField(service, "voyageApiKey", "");

        float[] embedding = service.getEmbeddingVector("hello world");

        assertThat(embedding).isNotEmpty();
        assertThat(embedding.length).isGreaterThan(0);
    }
}
