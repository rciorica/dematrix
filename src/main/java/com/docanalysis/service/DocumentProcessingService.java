package com.docanalysis.service;

import com.docanalysis.domain.Document;
import com.docanalysis.domain.DocumentChunk;
import com.docanalysis.exception.DocumentProcessingException;
import com.docanalysis.repository.DocumentRepository;
import com.docanalysis.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentProcessingService {
    
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorEmbeddingService vectorEmbeddingService;
    
    @Value("${app.chunking.chunk-size:1000}")
    private int chunkSize;
    
    @Value("${app.chunking.chunk-overlap:100}")
    private int chunkOverlap;
    
    @Value("${app.chunking.min-chunk-size:100}")
    private int minChunkSize;
    
    public Document processDocument(String fileName, String title, String filePath) {
        try {
            log.info("Starting document processing: {}", fileName);
            
            Document document = Document.builder()
                    .fileName(fileName)
                    .title(title)
                    .filePathRef(filePath)
                    .status(Document.DocumentStatus.PROCESSING)
                    .fileType(extractFileType(fileName))
                    .fileSize(getFileSize(filePath))
                    .build();
            
            document = documentRepository.save(document);
            log.info("Document entity created with ID: {}", document.getId());
            
            // Extract text based on file type
            String extractedText;
            if (fileName.toLowerCase().endsWith(".pdf")) {
                extractedText = extractTextFromPDF(filePath);
            } else if (fileName.toLowerCase().endsWith(".txt")) {
                extractedText = new String(Files.readAllBytes(Paths.get(filePath)));
            } else {
                throw new DocumentProcessingException("Unsupported file format");
            }
            
            document.setExtractedText(extractedText);
            
            // Perform smart chunking
            List<DocumentChunk> chunks = performSmartChunking(document, extractedText);
            log.info("Created {} chunks for document: {}", chunks.size(), document.getId());
            
            document.setChunks(chunks);
            document.setChunkCount(chunks.size());
            document.setStatus(Document.DocumentStatus.INDEXED);
            document = documentRepository.save(document);
            
            // Generate embeddings for all chunks
            for (DocumentChunk chunk : chunks) {
                vectorEmbeddingService.generateAndStoreEmbedding(chunk);
            }
            
            log.info("Document processing completed: {}", document.getId());
            return document;
        } catch (Exception e) {
            log.error("Error processing document: {}", fileName, e);
            throw new DocumentProcessingException("Failed to process document: " + e.getMessage(), e);
        }
    }
    
    private List<DocumentChunk> performSmartChunking(Document document, String text) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // Split by paragraphs first
        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int sequenceOrder = 0;
        int pageNumber = 1;
        int charOffset = 0;
        
        for (String paragraph : paragraphs) {
            String trimmedPara = paragraph.trim();
            
            if (trimmedPara.isEmpty()) {
                continue;
            }
            
            // Check if adding paragraph exceeds chunk size
            if ((currentChunk.length() + trimmedPara.length()) > chunkSize && currentChunk.length() > minChunkSize) {
                // Save current chunk
                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .chunkText(currentChunk.toString())
                        .sequenceOrder(sequenceOrder++)
                        .pageNumber(pageNumber)
                        .startOffset((long) charOffset)
                        .endOffset((long) (charOffset + currentChunk.length()))
                        .isTableData(detectTableData(currentChunk.toString()))
                        .build();
                
                chunks.add(documentChunkRepository.save(chunk));
                charOffset += currentChunk.length();
                currentChunk = new StringBuilder(trimmedPara);
                
                // Update page number (heuristic: ~3000 chars per page)
                if (charOffset > 3000 * pageNumber) {
                    pageNumber++;
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(trimmedPara);
            }
        }
        
        // Save final chunk
        if (currentChunk.length() > minChunkSize) {
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkText(currentChunk.toString())
                    .sequenceOrder(sequenceOrder)
                    .pageNumber(pageNumber)
                    .startOffset((long) charOffset)
                    .endOffset((long) (charOffset + currentChunk.length()))
                    .isTableData(detectTableData(currentChunk.toString()))
                    .build();
            
            chunks.add(documentChunkRepository.save(chunk));
        }
        
        return chunks;
    }
    
    private boolean detectTableData(String text) {
        // Simple heuristic: if text contains multiple pipes or tabs, likely a table
        int pipeCount = text.split("\\|", -1).length - 1;
        int tabCount = text.split("\t", -1).length - 1;
        return pipeCount > 2 || tabCount > 2;
    }
    
    private String extractTextFromPDF(String filePath) throws IOException {
        var document = Loader.loadPDF(new File(filePath));
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } finally {
            document.close();
        }
    }
    
    private String extractFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toUpperCase() : "UNKNOWN";
    }
    
    private long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("Could not determine file size: {}", filePath);
            return 0L;
        }
    }
}
