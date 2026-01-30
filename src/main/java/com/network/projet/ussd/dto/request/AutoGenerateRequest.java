package com.network.projet.ussd.dto.request;

import com.network.projet.ussd.domain.enums.SourceType;
import com.network.projet.ussd.domain.model.aigeneration.GenerationHints;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour génération automatique complète (one-shot).
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoGenerateRequest {
    
    @NotNull(message = "Le type de source est obligatoire")
    private SourceType source_type;
    
    private String source_url;
    private String file_content;
    
    @NotNull(message = "Les indications sont obligatoires")
    private GenerationHints hints;
    
    @Builder.Default
    private Integer selected_proposal_index = 1;  // Par défaut: Standard (index 1)
}