package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
//import java.util.List;
import java.util.Map;

/**
 * Structure complète d'une API analysée.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiStructure {
    private String api_title;
    private String api_version;
    private String base_url;
    
    @Builder.Default
    private Map<String, Endpoint> endpoints = new HashMap<>();
    
    @Builder.Default
    private Map<String, Object> schemas = new HashMap<>();
    
    private String authentication_type;  // none, bearer, apiKey
    
    /**
     * Compte le nombre total d'endpoints.
     */
    public int getTotalEndpoints() {
        return endpoints.size();
    }
}