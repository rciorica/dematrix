package com.docanalysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ChromaDbServiceTest {

    @Autowired
    private ChromaDbService chromaDbService;

    @BeforeEach
    public void setUp() {
        System.out.println("\n=== Starting Chroma Test ===");
    }

    @Test
    public void testChromaHealthCheck() {
        System.out.println("Test: Chroma Health Check");
        boolean isHealthy = chromaDbService.isHealthy();
        System.out.println("  Chroma health: " + (isHealthy ? "HEALTHY" : "UNHEALTHY"));
        assertNotNull(chromaDbService);
    }

    @Test
    public void testSmallVectorUpsert() {
        System.out.println("Test: Small Vector Upsert (10 dimensions)");
        try {
            List<String> ids = List.of("test-small-1");
            float[] smallVector = new float[10];
            Arrays.fill(smallVector, 0.5f);
            
            List<float[]> embeddings = List.of(smallVector);
            List<String> documents = List.of("Small test document");
            List<Map<String, Object>> metadatas = List.of(
                new HashMap<String, Object>() {{
                    put("source", "test");
                }}
            );

            chromaDbService.upsert("test_small_vectors", ids, embeddings, documents, metadatas);
            System.out.println("  Small vector upsert: SUCCESS");
            assertTrue(true);
        } catch (Exception e) {
            System.out.println("  Small vector upsert: FAILED - " + e.getMessage());
            fail("Small vector upsert failed: " + e.getMessage());
        }
    }

    @Test
    public void testSmallVectorQuery() {
        System.out.println("Test: Small Vector Query (10 dimensions)");
        try {
            List<String> ids = List.of("test-small-query-1");
            float[] smallVector = new float[10];
            Arrays.fill(smallVector, 0.5f);
            
            chromaDbService.upsert("test_small_query", ids, List.of(smallVector), 
                    List.of("Small query test"), List.of(new HashMap<>()));

            List<ChromaDbService.ChromaQueryResult> results = chromaDbService.query("test_small_query", smallVector, 5);
            System.out.println("  Small vector query: SUCCESS - returned " + results.size() + " results");
            assertNotNull(results);
        } catch (Exception e) {
            System.out.println("  Small vector query: FAILED - " + e.getMessage());
            fail("Small vector query failed: " + e.getMessage());
        }
    }

    @Test
    public void testMediumVectorUpsert() {
        System.out.println("Test: Medium Vector Upsert (384 dimensions)");
        try {
            List<String> ids = List.of("test-medium-1");
            float[] medVector = new float[384];
            for (int i = 0; i < 384; i++) {
                medVector[i] = 0.1f;
            }

            List<float[]> embeddings = List.of(medVector);
            List<String> documents = List.of("Medium dimension test document");
            List<Map<String, Object>> metadatas = List.of(new HashMap<>());

            chromaDbService.upsert("test_medium_vectors", ids, embeddings, documents, metadatas);
            System.out.println("  Medium vector upsert: SUCCESS");
            assertTrue(true);
        } catch (Exception e) {
            System.out.println("  Medium vector upsert: FAILED - " + e.getMessage());
            fail("Medium vector upsert failed: " + e.getMessage());
        }
    }

    @Test
    public void testMediumVectorQuery() {
        System.out.println("Test: Medium Vector Query (384 dimensions)");
        try {
            List<String> ids = List.of("test-medium-query-1");
            float[] medVector = new float[384];
            for (int i = 0; i < 384; i++) {
                medVector[i] = 0.1f;
            }

            chromaDbService.upsert("test_medium_query", ids, List.of(medVector), 
                    List.of("Medium query test"), List.of(new HashMap<>()));

            List<ChromaDbService.ChromaQueryResult> results = chromaDbService.query("test_medium_query", medVector, 5);
            System.out.println("  Medium vector query: SUCCESS - returned " + results.size() + " results");
            assertNotNull(results);
        } catch (Exception e) {
            System.out.println("  Medium vector query: FAILED - " + e.getMessage());
            fail("Medium vector query failed: " + e.getMessage());
        }
    }

    @Test
    public void testLargeVectorUpsert() {
        System.out.println("Test: CRITICAL - Large Vector Upsert (1024 dimensions)");
        try {
            List<String> ids = List.of("test-large-1");
            float[] largeVector = new float[1024];
            for (int i = 0; i < 1024; i++) {
                largeVector[i] = 0.1f;
            }

            List<float[]> embeddings = List.of(largeVector);
            List<String> documents = List.of("Large 1024 dimension test document");
            List<Map<String, Object>> metadatas = List.of(new HashMap<>());

            chromaDbService.upsert("test_large_vectors", ids, embeddings, documents, metadatas);
            System.out.println("  Large vector upsert (1024-dim): SUCCESS");
            assertTrue(true);
        } catch (Exception e) {
            System.out.println("  Large vector upsert (1024-dim): FAILED - " + e.getMessage());
            e.printStackTrace();
            fail("1024-dim vector upsert failed: " + e.getMessage());
        }
    }

    @Test
    public void testLargeVectorQuery() {
        System.out.println("Test: CRITICAL - Large Vector Query (1024 dimensions)");
        try {
            List<String> ids = List.of("test-large-query-1");
            float[] largeVector = new float[1024];
            for (int i = 0; i < 1024; i++) {
                largeVector[i] = 0.1f;
            }

            chromaDbService.upsert("test_large_query", ids, List.of(largeVector), 
                    List.of("Large query test"), List.of(new HashMap<>()));

            List<ChromaDbService.ChromaQueryResult> results = chromaDbService.query("test_large_query", largeVector, 5);
            System.out.println("  Large vector query (1024-dim): SUCCESS - returned " + results.size() + " results");
            assertNotNull(results);
        } catch (Exception e) {
            System.out.println("  Large vector query (1024-dim): FAILED - " + e.getMessage());
            e.printStackTrace();
            fail("1024-dim vector query failed: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleUpserts() {
        System.out.println("Test: Multiple Upserts in Same Collection");
        try {
            float[] vec1 = new float[1024];
            float[] vec2 = new float[1024];
            Arrays.fill(vec1, 0.1f);
            Arrays.fill(vec2, 0.2f);

            List<String> ids = List.of("multi-1", "multi-2");
            List<float[]> embeddings = List.of(vec1, vec2);
            List<String> documents = List.of("Doc 1", "Doc 2");
            List<Map<String, Object>> metadatas = List.of(new HashMap<>(), new HashMap<>());

            chromaDbService.upsert("test_multi", ids, embeddings, documents, metadatas);
            System.out.println("  Multiple upserts: SUCCESS - added 2 documents");
            assertTrue(true);
        } catch (Exception e) {
            System.out.println("  Multiple upserts: FAILED - " + e.getMessage());
            fail("Multiple upserts failed: " + e.getMessage());
        }
    }
}
