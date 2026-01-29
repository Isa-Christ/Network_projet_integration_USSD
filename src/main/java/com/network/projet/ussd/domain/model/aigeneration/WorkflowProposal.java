// package com.network.projet.ussd.domain.model.aigeneration;

// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// import java.util.ArrayList;
// import java.util.List;

// /**
//  * Une proposition complète de workflow USSD.
//  * 
//  * @author Network Project Team 
//  * @since 2025-01-25
//  */
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class WorkflowProposal {
//     private String name;                    // "Simple", "Standard", "Avancé"
//     private String description;
//     private String complexity;              // "low", "medium", "high"
    
//     @Builder.Default
//     private List<StateProposal> states = new ArrayList<>();
    
//     private int estimated_states_count;
//     private String reasoning;               // Pourquoi ce workflow
    
//     /**
//      * Vérifie si le workflow a un état initial.
//      */
//     public boolean hasInitialState() {
//         return states.stream().anyMatch(StateProposal::is_initial);
//     }
    
//     /**
//      * Retourne l'état initial.
//      */
//     public StateProposal getInitialState() {
//         return states.stream()
//             .filter(StateProposal::is_initial)
//             .findFirst()
//             .orElse(null);
//     }
// }

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