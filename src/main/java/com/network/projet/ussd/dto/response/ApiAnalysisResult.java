package com.network.projet.ussd.dto.response;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.CostEstimate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat de l'analyse d'une source API.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAnalysisResult {
    private boolean success;
    private String message;
    private ApiStructure api_structure;
    private CostEstimate cost_estimate;
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    private String error_message;
    
    /**
     * Crée un résultat de succès.
     */
    public static ApiAnalysisResult success(ApiStructure api_structure, 
                                           CostEstimate cost_estimate) {
        return ApiAnalysisResult.builder()
            .success(true)
            .message("API analysée avec succès")
            .api_structure(api_structure)
            .cost_estimate(cost_estimate)
            .warnings(new ArrayList<>())  // AJOUT
            .build();
    }
    
    /**
     * Crée un résultat d'échec.
     */
    public static ApiAnalysisResult failure(String error_message) {
        return ApiAnalysisResult.builder()
            .success(false)
            .message("Échec de l'analyse")  // AJOUT
            .error_message(error_message)
            .warnings(new ArrayList<>())  // AJOUT
            .build();
    }
}