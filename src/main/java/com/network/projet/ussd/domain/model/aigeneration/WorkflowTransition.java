package com.network.projet.ussd.domain.model.aigeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Une transition entre états.
 * 
 * @author Network Project Team
 * @since 2025-01-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransition {
    
    @JsonProperty("input")
    private String input;
    
    @JsonProperty("condition")
    private String condition;
    
    @JsonProperty("nextState")
    private String nextState;
    
    @JsonProperty("message")
    private String message;
}