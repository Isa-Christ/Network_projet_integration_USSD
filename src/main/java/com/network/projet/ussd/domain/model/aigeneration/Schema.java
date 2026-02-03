package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente un schéma de données (modèle) extrait du Swagger.
 * 
 * @author Netxork Project team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schema {
    private String name;                    // Nom du schéma (ex: "Todo", "User")
    private String type;                    // "object", "array", "string", etc.
    private String description;
    
    @Builder.Default
    private Map<String, SchemaProperty> properties = new HashMap<>();
    
    @Builder.Default
    private List<String> required_fields = new ArrayList<>();
    
    /**
     * Vérifie si un champ est obligatoire.
     */
    public boolean isFieldRequired(String field_name) {
        return required_fields.contains(field_name);
    }
    
    /**
     * Retourne les propriétés obligatoires uniquement.
     */
    public Map<String, SchemaProperty> getRequiredProperties() {
        Map<String, SchemaProperty> required = new HashMap<>();
        for (String field : required_fields) {
            if (properties.containsKey(field)) {
                required.put(field, properties.get(field));
            }
        }
        return required;
    }
    
    /**
     * Classe interne représentant une propriété de schéma.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchemaProperty {
        private String name;
        private String type;            // string, integer, boolean, array, object
        private String format;          // date-time, email, etc.
        private String description;
        private Object default_value;
        private Integer min_length;
        private Integer max_length;
        private String ref;             // Référence à un autre schéma
    }
}