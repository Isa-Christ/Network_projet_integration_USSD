package com.network.projet.ussd.dto.response;

import com.network.projet.ussd.domain.model.aigeneration.ValidationReport;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat de la génération de configuration.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResult {
    private boolean success;
    private String message;
    private AutomatonDefinition generated_config;
    private ValidationReport validation_report;
    private long processing_time_ms;
    private String error_message;
    
    /**
     * Crée un résultat de succès.
     */
    public static GenerationResult success(AutomatonDefinition config, 
                                          ValidationReport report,
                                          long processing_time_ms) {
        return GenerationResult.builder()
            .success(true)
            .message("Configuration générée avec succès")
            .generated_config(config)
            .validation_report(report)
            .processing_time_ms(processing_time_ms)
            .build();
    }
    
    /**
     * Crée un résultat d'échec.
     */
    public static GenerationResult failure(String error_message) {
        return GenerationResult.builder()
            .success(false)
            .message("Échec de la génération")  // AJOUT
            .error_message(error_message)
            .processing_time_ms(0)  // AJOUT
            .build();
    }
}