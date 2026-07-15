package com.docanalysis.controller;

import com.docanalysis.domain.Document;
import com.docanalysis.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    public void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    public void testUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("title", "Test Document"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName", equalTo("test-document.pdf")))
                .andExpect(jsonPath("$.title", equalTo("Test Document")))
                .andExpect(jsonPath("$.status", equalTo("UPLOADED")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();
        assertNotNull(result.getResponse().getContentAsString());
        // Verify document persisted in DB
        List<Document> docs = documentRepository.findAll();
        assertEquals(1, docs.size());
        assertEquals("test-document.pdf", docs.get(0).getFileName());
        assertEquals("Test Document", docs.get(0).getTitle());
        assertEquals("UPLOADED", docs.get(0).getStatus());
    }

    @Test
    public void testUploadDocumentWithoutTitle() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "another-file.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName", equalTo("another-file.pdf")))
                .andExpect(jsonPath("$.title", equalTo("another-file.pdf")))
                .andExpect(jsonPath("$.status", equalTo("UPLOADED")));
    }

    @Test
    public void testListDocuments() throws Exception {
        // Create test documents
        Document doc1 = Document.builder()
                .fileName("doc1.pdf")
                .title("Document 1")
                .filePathRef("/uploads/doc1.pdf")
                .status("UPLOADED")
                .fileSize(1000L)
                .fileType("pdf")
                .build();
        documentRepository.save(doc1);

        Document doc2 = Document.builder()
                .fileName("doc2.pdf")
                .title("Document 2")
                .filePathRef("/uploads/doc2.pdf")
                .status("UPLOADED")
                .fileSize(2000L)
                .fileType("pdf")
                .build();
        documentRepository.save(doc2);

        mockMvc.perform(get("/api/documents/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(2)))
                .andExpect(jsonPath("$.documents[0].fileName", equalTo("doc1.pdf")))
                .andExpect(jsonPath("$.documents[0].title", equalTo("Document 1")))
                .andExpect(jsonPath("$.documents[0].status", equalTo("UPLOADED")))
                .andExpect(jsonPath("$.documents[1].fileName", equalTo("doc2.pdf")))
                .andExpect(jsonPath("$.totalElements", equalTo(2)))
                .andExpect(jsonPath("$.hasMore", equalTo(false)));
    }

    @Test
    public void testListDocumentsEmpty() throws Exception {
        mockMvc.perform(get("/api/documents/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", equalTo(0)));
    }

    @Test
    public void testUploadEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("File is empty")));
    }

    @Test
    public void testDocumentPersistenceAcrossRequests() throws Exception {
        // Upload first document
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "file1.pdf",
                "application/pdf",
                "content1".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file1)
                .param("title", "First"))
                .andExpect(status().isOk());

        // Upload second document
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "file2.pdf",
                "application/pdf",
                "content2".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file2)
                .param("title", "Second"))
                .andExpect(status().isOk());

        // List should show both
        mockMvc.perform(get("/api/documents/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", equalTo(2)));

        // Verify DB has both
        assertEquals(2, documentRepository.count());
    }
}
