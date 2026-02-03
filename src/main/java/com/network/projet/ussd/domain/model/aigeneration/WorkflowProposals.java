// package com.network.projet.ussd.domain.model.aigeneration;

// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// /**
//  * Collection de propositions de workflows générées par le LLM.
//  * 
//  * @author Network project Team
//  * @since 2025-01-25
//  */
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class WorkflowProposals {
//     private String service_name;
    
//     @Builder.Default
//     private List<WorkflowProposal> proposals = new ArrayList<>();
    
//     @Builder.Default
//     private Map<String, MenuTextSuggestion> menu_texts = new HashMap<>();
    
//     @Builder.Default
//     private Map<String, ResponseSummary> response_summaries = new HashMap<>();
    
//     @Builder.Default
//     private Map<String, List<InputConfig>> input_states = new HashMap<>();
    
//     /**
//      * Retourne la proposition recommandée (généralement la "Standard").
//      */
//     public WorkflowProposal getRecommendedProposal() {
//         return proposals.stream()
//             .filter(p -> "Standard".equalsIgnoreCase(p.getName()))
//             .findFirst()
//             .orElse(proposals.isEmpty() ? null : proposals.get(0));
//     }
    
//     /**
//      * Classe interne pour les suggestions de textes menu.
//      */
//     @Data
//     @Builder
//     @NoArgsConstructor
//     @AllArgsConstructor
//     public static class MenuTextSuggestion {
//         private String suggestion;
//         private List<String> alternatives;
//         private String reasoning;
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
import java.util.Map;

/**
 * Ensemble de propositions de workflows générées par l'IA.
 * 
 * @author Network Project Team
 * @since 2025-01-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowProposals {
    
    @JsonProperty("service_name")
    private String service_name;
    
    @JsonProperty("proposals")
    @Builder.Default
    private List<WorkflowProposal> proposals = new ArrayList<>();
    
    @JsonProperty("menu_texts")
    @Builder.Default
    private Map<String, String> menu_texts = new java.util.HashMap<>();
    
    @JsonProperty("response_summaries")
    @Builder.Default
    private Map<String, String> response_summaries = new java.util.HashMap<>();
    
    @JsonProperty("input_states")
    @Builder.Default
    private Map<String, Object> input_states = new java.util.HashMap<>();
    
    @JsonProperty("recommendedProposal")
    private Integer recommendedProposal;
}