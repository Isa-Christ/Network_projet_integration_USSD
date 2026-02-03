package com.network.projet.ussd.dto.response;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.ValidationReport;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposals;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat complet de la génération automatique (one-shot).
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoGenerationResult {
    private boolean success;
    private String message;
    
    // Étape 1: Analyse
    private ApiStructure api_structure;
    
    // Étape 2: Propositions
    private WorkflowProposals workflow_proposals;
    
    // Étape 3: Config finale
    private AutomatonDefinition generated_config;
    
    // Étape 4: Validation
    private ValidationReport validation_report;
    
    private long total_processing_time_ms;
    private String error_message;
}