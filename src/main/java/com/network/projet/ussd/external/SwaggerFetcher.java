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

    @Value("${ai.generator.swagger.timeout:120000}") // 120 secondes par défaut
    private long swagger_timeout;

    @Value("${ai.generator.swagger.max-file-size-mb:5}")
    private int max_file_size_mb;

    /**
     * Télécharge un fichier Swagger depuis une URL.
     */
    public Mono<String> fetchSwagger(String url) {
        log.info("Fetching Swagger from URL: {} with timeout 120s (HARDCODED)", url);

        if (url == null || url.isEmpty()) {
            return Mono.error(new SwaggerParseException("URL Swagger vide"));
        }

        // AUTO-CORRECTION: Handle 'swagger-ui' HTML URL inputs
        // Automatically attempt to find the standard SpringDoc JSON endpoint
        // (/v3/api-docs)
        String finalUrl = url;
        if (url.contains("swagger-ui") || url.contains("html")) {
            log.warn("⚠️ Detected Swagger UI (HTML) URL. Attempting auto-correction to JSON endpoint...");

            String baseUrl = url;
            // Strategy: Strip everything after 'swagger-ui' and append '/v3/api-docs'
            // Example: https://host/context/swagger-ui/index.html ->
            // https://host/context/v3/api-docs
            if (url.contains("/swagger-ui")) {
                baseUrl = url.substring(0, url.indexOf("/swagger-ui"));
            } else if (url.contains("swagger-ui")) {
                baseUrl = url.substring(0, url.indexOf("swagger-ui"));
            }

            // Remove trailing slash if present
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // Default SpringDoc path
            finalUrl = baseUrl + "/v3/api-docs";
            log.info("✅ Auto-corrected URL: {} -> {}", url, finalUrl);
        }

        final String requestUrl = finalUrl; // effectively final for lambda

        return webClientBuilder.build()
                .get()
                .uri(requestUrl)
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(swagger_timeout))
                .doOnSuccess(content -> {
                    int size_kb = content.length() / 1024;
                    log.info("Swagger fetched successfully from {}: {} KB", requestUrl, size_kb);

                    // Basic check: did we get HTML instead of JSON?
                    if (content.trim().toLowerCase().startsWith("<!doctype html")
                            || content.trim().toLowerCase().startsWith("<html")) {
                        throw new SwaggerParseException(
                                "Le lien pointe vers une page HTML et non un JSON Swagger valide. URL testée: "
                                        + requestUrl);
                    }

                    if (size_kb > (max_file_size_mb * 1024)) {
                        throw new SwaggerParseException(
                                "Fichier Swagger trop volumineux: " + size_kb + " KB (max: " + (max_file_size_mb * 1024)
                                        + " KB)");
                    }
                })
                .onErrorMap(error -> {
                    if (error instanceof SwaggerParseException) {
                        return error;
                    }
                    log.error("Erreur téléchargement Swagger: {}", error.getMessage());
                    return new SwaggerParseException(
                            "Impossible de télécharger le Swagger depuis " + requestUrl + " : " + error.getMessage(),
                            error);
                });
    }
}