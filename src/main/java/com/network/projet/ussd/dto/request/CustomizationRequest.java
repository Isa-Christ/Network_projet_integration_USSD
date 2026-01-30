package com.network.projet.ussd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Personnalisations manuelles de la configuration générée.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationRequest {
    
    @Builder.Default
    private Map<String, String> menu_text_overrides = new HashMap<>();
    
    @Builder.Default
    private Map<String, Object> state_modifications = new HashMap<>();
    
    private Boolean include_confirmation_steps;
    private Integer max_items_per_page;
}