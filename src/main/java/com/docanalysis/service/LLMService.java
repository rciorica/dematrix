package com.docanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * LLM Service using Ollama Mistral for answer generation.
 * Provides streaming completion for RAG queries with strict grounding prompts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    @Qualifier("ollamaRestTemplate")
    private final RestTemplate restTemplate;

    @Qualifier("ollamaWebClient")
    private final WebClient webClient;

    @Value("${app.ollama.base-url:http://ollama:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model:mistral}")
    private String modelName;

    @Value("${app.llm.temperature:0.7}")
    private double temperature;

    private volatile String resolvedModelName;

    /**
     * Generate streaming completion from Ollama Mistral with grounding prompt.
     * @param prompt The augmented prompt with context and question
     * @return Flux of response chunks for streaming to client
     */
    public Flux<String> streamCompletion(String prompt) {
        log.debug("Generating streaming completion with Ollama Mistral model: {}", modelName);

        try {
            return generateOllamaStreamingResponse(prompt);
        } catch (Exception e) {
            log.error("Ollama generation failed: " + e.getMessage(), e);
            return Flux.error(e);
        }
    }

    private Flux<String> generateOllamaStreamingResponse(String prompt) {
        try {
            log.info("Calling Ollama at: {}", ollamaBaseUrl);
            String modelToUse = resolveModelForUse();
            return callOllamaGenerateStream(prompt, modelToUse)
                    .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                        if (isModelNotFound(notFound.getResponseBodyAsString())) {
                            log.warn("Configured or cached Ollama model '{}' not found. Re-resolving model.", modelToUse);
                            resolvedModelName = null;
                            String fallbackModel = resolveModelForUse();
                            return callOllamaGenerateStream(prompt, fallbackModel);
                        }
                        return Flux.error(notFound);
                    })
                    .filter(response -> response.getResponse() != null && !response.getResponse().isBlank())
                    .map(OllamaStreamResponse::getResponse)
                    .switchIfEmpty(Flux.error(new RuntimeException("Ollama returned empty response")))
                    .doOnNext(chunk -> log.debug("Received Ollama stream chunk of {} characters", chunk.length()))
                    .doOnComplete(() -> log.info("Ollama streaming response completed"));
        } catch (Exception e) {
            log.error("Ollama API generation failed: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }

    private Flux<OllamaStreamResponse> callOllamaGenerateStream(String prompt, String model) {
        String ollamaUrl = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl : ollamaBaseUrl + "/";

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("temperature", temperature);
        request.put("stream", true);

        log.debug("Using Ollama model for streaming: {}", model);
        return webClient.post()
                .uri(ollamaUrl + "api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(OllamaStreamResponse.class);
    }

    private boolean isModelNotFound(String responseBody) {
        return responseBody != null
                && responseBody.contains("model")
                && responseBody.contains("not found");
    }

    private OllamaResponse callOllamaGenerate(String prompt, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("temperature", temperature);
        request.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        String ollamaUrl = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl : ollamaBaseUrl + "/";

        log.debug("Using Ollama model: {}", model);
        return restTemplate.postForObject(ollamaUrl + "api/generate", entity, OllamaResponse.class);
    }

    private String resolveModelForUse() {
        if (resolvedModelName != null && !resolvedModelName.isBlank()) {
            return resolvedModelName;
        }

        String ollamaUrl = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl : ollamaBaseUrl + "/";
        OllamaTagsResponse tagsResponse = restTemplate.getForObject(ollamaUrl + "api/tags", OllamaTagsResponse.class);

        if (tagsResponse == null || tagsResponse.getModels() == null || tagsResponse.getModels().isEmpty()) {
            throw new IllegalStateException("No Ollama models are available. Pull a model first (e.g. ollama pull " + modelName + ").");
        }

        List<String> availableModels = new ArrayList<>();
        for (OllamaModelInfo model : tagsResponse.getModels()) {
            if (model.getName() != null && !model.getName().isBlank()) {
                availableModels.add(model.getName());
            }
        }

        String configuredModel = modelName;
        if (availableModels.contains(configuredModel)) {
            resolvedModelName = configuredModel;
            return resolvedModelName;
        }

        String configuredWithLatest = configuredModel + ":latest";
        if (availableModels.contains(configuredWithLatest)) {
            resolvedModelName = configuredWithLatest;
            return resolvedModelName;
        }

        String fallback = availableModels.get(0);
        log.warn("Configured Ollama model '{}' not found. Falling back to installed model '{}'.", configuredModel, fallback);
        resolvedModelName = fallback;
        return resolvedModelName;
    }

    /**
     * Ollama API response model.
     */
    static class OllamaResponse {
        private String response;
        private String error;

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    static class OllamaStreamResponse {
        private String response;
        private boolean done;
        private String error;

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    static class OllamaTagsResponse {
        private List<OllamaModelInfo> models;

        public List<OllamaModelInfo> getModels() {
            return models;
        }

        public void setModels(List<OllamaModelInfo> models) {
            this.models = models;
        }
    }

    static class OllamaModelInfo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
