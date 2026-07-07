package com.docanalysis.repository;

import com.docanalysis.domain.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
    
    List<DocumentChunk> findByDocumentIdOrderBySequenceOrderAsc(String documentId);
    
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId AND dc.pageNumber = :pageNumber ORDER BY dc.sequenceOrder ASC")
    List<DocumentChunk> findChunksByDocumentAndPage(@Param("documentId") String documentId, @Param("pageNumber") Integer pageNumber);
    
    List<DocumentChunk> findByIsTableData(Boolean isTableData);
    
    Long countByDocumentId(String documentId);
}
