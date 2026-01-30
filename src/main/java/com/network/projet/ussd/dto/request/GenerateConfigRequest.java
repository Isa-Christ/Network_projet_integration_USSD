package com.network.projet.ussd.dto.request;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposals;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Requête pour générer la configuration finale.
 * 
 * @author Network project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateConfigRequest {
    
    @NotNull(message = "La structure API est obligatoire")
    private ApiStructure api_structure;
    
    @NotNull(message = "Les propositions sont obligatoires")
    private WorkflowProposals workflow_proposals;
    
    @NotNull(message = "L'index de proposition sélectionnée est obligatoire")
    @Min(value = 0, message = "L'index doit être >= 0")
    @Max(value = 2, message = "L'index doit être <= 2")
    private Integer selected_proposal_index;
    
    @Builder.Default
    private Map<String, Object> customizations = new HashMap<>();
}