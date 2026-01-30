package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilitaire pour nettoyer ApiStructure avant envoi au LLM
 * - Supprime les champs null / vides / inutiles
 * - Réduit la taille du JSON (moins de tokens)
 * - Calcule la taille estimée
 */
@Component
@Slf4j
public class ApiStructureCleaner {

    /**
     * Nettoie l'ApiStructure pour le LLM
     */
    public ApiStructure cleanForLlm(ApiStructure original) {
        if (original == null) {
            return null;
        }

        log.debug("Nettoyage ApiStructure - {} endpoints avant", original.getEndpoints().size());

        // Nettoyer chaque endpoint
        Map<String, Endpoint> cleanedEndpoints = original.getEndpoints().entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> cleanEndpoint(e.getValue())
                ));

        // Construire une nouvelle structure nettoyée
        ApiStructure cleaned = ApiStructure.builder()
                .api_title(original.getApi_title())
                .api_version(original.getApi_version())
                .base_url(original.getBase_url())
                .authentication_type(original.getAuthentication_type())
                .endpoints(cleanedEndpoints)
                .schemas(new HashMap<>())  // On vide les schemas pour économiser (ou filtre si besoin)
                .build();

        log.info("ApiStructure nettoyée : {} → {} endpoints (réduction: {}%)",
                original.getEndpoints().size(),
                cleanedEndpoints.size(),
                original.getEndpoints().size() > 0 
                    ? (int) ((original.getEndpoints().size() - cleanedEndpoints.size()) * 100.0 / original.getEndpoints().size())
                    : 0);

        return cleaned;
    }

    /**
     * Nettoie un seul Endpoint
     */
    private Endpoint cleanEndpoint(Endpoint endpoint) {
        if (endpoint == null) return null;

        return Endpoint.builder()
                .operation_id(endpoint.getOperation_id())
                .path(endpoint.getPath())
                .method(endpoint.getMethod())
                .summary(endpoint.getSummary())
                .description(isBlank(endpoint.getDescription()) ? null : endpoint.getDescription())
                .type(endpoint.getType())
                .parameters(endpoint.getParameters().stream()
                        .filter(p -> p != null && !isBlank(p.getName()))
                        .collect(Collectors.toList()))
                .has_request_body(endpoint.isHas_request_body())
                .request_body_schema(isBlank(endpoint.getRequest_body_schema()) ? null : endpoint.getRequest_body_schema())
                .response_schema(isBlank(endpoint.getResponse_schema()) ? null : endpoint.getResponse_schema())
                .response_is_array(endpoint.isResponse_is_array())
                .build();
    }

    /**
     * Estime la taille en caractères (approximation)
     */
    public long estimateSize(ApiStructure structure) {
        if (structure == null) return 0;

        long size = 0;

        if (structure.getApi_title() != null) size += structure.getApi_title().length();
        if (structure.getApi_version() != null) size += structure.getApi_version().length();
        if (structure.getBase_url() != null) size += structure.getBase_url().length();
        if (structure.getAuthentication_type() != null) size += structure.getAuthentication_type().length();

        size += structure.getEndpoints().values().stream()
                .mapToLong(this::estimateEndpointSize)
                .sum();

        return size;
    }

    private long estimateEndpointSize(Endpoint ep) {
        if (ep == null) return 0;
        long size = 0;
        if (ep.getOperation_id() != null) size += ep.getOperation_id().length();
        if (ep.getPath() != null) size += ep.getPath().length();
        if (ep.getSummary() != null) size += ep.getSummary().length();
        if (ep.getDescription() != null) size += ep.getDescription().length();
        size += ep.getParameters().size() * 80; // estimation moyenne par paramètre
        return size;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}