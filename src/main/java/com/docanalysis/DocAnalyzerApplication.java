package com.docanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocAnalyzerApplication.class, args);
    }
}