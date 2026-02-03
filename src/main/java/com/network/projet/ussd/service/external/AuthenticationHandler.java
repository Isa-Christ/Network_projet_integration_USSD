package com.network.projet.ussd.service.external;

import com.network.projet.ussd.domain.enums.AuthenticationType;
import com.network.projet.ussd.domain.model.automaton.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthenticationHandler {
    
    /**
     * Construit les headers d'authentification à partir de l'objet Authentication
     * 
     * @param authentication Configuration d'authentification
     * @param context Variables de contexte pour résoudre les placeholders
     * @return Map des headers d'authentification
     */
    public Map<String, String> buildAuthHeaders(
            Authentication authentication, 
            Map<String, Object> context) {
        
        Map<String, String> authHeaders = new HashMap<>();
        
        if (authentication == null || authentication.getType() == AuthenticationType.NONE) {
            return authHeaders;
        }
        
        Map<String, String> credentials = authentication.getCredentials();
        if (credentials == null || credentials.isEmpty()) {
            return authHeaders;
        }
        
        return switch (authentication.getType()) {
            case NONE -> authHeaders;
            case BASIC -> buildBasicAuthHeaders(credentials);
            case BEARER -> buildBearerAuthHeaders(credentials);
            case API_KEY -> buildApiKeyAuthHeaders(credentials);
        };
    }
    
    /**
     * Construit les headers pour authentification BASIC
     * Format credentials: {"username": "user", "password": "pass"}
     */
    private Map<String, String> buildBasicAuthHeaders(Map<String, String> credentials) {
        Map<String, String> headers = new HashMap<>();
        
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        if (username != null && password != null) {
            String combined = username + ":" + password;
            String encodedCredentials = Base64.getEncoder()
                    .encodeToString(combined.getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + encodedCredentials);
        }
        
        return headers;
    }
    
    /**
     * Construit les headers pour authentification BEARER
     * Format credentials: {"token": "your-jwt-token"}
     */
    private Map<String, String> buildBearerAuthHeaders(Map<String, String> credentials) {
        Map<String, String> headers = new HashMap<>();
        
        String token = credentials.get("token");
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        
        return headers;
    }
    
    /**
     * Construit les headers pour authentification API_KEY
     * Format credentials: {"paramName": "appid", "apiKey": "abc123"}
     * ou {"headerName": "X-API-Key", "apiKey": "abc123"}
     */
    private Map<String, String> buildApiKeyAuthHeaders(Map<String, String> credentials) {
        Map<String, String> headers = new HashMap<>();
        
        String headerName = credentials.get("headerName");
        String apiKey = credentials.get("apiKey");
        
        // Si headerName est défini, utiliser celui-là, sinon défaut "X-API-Key"
        if (apiKey != null && headerName != null) {
            headers.put(headerName, apiKey);
        }
        
        // Note: Si c'est un query param (paramName), c'est géré par extractQueryParams()
        
        return headers;
    }
    
    /**
     * Extrait les query params pour l'authentification API_KEY
     * Utilisé quand l'API key doit être dans l'URL (ex: ?appid=abc123)
     */
    public Map<String, String> extractQueryParams(Authentication authentication) {
        if (authentication == null || authentication.getType() != AuthenticationType.API_KEY) {
            return null;
        }
        
        Map<String, String> credentials = authentication.getCredentials();
        if (credentials == null) {
            return null;
        }
        
        String paramName = credentials.get("paramName");
        String apiKey = credentials.get("apiKey");
        
        if (paramName != null && apiKey != null) {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put(paramName, apiKey);
            return queryParams;
        }
        
        return null;
    }
}