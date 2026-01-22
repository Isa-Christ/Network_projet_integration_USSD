package com.network.projet.ussd.domain.model.automaton;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import com.network.projet.ussd.dto.ExternalApiResponse;

/**
 * ActionResult - Result of action execution
 * 
 * @author Network Projet Team
 * @since 2026-01-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {
    /**
     * Indicates if action succeeded
     */
    private boolean success;
    
    /**
     * ID of the next state (from JSON definition)
     */
    private String nextState;
    
    /**
     * Response mapping (from JSON definition)
     */
    private Map<String, String> responseMapping;
    
    /**
     * Extracted data from API response
     */
    private Map<String, Object> responseData;
    
    /**
     * Raw external API response
     */
    private ExternalApiResponse apiResponse;
    
    /**
     * Message (from JSON definition)
     */
    private String message;
    
    /**
     * Error message in case of failure
     */
    private String errorMessage;
    
    /**
     * Exception thrown in case of error
     */
    private Throwable exception;
    
    /**
     * Helper method for success with next state
     */
    public static ActionResult success(String nextStateId, Map<String, Object> responseData) {
        return ActionResult.builder()
                .success(true)
                .nextState(nextStateId)
                .responseData(responseData)
                .build();
    }
    
    /**
     * Helper method for error
     */
    public static ActionResult error(String errorMessage, Throwable exception) {
        return ActionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .exception(exception)
                .build();
    }
}