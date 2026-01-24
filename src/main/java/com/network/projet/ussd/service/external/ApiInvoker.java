package com.network.projet.ussd.service.external;

import com.network.projet.ussd.domain.enums.ApiResponseStatus;
import com.network.projet.ussd.domain.enums.HttpMethod;
import com.network.projet.ussd.domain.model.automaton.Action;
import com.network.projet.ussd.domain.model.automaton.ApiConfig;
import com.network.projet.ussd.dto.ExternalApiResponse;
import com.network.projet.ussd.util.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * ApiInvoker - Service d'invocation des API externes
 * 
 * Rôle: Gestion des appels HTTP vers les API externes avec support complet
 * Appelle: WebClient, AuthenticationHandler, TemplateEngine
 * 
 * Responsabilités:
 * - Exécution des requêtes HTTP (GET, POST, PUT, DELETE, PATCH)
 * - Gestion de l'authentification via AuthenticationHandler
 * - Rendu des templates dans les URLs, headers et body
 * - Gestion des timeouts et retry
 * - Mapping des erreurs HTTP vers des réponses structurées
 * 
 * @author Network Projet Team
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiInvoker {

    private final WebClient.Builder webClientBuilder;
    private final TemplateEngine templateEngine;
    private final AuthenticationHandler authenticationHandler;
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    // ========== MÉTHODES PRINCIPALES ==========

    /**
     * Invoque une API externe basée sur la configuration de l'action
     * 
     * @param apiConfig Configuration API (baseUrl, timeout, auth, etc.)
     * @param action Action à exécuter (endpoint, method, body, headers)
     * @param sessionData Données de session pour le template rendering
     * @return Mono<ExternalApiResponse> Réponse de l'API externe
     */
    public Mono<ExternalApiResponse> invoke(
            ApiConfig apiConfig, 
            Action action, 
            Map<String, Object> sessionData) {
        
        log.info("Invoking external API: method={}, endpoint={}", 
            action.getMethod(), action.getEndpoint());

        // Validation des paramètres
        if (apiConfig == null || action == null || action.getEndpoint() == null) {
            log.error("Invalid API configuration or action");
            return Mono.just(ExternalApiResponse.builder()
                .status(ApiResponseStatus.CLIENT_ERROR)
                .errorMessage("Invalid API configuration or action")
                .build());
        }

        try {
            // Construction de la requête
            String url = buildUrl(apiConfig.getBaseUrl(), action.getEndpoint(), sessionData);
            Map<String, String> headers = buildHeaders(apiConfig, action, sessionData);
            Object requestBody = buildRequestBody(action, sessionData);
            HttpMethod method = action.getMethod() != null ? action.getMethod() : HttpMethod.POST;
            Duration timeout = apiConfig.getTimeout() != null 
                ? Duration.ofSeconds(apiConfig.getTimeout()) 
                : DEFAULT_TIMEOUT;

            log.debug("Built request: url={}, method={}, timeout={}s", url, method, timeout.getSeconds());

            // Exécution de la requête
            return executeRequest(url, method, requestBody, headers, timeout)
                .doOnSuccess(response -> log.info("API call successful: status={}", response.getStatus()))
                .doOnError(error -> log.error("API call failed: {}", error.getMessage()));

        } catch (Exception e) {
            log.error("Error preparing API request", e);
            return Mono.just(ExternalApiResponse.builder()
                .status(ApiResponseStatus.CLIENT_ERROR)
                .errorMessage("Error preparing request: " + e.getMessage())
                .build());
        }
    }

    /**
     * Construit et exécute une requête HTTP
     * 
     * @param url URL complète de la requête
     * @param method Méthode HTTP
     * @param body Corps de la requête (optionnel)
     * @param headers En-têtes HTTP (optionnel)
     * @param timeout Timeout de la requête
     * @return Mono<ExternalApiResponse> Réponse de l'API
     */
    public Mono<ExternalApiResponse> executeRequest(
            String url,
            HttpMethod method,
            Object body,
            Map<String, String> headers,
            Duration timeout) {
        
        log.debug("Executing {} request to {}", method, url);

        WebClient client = webClientBuilder.build();
        WebClient.RequestBodySpec requestSpec = client.method(convertToSpringHttpMethod(method))
            .uri(url);

        // Ajouter les headers
        if (headers != null && !headers.isEmpty()) {
            requestSpec.headers(httpHeaders -> headers.forEach(httpHeaders::add));
        }

        // Ajouter le body si présent
        if (body != null && requiresBody(method)) {
            requestSpec.bodyValue(body);
        }

        // Exécuter la requête avec timeout
        return requestSpec.retrieve()
            .toEntity(String.class)
            .timeout(timeout)
            .map(responseEntity -> ExternalApiResponse.builder()
                .status(ApiResponseStatus.SUCCESS)
                .statusCode(responseEntity.getStatusCode().value())
                .body(responseEntity.getBody())
                .headers(responseEntity.getHeaders().toSingleValueMap())
                .build())
            .onErrorResume(error -> handleException(error, url, method));
    }

    // ========== CONSTRUCTION DE LA REQUÊTE ==========

    /**
     * Construit l'URL complète en combinant baseUrl et endpoint avec template rendering
     */
    private String buildUrl(String baseUrl, String endpoint, Map<String, Object> sessionData) {
        // Rendre les templates dans l'endpoint
        String renderedEndpoint = templateEngine.render(endpoint, sessionData);
        
        if (baseUrl == null || baseUrl.isEmpty()) {
            return renderedEndpoint;
        }
        
        // Normaliser les slashes
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedEndpoint = renderedEndpoint.startsWith("/") ? renderedEndpoint : "/" + renderedEndpoint;
        
        return normalizedBase + normalizedEndpoint;
    }

    /**
     * Construit les headers en fusionnant config API + action + auth + template rendering
     */
    private Map<String, String> buildHeaders(
            ApiConfig apiConfig, 
            Action action, 
            Map<String, Object> sessionData) {
        
        Map<String, String> headers = new HashMap<>();
        
        // 1. Headers de la config API (si présents)
        if (apiConfig.getHeaders() != null) {
            apiConfig.getHeaders().forEach((key, value) -> 
                headers.put(key, templateEngine.render(value, sessionData))
            );
        }
        
        // 2. Headers de l'action (priorité plus haute)
        if (action.getHeaders() != null) {
            action.getHeaders().forEach((key, value) -> 
                headers.put(key, templateEngine.render(value, sessionData))
            );
        }
        
        // 3. Headers d'authentification (priorité maximale)
        if (apiConfig.getAuthentication() != null) {
            Map<String, String> authHeaders = authenticationHandler.buildAuthHeaders(
                apiConfig.getAuthentication(), 
                sessionData
            );
            headers.putAll(authHeaders);
        }
        
        log.debug("Built headers: {}", headers.keySet());
        return headers.isEmpty() ? null : headers;
    }

    /**
     * Construit le corps de la requête avec template rendering et mapping
     */
    private Object buildRequestBody(Action action, Map<String, Object> sessionData) {
        // 1. Si body explicite dans l'action
        if (action.getBody() != null) {
            return renderBodyTemplate(action.getBody(), sessionData);
        }
        
        // 2. Si requestMapping défini, mapper les données
        if (action.getRequestMapping() != null && !action.getRequestMapping().isEmpty()) {
            return buildMappedBody(action.getRequestMapping(), sessionData);
        }
        
        // 3. Par défaut, utiliser toutes les données de session
        return sessionData;
    }

    /**
     * Rend les templates dans le body (récursif pour Map et String)
     */
    private Object renderBodyTemplate(Object body, Map<String, Object> sessionData) {
        if (body instanceof String) {
            return templateEngine.render((String) body, sessionData);
        } else if (body instanceof Map) {
            Map<String, Object> renderedMap = new HashMap<>();
            ((Map<?, ?>) body).forEach((key, value) -> {
                String renderedKey = key.toString();
                Object renderedValue = value instanceof String 
                    ? templateEngine.render((String) value, sessionData)
                    : value;
                renderedMap.put(renderedKey, renderedValue);
            });
            return renderedMap;
        }
        return body;
    }

    /**
     * Construit le body en mappant les données selon requestMapping
     */
    private Map<String, Object> buildMappedBody(
            Map<String, String> requestMapping, 
            Map<String, Object> sessionData) {
        
        Map<String, Object> mappedBody = new HashMap<>();
        
        requestMapping.forEach((targetKey, sourceKey) -> {
            // Extraire la valeur avec support des nested paths (ex: "user.name")
            Object value = extractNestedValue(sessionData, sourceKey);
            if (value != null) {
                mappedBody.put(targetKey, value);
            } else {
                log.warn("Request mapping: source key '{}' not found in session data", sourceKey);
            }
        });
        
        log.debug("Mapped request body: {} → {} fields", requestMapping.size(), mappedBody.size());
        return mappedBody;
    }

    // ========== GESTION DES ERREURS ==========

    /**
     * Gère les exceptions lors des appels API
     */
    private Mono<ExternalApiResponse> handleException(Throwable error, String url, HttpMethod method) {
        log.error("Error calling external API [{} {}]: {}", method, url, error.getMessage());

        ExternalApiResponse.ExternalApiResponseBuilder responseBuilder = ExternalApiResponse.builder()
            .errorMessage(error.getMessage());

        // Erreur WebClient (4xx, 5xx)
        if (error instanceof WebClientResponseException webClientException) {
            responseBuilder
                .status(webClientException.getStatusCode().is5xxServerError()
                    ? ApiResponseStatus.SERVER_ERROR
                    : ApiResponseStatus.CLIENT_ERROR)
                .statusCode(webClientException.getStatusCode().value())
                .body(webClientException.getResponseBodyAsString());
        } 
        // Timeout
        else if (error instanceof TimeoutException || error instanceof java.net.SocketTimeoutException) {
            responseBuilder.status(ApiResponseStatus.TIMEOUT);
        } 
        // Erreur réseau
        else {
            responseBuilder.status(ApiResponseStatus.NETWORK_ERROR);
        }

        return Mono.just(responseBuilder.build());
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Convertit HttpMethod interne vers org.springframework.http.HttpMethod
     */
    private org.springframework.http.HttpMethod convertToSpringHttpMethod(HttpMethod method) {
        return switch (method) {
            case GET -> org.springframework.http.HttpMethod.GET;
            case POST -> org.springframework.http.HttpMethod.POST;
            case PUT -> org.springframework.http.HttpMethod.PUT;
            case DELETE -> org.springframework.http.HttpMethod.DELETE;
            case PATCH -> org.springframework.http.HttpMethod.PATCH;
        };
    }

    /**
     * Vérifie si la méthode HTTP requiert un body
     */
    private boolean requiresBody(HttpMethod method) {
        return method == HttpMethod.POST 
            || method == HttpMethod.PUT 
            || method == HttpMethod.PATCH;
    }

    /**
     * Extrait une valeur nested d'une Map (ex: "user.profile.name")
     */
    private Object extractNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }
}