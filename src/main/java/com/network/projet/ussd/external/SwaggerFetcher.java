package com.network.projet.ussd.external;

import com.network.projet.ussd.exception.SwaggerParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service de téléchargement de fichiers Swagger depuis URL.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwaggerFetcher {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${ai.generator.swagger.timeout:10000}")
    private long swagger_timeout;
    
    @Value("${ai.generator.swagger.max-file-size-mb:5}")
    private int max_file_size_mb;
    
    /**
     * Télécharge un fichier Swagger depuis une URL.
     */
    public Mono<String> fetchSwagger(String url) {
        log.info("Fetching Swagger from URL: {}", url);
        
        if (url == null || url.isEmpty()) {
            return Mono.error(new SwaggerParseException("URL Swagger vide"));
        }
        
        return webClientBuilder.build()
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(swagger_timeout))
            .doOnSuccess(content -> {
                int size_kb = content.length() / 1024;
                log.info("Swagger fetched successfully: {} KB", size_kb);
                
                if (size_kb > (max_file_size_mb * 1024)) {
                    throw new SwaggerParseException(
                        "Fichier Swagger trop volumineux: " + size_kb + " KB (max: " + (max_file_size_mb * 1024) + " KB)"
                    );
                }
            })
            .onErrorMap(error -> {
                if (error instanceof SwaggerParseException) {
                    return error;
                }
                log.error("Erreur téléchargement Swagger: {}", error.getMessage());
                return new SwaggerParseException("Impossible de télécharger le Swagger: " + error.getMessage(), error);
            });
    }
}