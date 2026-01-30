package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.CostEstimate;
import org.springframework.stereotype.Service;

/**
 * Estimateur de temps de génération (pas de coût pour LLM local).
 * 
 * @author Your Name
 * @since 2025-01-26
 */
@Service
public class CostEstimator {
    
    /**
     * Estime le temps de génération.
     */
    public CostEstimate estimate(ApiStructure api_structure) {
        int endpoint_count = api_structure.getEndpoints().size();
        
        // Estimation tokens input
        int estimated_input_tokens = 1500 + (endpoint_count * 200);
        
        // Estimation tokens output
        int states_per_proposal = endpoint_count * 3;
        int estimated_output_tokens = states_per_proposal * 150 * 3; // 3 propositions
        
        // Estimation temps (Ollama local)
        // Environ 20 tokens/sec pour Llama 3.2
        long estimated_time_ms = ((estimated_input_tokens + estimated_output_tokens) / 20) * 1000;
        
        return CostEstimate.builder()
            .estimated_input_tokens(estimated_input_tokens)
            .estimated_output_tokens(estimated_output_tokens)
            .estimated_processing_time_ms(estimated_time_ms)
            .build();
    }
}