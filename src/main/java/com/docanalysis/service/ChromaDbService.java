package com.docanalysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChromaDbService {

    private final RestTemplate restTemplate;

    public ChromaDbService(@Qualifier("chromaRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${app.chroma.url:http://chroma:8000}")
    private String chromaUrl;

    @Value("${app.chroma.max-retries:3}")
    private int maxRetries;

    @Value("${app.chroma.tenant:default_tenant}")
    private String chromaTenant;

    @Value("${app.chroma.database:default_database}")
    private String chromaDatabase;

    private final Map<String, String> collectionIdCache = new ConcurrentHashMap<>();

    /**
     * Query Chroma for semantically similar vectors with retry logic.
     * Chroma creates collections automatically on first upsert.
     * @param collectionName Name of the collection in Chroma
     * @param queryVector The embedding vector to search for
     * @param topK Number of results to return
     * @return List of matched documents with distances
     */
    public List<ChromaQueryResult> query(String collectionName, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) {
            log.warn("Invalid query vector: null or empty");
            return Collections.emptyList();
        }

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                log.debug("Chroma query attempt {}/{} for collection: {} with {} dimensions", 
                        attempt + 1, maxRetries, collectionName, queryVector.length);
                
                return executeQuery(collectionName, queryVector, topK);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                log.warn("Chroma query attempt {}/{} failed for collection {}: {}", 
                        attempt, maxRetries, collectionName, e.getMessage());
                
                if (attempt < maxRetries) {
                    long waitTime = 1000L * attempt;
                    try {
                        log.debug("Waiting {}ms before retry...", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted");
                        break;
                    }
                }
            }
        }

        log.error("All Chroma query attempts failed for collection: {}", collectionName, lastException);
        return Collections.emptyList();
    }

    private List<ChromaQueryResult> executeQuery(String collectionName, float[] queryVector, int topK) {
        String collectionId = resolveCollectionId(collectionName);
        String url = buildV2CollectionsBaseUrl() + "/" + collectionId + "/query";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new LinkedHashMap<>();
        
        List<Double> queryList = new ArrayList<>();
        for (float v : queryVector) {
            queryList.add((double) v);
        }
        
        request.put("query_embeddings", List.of(queryList));
        request.put("n_results", topK);
        request.put("include", List.of("embeddings", "documents", "metadatas", "distances"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        log.debug("Sending query to Chroma at: {}", url);
        ChromaQueryResponse response = restTemplate.postForObject(url, entity, ChromaQueryResponse.class);

        if (response == null || response.getIds() == null || response.getIds().isEmpty()) {
            log.debug("Chroma returned null or empty response for collection {}", collectionName);
            return Collections.emptyList();
        }

        List<ChromaQueryResult> results = new ArrayList<>();
        List<List<String>> ids = response.getIds();
        List<List<String>> documents = response.getDocuments();
        List<List<Map<String, Object>>> metadatas = response.getMetadatas();
        List<List<Double>> distances = response.getDistances();

        if (!ids.isEmpty() && !ids.get(0).isEmpty()) {
            for (int i = 0; i < ids.get(0).size(); i++) {
                ChromaQueryResult result = new ChromaQueryResult();
                result.setId(ids.get(0).get(i));
                if (documents != null && !documents.isEmpty() && documents.get(0).size() > i) {
                    result.setDocument(documents.get(0).get(i));
                }
                if (metadatas != null && !metadatas.isEmpty() && metadatas.get(0).size() > i) {
                    result.setMetadata(metadatas.get(0).get(i));
                }
                if (distances != null && !distances.isEmpty() && distances.get(0).size() > i) {
                    result.setDistance(distances.get(0).get(i));
                }
                results.add(result);
            }
        }

        log.debug("Chroma query returned {} results for collection {}", results.size(), collectionName);
        return results;
    }

    /**
     * Add or update embeddings in Chroma with retry logic.
     * Collection is created automatically on first upsert.
     * @param collectionName Name of the collection
     * @param ids List of document IDs
     * @param embeddings List of embedding vectors
     * @param documents List of document texts
     * @param metadatas List of metadata maps
     */
    public void upsert(String collectionName, List<String> ids, List<float[]> embeddings,
                       List<String> documents, List<Map<String, Object>> metadatas) {
        
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                executeUpsert(collectionName, ids, embeddings, documents, metadatas);
                return;
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                log.warn("Chroma upsert attempt {}/{} failed for collection {}: {}", 
                        attempt, maxRetries, collectionName, e.getMessage());
                
                if (attempt < maxRetries) {
                    long waitTime = 1000L * attempt;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All Chroma upsert attempts failed for collection: {}", collectionName, lastException);
    }

    private void executeUpsert(String collectionName, List<String> ids, List<float[]> embeddings,
                              List<String> documents, List<Map<String, Object>> metadatas) {
        String collectionId = resolveCollectionId(collectionName);
        String url = buildV2CollectionsBaseUrl() + "/" + collectionId + "/upsert";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("ids", ids);

        List<List<Double>> embeddingsList = new ArrayList<>();
        for (float[] embedding : embeddings) {
            List<Double> list = new ArrayList<>();
            for (float v : embedding) {
                list.add((double) v);
            }
            embeddingsList.add(list);
        }
        request.put("embeddings", embeddingsList);
        request.put("documents", documents);
        request.put("metadatas", metadatas);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        restTemplate.postForObject(url, entity, Object.class);

        log.debug("Upserted {} embeddings into Chroma collection: {}", ids.size(), collectionName);
    }

    /**
     * Health check for Chroma service.
     * @return true if Chroma is healthy
     */
    public boolean isHealthy() {
        try {
            String url = chromaUrl.endsWith("/")
                    ? chromaUrl + "api/v2/heartbeat"
                    : chromaUrl + "/api/v2/heartbeat";
            
            String response = restTemplate.getForObject(url, String.class);
            log.debug("Chroma heartbeat successful: {}", response);
            return true;
        } catch (Exception e) {
            log.warn("Chroma health check failed: {}", e.getMessage());
            return false;
        }
    }

    private String resolveCollectionId(String collectionName) {
        return collectionIdCache.computeIfAbsent(collectionName, this::getOrCreateCollectionId);
    }

    private String getOrCreateCollectionId(String collectionName) {
        String url = buildV2CollectionsBaseUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", collectionName);
        request.put("get_or_create", true);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ChromaCollectionResponse response = restTemplate.postForObject(url, entity, ChromaCollectionResponse.class);

        if (response == null || response.getId() == null || response.getId().isBlank()) {
            throw new IllegalStateException("Failed to resolve Chroma collection id for: " + collectionName);
        }

        return response.getId();
    }

    private String buildV2CollectionsBaseUrl() {
        String baseUrl = chromaUrl.endsWith("/") ? chromaUrl.substring(0, chromaUrl.length() - 1) : chromaUrl;
        return baseUrl + "/api/v2/tenants/" + chromaTenant + "/databases/" + chromaDatabase + "/collections";
    }

    public static class ChromaCollectionResponse {
        private String id;
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class ChromaQueryResponse {
        private List<List<String>> ids;
        private List<List<String>> documents;
        private List<List<Map<String, Object>>> metadatas;
        private List<List<Double>> distances;

        public List<List<String>> getIds() { return ids; }
        public void setIds(List<List<String>> ids) { this.ids = ids; }

        public List<List<String>> getDocuments() { return documents; }
        public void setDocuments(List<List<String>> documents) { this.documents = documents; }

        public List<List<Map<String, Object>>> getMetadatas() { return metadatas; }
        public void setMetadatas(List<List<Map<String, Object>>> metadatas) { this.metadatas = metadatas; }

        public List<List<Double>> getDistances() { return distances; }
        public void setDistances(List<List<Double>> distances) { this.distances = distances; }
    }

    public static class ChromaQueryResult {
        private String id;
        private String document;
        private Map<String, Object> metadata;
        private Double distance;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDocument() { return document; }
        public void setDocument(String document) { this.document = document; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
    }
}
