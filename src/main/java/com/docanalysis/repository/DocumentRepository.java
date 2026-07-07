package com.docanalysis.repository;

import com.docanalysis.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    
    List<Document> findByOwnerId(String ownerId);
    
    List<Document> findByStatus(Document.DocumentStatus status);
    
    @Query("SELECT d FROM Document d WHERE d.status = 'INDEXED' ORDER BY d.uploadedAt DESC")
    List<Document> findIndexedDocuments();
    
    Optional<Document> findByIdAndOwnerId(String id, String ownerId);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = :status")
    Long countByStatus(@Param("status") Document.DocumentStatus status);
}
