package com.network.projet.ussd.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ServiceNotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleServiceNotFound(ServiceNotFoundException ex) {
        log.error("Service not found: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND)));
    }
    
    @ExceptionHandler(SessionExpiredException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleSessionExpired(SessionExpiredException ex) {
        log.warn("Session expired: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.GONE)
            .body(createErrorResponse(ex.getMessage(), HttpStatus.GONE)));
    }
    
    @ExceptionHandler(InvalidStateException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleInvalidState(InvalidStateException ex) {
        log.error("Invalid state: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST)));
    }
    
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST)));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("Une erreur inattendue s'est produite", HttpStatus.INTERNAL_SERVER_ERROR)));
    }
    
    private Map<String, Object> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        return error;
    }
}