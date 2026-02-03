package com.network.projet.ussd.exception;

import lombok.Getter;

@Getter
public class ApiCallException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;
    private final String url;

    public ApiCallException(int statusCode, String responseBody, String url) {
        super(statusCode + " from " + url);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.url = url;
    }
}