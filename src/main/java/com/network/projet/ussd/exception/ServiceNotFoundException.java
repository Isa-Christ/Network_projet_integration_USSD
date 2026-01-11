package com.network.projet.ussd.exception;

/**
 * Exception thrown when an invalid transition is attempted
 */
public class ServiceNotFoundException extends RuntimeException {
    
    private String fromStateId;
    private String toStateId;
    private String trigger;
    
    public ServiceNotFoundException(String message) {
        super(message);
    }
    
    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ServiceNotFoundException(String message, String fromStateId, String toStateId) {
        super(message);
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
    }
    
    public ServiceNotFoundException(String message, String fromStateId, 
                                     String toStateId, String trigger) {
        super(message);
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
        this.trigger = trigger;
    }
    
    public ServiceNotFoundException(String message, String fromStateId, 
                                     String toStateId, String trigger, Throwable cause) {
        super(message, cause);
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
        this.trigger = trigger;
    }
    
    public String getFromStateId() {
        return fromStateId;
    }
    
    public String getToStateId() {
        return toStateId;
    }
    
    public String getTrigger() {
        return trigger;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (fromStateId != null && toStateId != null) {
            sb.append(" [").append(fromStateId).append(" -> ").append(toStateId);
            if (trigger != null) {
                sb.append(" (").append(trigger).append(")");
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
