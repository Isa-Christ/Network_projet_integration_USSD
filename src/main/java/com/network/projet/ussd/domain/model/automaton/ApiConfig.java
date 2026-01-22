package com.network.projet.ussd.domain.model.automaton;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiConfig {
    private String baseUrl;
    private Integer timeout;
    private Integer retryAttempts;
    private Authentication authentication;
    
    /**
     * Headers par défaut pour toutes les requêtes API
     * Utilisé dans ApiInvoker lignes 178-179
     */
    private Map<String, String> headers;
}