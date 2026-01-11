package com.network.projet.ussd.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JsonPathExtractor - Extract values from JSON responses using path notation
 * 
 * Supported patterns:
 * - Simple: "data.trackingId" → response.data.trackingId
 * - Nested: "data.recipient.name" → response.data.recipient.name
 * - Array: "data.items[0].name" → response.data.items[0].name
 * - Root: "status" → response.status
 * 
 * @author Magne Isabelle Christ
 * @since 2026-01-06
 * @version 1.0.0
 */
@Slf4j
@Component
public class JsonPathExtractor {
    
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)]");
    
    /**
     * Extract value from JSON object using path notation
     * 
     * @param json JSON object (Map)
     * @param path Path notation (e.g., "data.trackingId")
     * @return Extracted value or null if path not found
     */
    public Object extract(Map<String, Object> json, String path) {
        if (json == null || path == null || path.trim().isEmpty()) {
            log.warn("Invalid input: json={}, path={}", json != null, path);
            return null;
        }
        
        try {
            return extractRecursive(json, path.split("\\."), 0);
        } catch (Exception e) {
            log.error("Failed to extract path '{}' from JSON", path, e);
            return null;
        }
    }
    
    /**
     * Extract with type casting to String
     * 
     * @param json JSON object
     * @param path Path notation
     * @return String value or null
     */
    public String extractAsString(Map<String, Object> json, String path) {
        Object value = extract(json, path);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Extract with type casting to Integer
     * 
     * @param json JSON object
     * @param path Path notation
     * @return Integer value or null
     */
    public Integer extractAsInt(Map<String, Object> json, String path) {
        Object value = extract(json, path);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot convert '{}' to Integer", value);
            return null;
        }
    }
    
    /**
     * Extract with type casting to Double
     * 
     * @param json JSON object
     * @param path Path notation
     * @return Double value or null
     */
    public Double extractAsDouble(Map<String, Object> json, String path) {
        Object value = extract(json, path);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot convert '{}' to Double", value);
            return null;
        }
    }
    
    /**
     * Extract with type casting to Boolean
     * 
     * @param json JSON object
     * @param path Path notation
     * @return Boolean value or null
     */
    public Boolean extractAsBoolean(Map<String, Object> json, String path) {
        Object value = extract(json, path);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        String str_value = value.toString().toLowerCase();
        return "true".equals(str_value) || "1".equals(str_value) || "yes".equals(str_value);
    }
    
    /**
     * Recursive extraction logic
     * 
     * @param current Current object being navigated
     * @param path_parts Array of path parts
     * @param index Current index in path_parts
     * @return Extracted value or null
     */
    private Object extractRecursive(Object current, String[] path_parts, int index) {
        if (index >= path_parts.length) {
            return current;
        }
        
        if (current == null) {
            return null;
        }
        
        String part = path_parts[index];
        
        Matcher array_matcher = ARRAY_PATTERN.matcher(part);
        if (array_matcher.matches()) {
            String key = array_matcher.group(1);
            int array_index = Integer.parseInt(array_matcher.group(2));
            
            if (current instanceof Map) {
                Object list = ((Map<?, ?>) current).get(key);
                if (list instanceof List) {
                    List<?> list_obj = (List<?>) list;
                    if (array_index >= 0 && array_index < list_obj.size()) {
                        return extractRecursive(
                            list_obj.get(array_index), 
                            path_parts, 
                            index + 1
                        );
                    }
                }
            }
            return null;
        }
        
        if (current instanceof Map) {
            Object next_value = ((Map<?, ?>) current).get(part);
            return extractRecursive(next_value, path_parts, index + 1);
        }
        
        log.warn("Cannot navigate path '{}' - current object is not a Map", part);
        return null;
    }
    
    /**
     * Extract multiple paths at once
     * 
     * @param json JSON object
     * @param path_mappings Map of alias to path (e.g., {"id": "data.trackingId"})
     * @return Map of alias to extracted value
     */
    public Map<String, Object> extractMultiple(
        Map<String, Object> json, 
        Map<String, String> path_mappings
    ) {
        Map<String, Object> results = new java.util.HashMap<>();
        
        if (path_mappings != null) {
            path_mappings.forEach((alias, path) -> {
                Object value = extract(json, path);
                if (value != null) {
                    results.put(alias, value);
                }
            });
        }
        
        return results;
    }
    
    /**
     * Check if path exists in JSON
     * 
     * @param json JSON object
     * @param path Path notation
     * @return true if path exists and has non-null value
     */
    public boolean pathExists(Map<String, Object> json, String path) {
        return extract(json, path) != null;
    }
    
    /**
     * Extract with default value if not found
     * 
     * @param json JSON object
     * @param path Path notation
     * @param default_value Default value to return if path not found
     * @return Extracted value or default_value
     */
    public Object extractOrDefault(
        Map<String, Object> json, 
        String path, 
        Object default_value
    ) {
        Object value = extract(json, path);
        return value != null ? value : default_value;
    }
}