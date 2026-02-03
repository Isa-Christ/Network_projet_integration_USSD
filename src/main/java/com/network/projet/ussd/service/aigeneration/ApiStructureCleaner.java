package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nettoie la structure API en supprimant les données nulles et inutiles.
 * Réduit la taille du JSON envoyé au LLM pour meilleure clarté.
 * 
 * @author Network Project Team
 * @since 2025-01-28
 */
@Component
@Slf4j
public class ApiStructureCleaner {

    /**
     * Nettoie l'ApiStructure en supprimant tous les champs nulles/vides.
     * Conserve uniquement les données essentielles pour le LLM.
     */
    public ApiStructure cleanForLlm(ApiStructure apiStructure) {
        if (apiStructure == null) {
            return null;
        }

        try {
            log.debug("Cleaning ApiStructure: {} endpoints before filtering",
                    apiStructure.getEndpoints() != null ? apiStructure.getEndpoints().size() : 0);

            Map<String, Endpoint> cleanedEndpoints = new HashMap<>();

            if (apiStructure.getEndpoints() != null) {
                apiStructure.getEndpoints().forEach((path, endpoint) -> {
                    if (endpoint != null) {
                        try {
                            Endpoint cleaned = cleanEndpoint(endpoint);
                            if (cleaned != null) {
                                cleanedEndpoints.put(path, cleaned);
                            }
                        } catch (Exception e) {
                            log.warn("Skipping endpoint {} due to cleaning error", path);
                        }
                    }
                });
            }

            return ApiStructure.builder()
                    .api_title(apiStructure.getApi_title())
                    .api_version(apiStructure.getApi_version())
                    .base_url(apiStructure.getBase_url())
                    .endpoints(cleanedEndpoints)
                    .schemas(new HashMap<>())
                    .authentication_type(apiStructure.getAuthentication_type())
                    .build();
        } catch (Exception e) {
            log.error("CRITIQUE: Erreur fatale lors du nettoyage de l'API", e);
            // En cas de crash total, on retourne l'original plutôt que de planter
            return apiStructure;
        }
    }

    private Endpoint cleanEndpoint(Endpoint endpoint) {
        if (endpoint == null)
            return null;

        try {
            String opId = endpoint.getOperation_id() != null ? endpoint.getOperation_id() : "unknown";
            String summary = endpoint.getSummary() != null ? endpoint.getSummary() : opId;

            // Truncate summary if too long
            if (summary.length() > 50)
                summary = summary.substring(0, 50) + "...";

            return Endpoint.builder()
                    .operation_id(opId)
                    .path(endpoint.getPath() != null ? endpoint.getPath() : "/unknown")
                    .method(endpoint.getMethod())
                    .summary(summary)
                    .description(null) // Toujours null pour économiser des tokens
                    .type(endpoint.getType())
                    .parameters(new java.util.ArrayList<>()) // Vide pour économiser
                    .has_request_body(endpoint.isHas_request_body())
                    .request_body_schema(null)
                    .response_schema(null)
                    .response_is_array(endpoint.isResponse_is_array())
                    .build();
        } catch (Exception e) {
            log.warn("Error cleaning specific endpoint", e);
            return null;
        }
    }

    /**
     * Vérifie si une string est vide/null/whitespace.
     */
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Retourne la taille estimée en caractères du JSON.
     * Utile pour vérifier la réduction.
     */
    public long estimateSize(ApiStructure apiStructure) {
        if (apiStructure == null)
            return 0;

        // Estimation simple : sum des longueurs de strings
        long size = 0;

        if (apiStructure.getApi_title() != null)
            size += apiStructure.getApi_title().length();
        if (apiStructure.getApi_version() != null)
            size += apiStructure.getApi_version().length();
        if (apiStructure.getBase_url() != null)
            size += apiStructure.getBase_url().length();

        size += apiStructure.getEndpoints().values().stream()
                .mapToLong(ep -> {
                    long epSize = 0;
                    if (ep.getOperation_id() != null)
                        epSize += ep.getOperation_id().length();
                    if (ep.getPath() != null)
                        epSize += ep.getPath().length();
                    if (ep.getSummary() != null)
                        epSize += ep.getSummary().length();
                    if (ep.getDescription() != null)
                        epSize += ep.getDescription().length();
                    epSize += ep.getParameters().size() * 50; // Estimation par paramètre
                    return epSize;
                })
                .sum();

        return size;
    }
}
