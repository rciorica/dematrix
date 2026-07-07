package com.docanalysis.controller;

import com.docanalysis.domain.SystemSettings;
import com.docanalysis.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class SettingsController {
    
    private final SystemSettingsRepository settingsRepository;
    
    @GetMapping
    public ResponseEntity<?> getSettings() {
        try {
            var allSettings = settingsRepository.findAll();
            SystemSettings settings;
            
            if (allSettings.isEmpty()) {
                settings = SystemSettings.builder().build();
                settings = settingsRepository.save(settings);
            } else {
                settings = allSettings.get(0);
            }
            
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error fetching settings", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> saveSettings(@RequestBody SystemSettings settings) {
        try {
            log.info("Saving settings: chunkSize={}, chunkOverlap={}, topKResults={}, threshold={}",
                    settings.getChunkSize(), settings.getChunkOverlap(), 
                    settings.getTopKResults(), settings.getSimilarityThreshold());
            
            var allSettings = settingsRepository.findAll();
            if (!allSettings.isEmpty()) {
                settings.setId(allSettings.get(0).getId());
            }
            
            settings = settingsRepository.save(settings);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error saving settings", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
