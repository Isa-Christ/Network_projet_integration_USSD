package com.network.projet.ussd.exception;

/**
 * Exception levée lors d'erreurs de génération IA.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
public class AiGenerationException extends RuntimeException {
    
    public AiGenerationException(String message) {
        super(message);
    }
    
    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}