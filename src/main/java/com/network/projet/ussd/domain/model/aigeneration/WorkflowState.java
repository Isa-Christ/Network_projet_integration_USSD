package com.network.projet.ussd.domain.model.aigeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.network.projet.ussd.domain.enums.StateType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Un état dans le workflow proposé.
 * 
 * @author Network Project Team
 * @since 2025-01-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowState {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private StateType type;
    
    @JsonProperty("is_initial")
    private boolean is_initial;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("linked_endpoint")
    private String linked_endpoint;
    
    @JsonProperty("parameter_name")
    private String parameter_name;
    
    @JsonProperty("transitions")
    @Builder.Default
    private List<WorkflowTransition> transitions = new ArrayList<>();
    
    @JsonProperty("reasoning")
    private String reasoning;
}
