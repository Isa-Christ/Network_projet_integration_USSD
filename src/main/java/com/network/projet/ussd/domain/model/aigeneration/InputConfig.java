package com.network.projet.ussd.domain.model.aigeneration;

import com.network.projet.ussd.domain.enums.ValidationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration d'un état INPUT suggérée par le LLM.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputConfig {
    private String parameter;           // Nom du paramètre
    private String message;             // Message à afficher
    private String validation;          // Type validation (string)
    private ValidationType validation_type;  // Type validation (enum)
    private Integer min_length;
    private Integer max_length;
    private String data_type;           // Type de données attendu
    private String reasoning;           // Explication du LLM
}