package com.docanalysis.repository;

import com.docanalysis.domain.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DocumentChunk entities.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
    
    List<DocumentChunk> findByDocumentIdOrderBySequenceOrderAsc(String documentId);
    
    List<DocumentChunk> findByDocumentId(String documentId);
    
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId AND dc.pageNumber = :pageNumber ORDER BY dc.sequenceOrder ASC")
    List<DocumentChunk> findChunksByDocumentAndPage(@Param("documentId") String documentId, @Param("pageNumber") Integer pageNumber);
    
    List<DocumentChunk> findByIsTableData(Boolean isTableData);
    
    Long countByDocumentId(String documentId);
    
    /**
     * Find a chunk by document ID and sequence order for neighbor chunk expansion.
     * @param documentId The document ID
     * @param sequenceOrder The sequence order (position) of the chunk
     * @return Optional containing the chunk if found
     */
    Optional<DocumentChunk> findByDocumentIdAndSequenceOrder(String documentId, Integer sequenceOrder);
}
