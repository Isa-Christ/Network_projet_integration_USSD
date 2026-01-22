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
     * Utilisé par ApiInvoker ligne 193
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
        
        // Résoudre les credentials avec le contexte si nécessaire
        String resolvedCredentials = resolveCredentials(authentication.getCredentials(), context);
        
        return switch (authentication.getType()) {
            case NONE -> authHeaders;
            case BASIC -> buildBasicAuthHeaders(resolvedCredentials);
            case BEARER -> buildBearerAuthHeaders(resolvedCredentials);
            case API_KEY -> buildApiKeyAuthHeaders(resolvedCredentials);
        };
    }
    
    /**
     * Applique l'authentification au WebClient selon le type spécifié
     */
    public WebClient.RequestHeadersSpec<?> applyAuthentication(
            WebClient.RequestHeadersSpec<?> request,
            AuthenticationType authenticationType,
            String credentials) {
        
        return switch (authenticationType) {
            case NONE -> request;
            case BASIC -> applyBasicAuth(request, credentials);
            case BEARER -> applyBearerAuth(request, credentials);
            case API_KEY -> applyApiKeyAuth(request, credentials);
        };
    }
    
    /**
     * Construit les headers pour authentification BASIC
     */
    private Map<String, String> buildBasicAuthHeaders(String credentials) {
        Map<String, String> headers = new HashMap<>();
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization", "Basic " + encodedCredentials);
        return headers;
    }
    
    /**
     * Construit les headers pour authentification BEARER
     */
    private Map<String, String> buildBearerAuthHeaders(String credentials) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + credentials);
        return headers;
    }
    
    /**
     * Construit les headers pour authentification API_KEY
     */
    private Map<String, String> buildApiKeyAuthHeaders(String credentials) {
        Map<String, String> headers = new HashMap<>();
        String headerName = "X-API-Key";
        String apiKeyValue = credentials;
        
        // Si format "headerName:apiKeyValue"
        if (credentials.contains(":")) {
            String[] parts = credentials.split(":", 2);
            headerName = parts[0];
            apiKeyValue = parts[1];
        }
        
        headers.put(headerName, apiKeyValue);
        return headers;
    }
    
    /**
     * Authentification BASIC (username:password encodé en Base64)
     * Format credentials: "username:password"
     */
    private WebClient.RequestHeadersSpec<?> applyBasicAuth(
            WebClient.RequestHeadersSpec<?> request, String credentials) {
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return request.header("Authorization", "Basic " + encodedCredentials);
    }
    
    /**
     * Authentification BEARER (token JWT ou OAuth)
     * Format credentials: "your-token-here"
     */
    private WebClient.RequestHeadersSpec<?> applyBearerAuth(
            WebClient.RequestHeadersSpec<?> request, String credentials) {
        return request.header("Authorization", "Bearer " + credentials);
    }
    
    /**
     * Authentification API_KEY
     * Format credentials: "headerName:apiKeyValue" ou juste "apiKeyValue" (défaut: X-API-Key)
     */
    private WebClient.RequestHeadersSpec<?> applyApiKeyAuth(
            WebClient.RequestHeadersSpec<?> request, String credentials) {
        String headerName = "X-API-Key";
        String apiKeyValue = credentials;
        
        // Si format "headerName:apiKeyValue"
        if (credentials.contains(":")) {
            String[] parts = credentials.split(":", 2);
            headerName = parts[0];
            apiKeyValue = parts[1];
        }
        
        return request.header(headerName, apiKeyValue);
    }
    
    /**
     * Résout les placeholders dans les credentials
     * Exemple: "{{context.apiKey}}" -> valeur réelle depuis le contexte
     */
    private String resolveCredentials(String credentials, Map<String, Object> context) {
        if (credentials == null || !credentials.contains("{{")) {
            return credentials;
        }
        
        String resolved = credentials;
        
        // Remplacer les placeholders {{key}}
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (resolved.contains(placeholder)) {
                resolved = resolved.replace(placeholder, 
                        entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }
        
        return resolved;
    }
}