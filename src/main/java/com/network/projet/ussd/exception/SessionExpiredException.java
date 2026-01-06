package com.network.projet.ussd.exception;

/**
 * Exception thrown when a user session has expired
 */
public class SessionExpiredException extends RuntimeException {
    
    private String sessionId;
    private long expirationTime;
    
    public SessionExpiredException(String message) {
        super(message);
    }
    
    public SessionExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SessionExpiredException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
    }
    
    public SessionExpiredException(String message, String sessionId, long expirationTime) {
        super(message);
        this.sessionId = sessionId;
        this.expirationTime = expirationTime;
    }
    
    public SessionExpiredException(String message, String sessionId, 
                                  long expirationTime, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
        this.expirationTime = expirationTime;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (sessionId != null) {
            sb.append(" [sessionId=").append(sessionId);
            if (expirationTime > 0) {
                sb.append(", expired at ").append(expirationTime);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
