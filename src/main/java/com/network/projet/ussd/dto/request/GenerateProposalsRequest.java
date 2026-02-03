package com.network.projet.ussd.dto.request;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.GenerationHints;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour générer des propositions de workflows.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateProposalsRequest {
    
    @NotNull(message = "La structure API est obligatoire")
    private ApiStructure api_structure;
    
    @NotNull(message = "Les indications sont obligatoires")
    private GenerationHints hints;
}