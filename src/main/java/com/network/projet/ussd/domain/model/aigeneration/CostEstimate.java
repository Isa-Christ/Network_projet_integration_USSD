package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estimation du temps de génération (pour LLM local, pas de coût monétaire).
 * 
 * @author Network Project Team 
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostEstimate {
    private int estimated_input_tokens;
    private int estimated_output_tokens;
    private long estimated_processing_time_ms;  // Temps estimé en millisecondes
    
    /**
     * Calcule le temps estimé en secondes.
     */
    public double getEstimatedTimeSeconds() {
        return estimated_processing_time_ms / 1000.0;
    }
}