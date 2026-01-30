package com.network.projet.ussd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration pour le générateur IA avec HuggingFace Inference API.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Configuration
@Slf4j
public class AiGeneratorConfig {
    
    @Value("${ai.generator.huggingface.base-url}")
    private String huggingface_base_url;
    
    @Value("${ai.generator.huggingface.api-key}")
    private String huggingface_api_key;
    
    /**
     * WebClient pour HuggingFace Inference API.
     */
    @Bean("huggingfaceWebClient")
    public WebClient huggingfaceWebClient() {
        log.info("Initializing HuggingFace WebClient with base URL: {}", huggingface_base_url);
        return WebClient.builder()
            .baseUrl(huggingface_base_url)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Authorization", "Bearer " + huggingface_api_key)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))  // 10MB buffer
            .build();
    }
}
