package com.network.projet.ussd.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ServiceAlreadyExistsException extends RuntimeException {
    public ServiceAlreadyExistsException(String message) {
        super(message);
    }
}
