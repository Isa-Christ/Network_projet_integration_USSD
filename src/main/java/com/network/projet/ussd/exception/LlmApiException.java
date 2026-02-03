package com.network.projet.ussd.exception;

/**
 * Exception levée lors d'erreurs d'appel au LLM (Ollama).
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
public class LlmApiException extends RuntimeException {
    
    public LlmApiException(String message) {
        super(message);
    }
    
    public LlmApiException(String message, Throwable cause) {
        super(message, cause);
    }
}