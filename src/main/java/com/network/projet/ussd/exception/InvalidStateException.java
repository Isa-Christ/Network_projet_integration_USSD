package com.network.projet.ussd.exception;

/**
 * Exception thrown when an invalid state is encountered
 */
public class InvalidStateException extends RuntimeException {
    
    private String stateId;
    
    public InvalidStateException(String message) {
        super(message);
    }
    
    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InvalidStateException(String message, String stateId) {
        super(message);
        this.stateId = stateId;
    }
    
    public InvalidStateException(String message, String stateId, Throwable cause) {
        super(message, cause);
        this.stateId = stateId;
    }
    
    public String getStateId() {
        return stateId;
    }
    
    @Override
    public String toString() {
        if (stateId != null) {
            return super.toString() + " [stateId=" + stateId + "]";
        }
        return super.toString();
    }
}
