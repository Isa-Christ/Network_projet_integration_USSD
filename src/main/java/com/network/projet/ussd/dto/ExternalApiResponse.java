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
import java.util.List;
import java.util.Map;

/**
 * ExternalApiResponse - DTO représentant la réponse d'une API externe
 * 
 * @author Network Projet Team
 * @version 2.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ExternalApiResponse {

    private ApiResponseStatus status;
    private Integer statusCode;
    private String body;
    
    /**
     * Données parsées de la réponse
     * Peut être soit Map<String, Object> soit List<Map<String, Object>>
     */
    private Object data;
    
    private Map<String, String> headers;
    private String errorMessage;

    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
    private Long duration;

    // ========== MÉTHODES UTILITAIRES ==========

    public boolean isSuccess() {
        return status == ApiResponseStatus.SUCCESS;
    }

    public boolean isError() {
        return status != ApiResponseStatus.SUCCESS;
    }

    public boolean isHttpSuccess() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    public boolean isClientError() {
        return statusCode != null && statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode != null && statusCode >= 500;
    }

    /**
     * Parse le body JSON en Object (Map ou List) si pas déjà fait
     */
    public Object getData() {
        if (data == null && body != null && !body.isEmpty()) {
            data = parseBody();
        }
        return data != null ? data : new HashMap<>();
    }

    /**
     * Récupère les données sous forme de Map
     * Si data est une List, retourne une Map avec "data" comme clé
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDataAsMap() {
        Object parsedData = getData();
        
        if (parsedData instanceof Map) {
            return (Map<String, Object>) parsedData;
        } else if (parsedData instanceof List) {
            // Si c'est un tableau, on le wrappe dans une Map
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("data", parsedData);
            return wrapper;
        }
        
        return new HashMap<>();
    }

    /**
     * Récupère les données sous forme de List
     * Si data est une Map, retourne null
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDataAsList() {
        Object parsedData = getData();
        
        if (parsedData instanceof List) {
            return (List<Map<String, Object>>) parsedData;
        }
        
        return null;
    }

    /**
     * Vérifie si les données sont un tableau
     */
    public boolean isDataArray() {
        return getData() instanceof List;
    }

    /**
     * Vérifie si les données sont un objet
     */
    public boolean isDataObject() {
        return getData() instanceof Map;
    }

    public Object getDataValue(String path) {
        Object parsedData = getData();
        
        // Si c'est un tableau et qu'on demande "data", retourner le tableau
        if (parsedData instanceof List && "data".equals(path)) {
            return parsedData;
        }
        
        // Si c'est un tableau et qu'on demande "$", retourner le tableau
        if (parsedData instanceof List && "$".equals(path)) {
            return parsedData;
        }
        
        Map<String, Object> currentData = getDataAsMap();
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

    public String getDataValueAsString(String path) {
        Object value = getDataValue(path);
        return value != null ? value.toString() : null;
    }

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

    public String getHeader(String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }

        return headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public boolean isJsonResponse() {
        String contentType = getContentType();
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    // ========== MÉTHODES PRIVÉES ==========

    /**
     * Parse le body JSON en Object (Map ou List)
     */
    private Object parseBody() {
        if (body == null || body.isEmpty()) {
            return new HashMap<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String trimmedBody = body.trim();
            
            // Si ça commence par '[', c'est un tableau
            if (trimmedBody.startsWith("[")) {
                return mapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
            } else {
                // Sinon c'est un objet
                return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse response body as JSON: {}", e.getMessage());
            log.debug("Body content: {}", body);
            return new HashMap<>();
        }
    }

    // ========== BUILDERS STATIQUES UTILITAIRES ==========

    public static ExternalApiResponse success(int statusCode, String body, Map<String, String> headers) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.SUCCESS)
            .statusCode(statusCode)
            .body(body)
            .headers(headers)
            .build();
    }

    public static ExternalApiResponse clientError(int statusCode, String body, String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.CLIENT_ERROR)
            .statusCode(statusCode)
            .body(body)
            .errorMessage(errorMessage)
            .build();
    }

    public static ExternalApiResponse serverError(int statusCode, String body, String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.SERVER_ERROR)
            .statusCode(statusCode)
            .body(body)
            .errorMessage(errorMessage)
            .build();
    }

    public static ExternalApiResponse timeout(String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.TIMEOUT)
            .statusCode(0)
            .errorMessage(errorMessage)
            .build();
    }

    public static ExternalApiResponse networkError(String errorMessage) {
        return ExternalApiResponse.builder()
            .status(ApiResponseStatus.NETWORK_ERROR)
            .statusCode(0)
            .errorMessage(errorMessage)
            .build();
    }

    @Override
    public String toString() {
        return String.format(
            "ExternalApiResponse{status=%s, statusCode=%d, hasBody=%s, dataType=%s, " +
            "headers=%d, errorMessage='%s', duration=%dms}",
            status, statusCode, body != null, 
            data != null ? (data instanceof List ? "Array" : "Object") : "null",
            headers != null ? headers.size() : 0,
            errorMessage, duration != null ? duration : 0
        );
    }
}