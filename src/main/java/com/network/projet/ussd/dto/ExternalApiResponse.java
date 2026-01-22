package com.network.projet.ussd.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.enums.ApiResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * ExternalApiResponse - DTO représentant la réponse d'une API externe
 * 
 * Rôle: Encapsule la réponse complète d'un appel API externe
 * Champs: status, statusCode, body, headers, errorMessage, data
 * Appelle: ApiResponseStatus
 * 
 * Utilisation:
 * - Retourné par ApiInvoker après chaque appel API
 * - Utilisé par AutomatonEngine pour traiter les réponses
 * - Contient à la fois les données brutes (body) et parsées (data)
 * 
 * @author Network Projet Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ExternalApiResponse {

    /**
     * Statut de la réponse API
     * Valeurs possibles: SUCCESS, CLIENT_ERROR, SERVER_ERROR, TIMEOUT, NETWORK_ERROR
     */
    private ApiResponseStatus status;

    /**
     * Code HTTP de la réponse (200, 404, 500, etc.)
     * - 2xx: Succès
     * - 4xx: Erreur client (requête invalide)
     * - 5xx: Erreur serveur
     * - 0: Pas de réponse (timeout, erreur réseau)
     */
    private Integer statusCode;

    /**
     * Corps de la réponse brute (String)
     * Typiquement du JSON, mais peut être XML, HTML, texte, etc.
     */
    private String body;

    /**
     * Données parsées de la réponse (Map)
     * Contient le JSON parsé si le body est valide
     * Utilisé pour l'extraction facile de données via JsonPath
     */
    private Map<String, Object> data;

    /**
     * En-têtes HTTP de la réponse
     * Exemples: Content-Type, Authorization, X-Request-Id, etc.
     */
    private Map<String, String> headers;

    /**
     * Message d'erreur si la requête a échoué
     * Contient la description de l'erreur pour le debugging
     */
    private String errorMessage;

    /**
     * Timestamp de la réponse (milliseconds)
     * Utilisé pour mesurer le temps de réponse
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();

    /**
     * Durée de la requête en millisecondes
     * Temps écoulé entre l'envoi et la réception
     */
    private Long duration;

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Vérifie si la réponse est un succès
     * 
     * @return boolean true si status == SUCCESS
     */
    public boolean isSuccess() {
        return status == ApiResponseStatus.SUCCESS;
    }

    /**
     * Vérifie si la réponse est une erreur
     * 
     * @return boolean true si status != SUCCESS
     */
    public boolean isError() {
        return status != ApiResponseStatus.SUCCESS;
    }

    /**
     * Vérifie si le code HTTP indique un succès (2xx)
     * 
     * @return boolean true si statusCode entre 200 et 299
     */
    public boolean isHttpSuccess() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    /**
     * Vérifie si c'est une erreur client (4xx)
     * 
     * @return boolean true si statusCode entre 400 et 499
     */
    public boolean isClientError() {
        return statusCode != null && statusCode >= 400 && statusCode < 500;
    }

    /**
     * Vérifie si c'est une erreur serveur (5xx)
     * 
     * @return boolean true si statusCode >= 500
     */
    public boolean isServerError() {
        return statusCode != null && statusCode >= 500;
    }

    /**
     * Parse le body JSON en Map si pas déjà fait
     * Utilise un ObjectMapper statique pour éviter les dépendances
     * 
     * @return Map<String, Object> Données parsées ou map vide si erreur
     */
    public Map<String, Object> getData() {
        if (data == null && body != null && !body.isEmpty()) {
            data = parseBodyToMap();
        }
        return data != null ? data : new HashMap<>();
    }

    /**
     * Extrait une valeur du body parsé via un chemin
     * Supporte les chemins imbriqués: "user.profile.name"
     * 
     * @param path Chemin vers la valeur (ex: "data.userId")
     * @return Object Valeur extraite ou null si non trouvée
     */
    public Object getDataValue(String path) {
        Map<String, Object> currentData = getData();
        if (path == null || path.isEmpty() || currentData.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = currentData;

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

    /**
     * Extrait une valeur string du body parsé
     * 
     * @param path Chemin vers la valeur
     * @return String Valeur extraite ou null
     */
    public String getDataValueAsString(String path) {
        Object value = getDataValue(path);
        return value != null ? value.toString() : null;
    }

    /**
     * Extrait une valeur integer du body parsé
     * 
     * @param path Chemin vers la valeur
     * @return Integer Valeur extraite ou null
     */
    public Integer getDataValueAsInteger(String path) {
        Object value = getDataValue(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : null;
        } catch (NumberFormatException e) {
            log.warn("Cannot convert value at path '{}' to Integer: {}", path, value);
            return null;
        }
    }

    /**
     * Récupère un header spécifique (case-insensitive)
     * 
     * @param headerName Nom du header
     * @return String Valeur du header ou null
     */
    public String getHeader(String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }

        // Recherche case-insensitive
        return headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    /**
     * Récupère le Content-Type de la réponse
     * 
     * @return String Content-Type ou null
     */
    public String getContentType() {
        return getHeader("Content-Type");
    }

    /**
     * Vérifie si la réponse contient du JSON
     * 
     * @return boolean true si Content-Type contient "json"
     */
    public boolean isJsonResponse() {
        String contentType = getContentType();
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    // ========== MÉTHODES PRIVÉES ==========

    /**
     * Parse le body JSON en Map
     */
    private Map<String, Object> parseBodyToMap() {
        if (body == null || body.isEmpty()) {
            return new HashMap<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse response body as JSON: {}", e.getMessage());
            log.debug("Body content: {}", body);
            return new HashMap<>();
        }
    }

    // ========== BUILDERS STATIQUES UTILITAIRES ==========

    /**
     * Crée une réponse de succès
     */
    public static ExternalApiResponse success(int statusCode, String body, Map<String, String> headers) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.SUCCESS)
            .statusCode(statusCode)
            .body(body)
            .headers(headers)
            .build();
    }

    /**
     * Crée une réponse d'erreur client
     */
    public static ExternalApiResponse clientError(int statusCode, String body, String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.CLIENT_ERROR)
            .statusCode(statusCode)
            .body(body)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Crée une réponse d'erreur serveur
     */
    public static ExternalApiResponse serverError(int statusCode, String body, String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.SERVER_ERROR)
            .statusCode(statusCode)
            .body(body)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Crée une réponse de timeout
     */
    public static ExternalApiResponse timeout(String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.TIMEOUT)
            .statusCode(0)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Crée une réponse d'erreur réseau
     */
    public static ExternalApiResponse networkError(String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.NETWORK_ERROR)
            .statusCode(0)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Représentation string pour le debugging
     */
    @Override
    public String toString() {
        return String.format(
            "ExternalApiResponse{status=%s, statusCode=%d, hasBody=%s, hasData=%s, " +
            "headers=%d, errorMessage='%s', duration=%dms}",
            status, statusCode, body != null, data != null,
            headers != null ? headers.size() : 0,
            errorMessage, duration != null ? duration : 0
        );
    }
}