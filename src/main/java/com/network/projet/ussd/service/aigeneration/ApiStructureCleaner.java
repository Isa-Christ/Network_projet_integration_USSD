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
        
        log.debug("Cleaning ApiStructure: {} endpoints before filtering", 
            apiStructure.getEndpoints().size());
        
        // Nettoyer les endpoints
        Map<String, Endpoint> cleanedEndpoints = apiStructure.getEndpoints()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> cleanEndpoint(entry.getValue())
            ));
        
        // Créer une copie nettoyée
        ApiStructure cleaned = ApiStructure.builder()
            .api_title(apiStructure.getApi_title())
            .api_version(apiStructure.getApi_version())
            .base_url(apiStructure.getBase_url())
            .endpoints(cleanedEndpoints)
            .schemas(new HashMap<>())  // Optionnel: vider ou filtrer
            .authentication_type(apiStructure.getAuthentication_type())
            .build();
        
        log.info("Cleaned ApiStructure: {} endpoints after filtering (removed {} null/empty)",
            cleanedEndpoints.size(),
            apiStructure.getEndpoints().size() - cleanedEndpoints.size());
        
        return cleaned;
    }
    
    /**
     * Nettoie un endpoint en supprimant les champs nulles/vides/inutiles.
     */
    private Endpoint cleanEndpoint(Endpoint endpoint) {
        if (endpoint == null) {
            return null;
        }
        
        return Endpoint.builder()
            .operation_id(endpoint.getOperation_id())
            .path(endpoint.getPath())
            .method(endpoint.getMethod())
            .summary(endpoint.getSummary())
            // description: garder seulement si pertinent
            .description(isEmpty(endpoint.getDescription()) ? null : endpoint.getDescription())
            .type(endpoint.getType())
            // parameters: garder seulement ceux avec valeurs significatives
            .parameters(!endpoint.getParameters().isEmpty() ? 
                endpoint.getParameters().stream()
                    .filter(p -> !isEmpty(p.getName()))
                    .collect(Collectors.toList()) 
                : new java.util.ArrayList<>())
            .has_request_body(endpoint.isHas_request_body())
            // request_body_schema: garder seulement si significatif
            .request_body_schema(isEmpty(endpoint.getRequest_body_schema()) ? null : endpoint.getRequest_body_schema())
            // response_schema: garder seulement si significatif
            .response_schema(isEmpty(endpoint.getResponse_schema()) ? null : endpoint.getResponse_schema())
            .response_is_array(endpoint.isResponse_is_array())
            .build();
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
        if (apiStructure == null) return 0;
        
        // Estimation simple : sum des longueurs de strings
        long size = 0;
        
        if (apiStructure.getApi_title() != null) size += apiStructure.getApi_title().length();
        if (apiStructure.getApi_version() != null) size += apiStructure.getApi_version().length();
        if (apiStructure.getBase_url() != null) size += apiStructure.getBase_url().length();
        
        size += apiStructure.getEndpoints().values().stream()
            .mapToLong(ep -> {
                long epSize = 0;
                if (ep.getOperation_id() != null) epSize += ep.getOperation_id().length();
                if (ep.getPath() != null) epSize += ep.getPath().length();
                if (ep.getSummary() != null) epSize += ep.getSummary().length();
                if (ep.getDescription() != null) epSize += ep.getDescription().length();
                epSize += ep.getParameters().size() * 50; // Estimation par paramètre
                return epSize;
            })
            .sum();
        
        return size;
    }
}
