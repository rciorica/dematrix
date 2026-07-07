package com.docanalysis.repository;

import com.docanalysis.domain.EmbeddingVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmbeddingVectorRepository extends JpaRepository<EmbeddingVector, String> {
    
    Optional<EmbeddingVector> findByDocumentChunkId(String chunkId);
    
    List<EmbeddingVector> findByEmbeddingModel(String model);
    
    void deleteByDocumentChunkId(String chunkId);
    
    Long countByEmbeddingModel(String model);
}
