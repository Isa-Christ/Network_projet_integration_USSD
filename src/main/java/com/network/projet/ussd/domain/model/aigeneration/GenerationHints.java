package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Indications fournies par l'admin pour guider la génération.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationHints {
    private String service_name;
    private String primary_entity;      // Ex: "todos" (backward compatibility)
    
    @Builder.Default
    private List<String> primary_entities = new ArrayList<>();  // Ex: ["orders", "products", "customers"]
    
    private String short_code;           // Ex: "*123*1#"
    private String target_language;     // "fr", "en"
    
    @Builder.Default
    private int max_menu_options = 7;
    
    @Builder.Default
    private boolean include_delete_operations = true;
    
    @Builder.Default
    private boolean include_update_operations = true;
    
    @Builder.Default
    private Map<String, String> entity_relationships = new java.util.HashMap<>();  
    // Ex: {"orders": "has_many:products", "products": "belongs_to:customers"}
    
    private String[] priority_endpoints;  // Liste d'operation_id prioritaires
    
    @Builder.Default
    private List<String> critical_states = new ArrayList<>();  
    // États essentiels à inclure ex: ["authentication", "confirmation", "error_handling"]
    
    @Builder.Default
    private String complexity = "medium"; // low | medium | high
    
    /**
     * Retourne la langue cible avec fallback sur français.
     */
    public String getTargetLanguageOrDefault() {
        return target_language != null ? target_language : "fr";
    }
}