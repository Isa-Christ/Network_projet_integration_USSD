package com.network.projet.ussd.domain.model.aigeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Une proposition de workflow individuelle.
 * 
 * @author Network Project Team
 * @since 2025-01-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowProposal {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("complexity")
    private String complexity;
    
    @JsonProperty("states")
    @Builder.Default
    private List<WorkflowState> states = new ArrayList<>();
    
    @JsonProperty("estimated_states_count")
    private Integer estimated_states_count;
    
    @JsonProperty("reasoning")
    private String reasoning;
}