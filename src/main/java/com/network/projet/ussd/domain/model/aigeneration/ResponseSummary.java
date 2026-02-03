package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration pour résumer une réponse API en format USSD.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseSummary {
    private String template;            // Template Handlebars
    private String reasoning;           // Pourquoi ce template
    private Integer max_items;          // Max items à afficher
    private boolean pagination_needed;  // Si pagination nécessaire
    private String[] display_fields;    // Champs à afficher
}