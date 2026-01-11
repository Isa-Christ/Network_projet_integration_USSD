package com.network.projet.ussd.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RuntimeException {
    
    private Map<String, String> errorMap;
    private String fieldName;
    
    public ValidationException(String message) {
        super(message);
        this.errorMap = new HashMap<>();
    }
    
    public ValidationException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
        this.errorMap = new HashMap<>();
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorMap = new HashMap<>();
    }
    
    public ValidationException(String message, Map<String, String> errorMap) {
        super(message);
        this.errorMap = new HashMap<>(errorMap);
    }
    
    public ValidationException(String message, String fieldName, 
                              Map<String, String> errorMap, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
        this.errorMap = new HashMap<>(errorMap);
    }
    
    public void addError(String field, String error) {
        errorMap.put(field, error);
    }
    
    public Map<String, String> getErrorMap() {
        return Collections.unmodifiableMap(errorMap);
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public String getFieldError(String field) {
        return errorMap.get(field);
    }
    
    public boolean hasErrors() {
        return !errorMap.isEmpty();
    }
    
    public int getErrorCount() {
        return errorMap.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (!errorMap.isEmpty()) {
            sb.append(" [Errors: ");
            errorMap.forEach((k, v) -> sb.append(k).append("=").append(v).append("; "));
            sb.append("]");
        }
        return sb.toString();
    }
}
