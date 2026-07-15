package com.docanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {
    "com.docanalysis",
    "com.docanalysis.controller",
    "com.docanalysis.service",
    "com.docanalysis.config"
})
public class DocAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocAnalyzerApplication.class, args);
    }
}
