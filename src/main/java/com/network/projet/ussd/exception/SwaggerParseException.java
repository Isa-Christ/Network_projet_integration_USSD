package com.network.projet.ussd.exception;

public class SwaggerParseException extends RuntimeException {
    public SwaggerParseException(String message) {
        super(message);
    }

    public SwaggerParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
