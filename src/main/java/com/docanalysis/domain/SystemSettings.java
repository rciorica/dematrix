package com.docanalysis.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "chunk_size")
    @Builder.Default
    private Integer chunkSize = 1000;
    
    @Column(name = "chunk_overlap")
    @Builder.Default
    private Integer chunkOverlap = 100;
    
    @Column(name = "top_k_results")
    @Builder.Default
    private Integer topKResults = 5;
    
    @Column(name = "similarity_threshold")
    @Builder.Default
    private Double similarityThreshold = 0.7;
    
    @Column(name = "dark_theme")
    @Builder.Default
    private Boolean darkTheme = false;
}
