package com.network.projet.ussd.domain.model.aigeneration;

import com.network.projet.ussd.domain.enums.StateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proposition d'état générée par le LLM.
 * 
 * @author Network Project Team 
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateProposal {
    private String id;
    private String name;
    private StateType type;
    private boolean is_initial;
    private String message;
    private String linked_endpoint;      // operation_id de l'endpoint associé
    private String parameter_name;       // Si INPUT, quel paramètre
    
    @Builder.Default
    private List<Map<String, String>> transitions = new ArrayList<>();
    
    private String reasoning;            // Explication du LLM
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}