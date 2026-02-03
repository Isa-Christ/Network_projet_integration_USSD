package com.network.projet.ussd.service.external;

import com.network.projet.ussd.domain.enums.ApiResponseStatus;
import com.network.projet.ussd.domain.enums.HttpMethod;
import com.network.projet.ussd.domain.model.automaton.Action;
import com.network.projet.ussd.domain.model.automaton.ApiConfig;
import com.network.projet.ussd.dto.ExternalApiResponse;
import com.network.projet.ussd.util.TemplateEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.exception.ApiCallException;

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
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    // ========== MÉTHODES PRINCIPALES ==========

    /**
     * Invoque une API externe basée sur la configuration de l'action
     * 
     * @param apiConfig   Configuration API (baseUrl, timeout, auth, etc.)
     * @param action      Action à exécuter (endpoint, method, body, headers)
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
            String url = buildUrl(apiConfig.getBaseUrl(), action.getEndpoint(), sessionData, apiConfig);
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
     * @param url     URL complète de la requête
     * @param method  Méthode HTTP
     * @param body    Corps de la requête (optionnel)
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

        // Construction de la requête
        WebClient.RequestBodySpec requestSpec = client
                .method(convertToSpringHttpMethod(method))
                .uri(url);

        // Ajouter les headers
        if (headers != null && !headers.isEmpty()) {
            requestSpec.headers(h -> headers.forEach(h::add));
        }

        // Ajouter le body si présent
        if (body != null && requiresBody(method)) {
            requestSpec.bodyValue(body);
        }

        // Exécuter avec gestion des erreurs
        return requestSpec
                .retrieve()
                .onStatus(
    status -> status.is4xxClientError(),
    response -> {
        log.error("Error calling external API [{} {}]: {}", method, url, response.statusCode());
        return response.bodyToMono(String.class)
            .flatMap(errorBody -> {
                log.error("Error response body: {}", errorBody);
                // Créer une exception personnalisée avec le body
                return Mono.error(new ApiCallException(
                    response.statusCode().value(),
                    errorBody,
                    url
                ));
            });
    })
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> {
                            log.error("Server error calling external API [{} {}]: {}",
                                    method, url, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    response.statusCode().value() + " from " + method + " " + url));
                        })
                .bodyToMono(String.class)
                .timeout(timeout)
                .map(responseBody -> {
                    Object data = null;
                    try {
                        data = objectMapper.readValue(responseBody, Object.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse response as JSON", e);
                    }

                    return ExternalApiResponse.builder()
                            .status(ApiResponseStatus.SUCCESS)
                            .statusCode(200)
                            .body(responseBody)
                            .data(data)
                            .build();
                })
                .onErrorResume(error -> handleException(error, url, method));
    }

    // ========== CONSTRUCTION DE LA REQUÊTE ==========

    /**
     * Construit l'URL complète en combinant baseUrl et endpoint avec template
     * rendering
     */
    private String buildUrl(String baseUrl, String endpoint, Map<String, Object> sessionData, ApiConfig apiConfig) {
        // Rendre les templates dans l'endpoint
        String renderedEndpoint = templateEngine.render(endpoint, sessionData);

        if (baseUrl == null || baseUrl.isEmpty()) {
            return renderedEndpoint;
        }

        // Normaliser les slashes
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedEndpoint = renderedEndpoint.startsWith("/") ? renderedEndpoint : "/" + renderedEndpoint;

        String fullUrl = normalizedBase + normalizedEndpoint;

        // Ajouter les query params d'authentification si API_KEY avec paramName
        if (apiConfig.getAuthentication() != null) {
            Map<String, String> queryParams = authenticationHandler.extractQueryParams(apiConfig.getAuthentication());
            if (queryParams != null && !queryParams.isEmpty()) {
                String separator = fullUrl.contains("?") ? "&" : "?";
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    fullUrl += separator + entry.getKey() + "=" + entry.getValue();
                    separator = "&";
                }
            }
        }

        return fullUrl;
    }

    /**
     * Construit les headers en fusionnant config API + action + auth + template
     * rendering
     */
    private Map<String, String> buildHeaders(
            ApiConfig apiConfig,
            Action action,
            Map<String, Object> sessionData) {

        Map<String, String> headers = new HashMap<>();

        // 1. Headers de la config API (si présents)
        if (apiConfig.getHeaders() != null) {
            apiConfig.getHeaders().forEach((key, value) -> headers.put(key, templateEngine.render(value, sessionData)));
        }

        // 2. Headers de l'action (priorité plus haute)
        if (action.getHeaders() != null) {
            action.getHeaders().forEach((key, value) -> headers.put(key, templateEngine.render(value, sessionData)));
        }

        // 3. Headers d'authentification (priorité maximale)
        if (apiConfig.getAuthentication() != null) {
            Map<String, String> authHeaders = authenticationHandler.buildAuthHeaders(
                    apiConfig.getAuthentication(),
                    sessionData);
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
            Object resolvedBody = renderBodyTemplate(action.getBody(), sessionData);

            // ✅ AJOUTE CES LOGS ICI
            try {
                log.info(">>> REQUEST BODY (resolved): {}", objectMapper.writeValueAsString(resolvedBody));
            } catch (Exception e) {
                log.warn("Could not serialize body for logging", e);
            }

            return resolvedBody;
        }

        // 2. Si requestMapping défini, mapper les données
        if (action.getRequestMapping() != null && !action.getRequestMapping().isEmpty()) {
            Map<String, Object> mappedBody = buildMappedBody(action.getRequestMapping(), sessionData);

            // ✅ AJOUTE CES LOGS ICI AUSSI
            try {
                log.info(">>> REQUEST BODY (mapped): {}", objectMapper.writeValueAsString(mappedBody));
            } catch (Exception e) {
                log.warn("Could not serialize body for logging", e);
            }

            return mappedBody;
        }

        // 3. Par défaut, utiliser toutes les données de session
        log.info(">>> REQUEST BODY (sessionData): {}", sessionData.keySet());
        return sessionData;
    }

    /**
     * Rend les templates dans le body (récursif pour Map et String)
     */
    private Object renderBodyTemplate(Object body, Map<String, Object> sessionData) {
        log.debug(">>> Rendering body template with sessionData keys: {}", sessionData.keySet());

        if (body instanceof String) {
            return templateEngine.render((String) body, sessionData);
        } else if (body instanceof Map) {
            Map<String, Object> renderedMap = new HashMap<>();
            ((Map<?, ?>) body).forEach((key, value) -> {
                String renderedKey = key.toString();
                Object renderedValue = value instanceof String
                        ? templateEngine.render((String) value, sessionData)
                        : value;

                // ✅ LOG CHAQUE CHAMP
                log.debug(">>> Body field: {} = {} (rendered: {})", renderedKey, value, renderedValue);

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

        // Simplement propager l'erreur pour que AutomatonEngine aille dans
        // onErrorResume()
        return Mono.error(error);
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