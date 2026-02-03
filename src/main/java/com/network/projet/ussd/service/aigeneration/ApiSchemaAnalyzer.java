package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.enums.EndpointType;
import com.network.projet.ussd.domain.model.aigeneration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

//import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyseur de structure API (classification, dépendances).
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Service
@Slf4j
public class ApiSchemaAnalyzer {
    
    /**
     * Analyse une structure API et construit le graphe de dépendances.
     */
    public Mono<ApiStructure> analyze(ApiStructure raw_structure) {
        return Mono.fromCallable(() -> {
            log.info("Analyzing API structure: {} endpoints", raw_structure.getEndpoints().size());
            
            // Construire le graphe de dépendances
            DependencyGraph graph = buildDependencyGraph(raw_structure);
            
            // Classer les endpoints par type
            classifyEndpoints(raw_structure);
            
            log.info("Analysis complete: {} independent endpoints", 
                graph.getIndependentEndpoints().size());
            
            return raw_structure;
        });
    }
    
    private DependencyGraph buildDependencyGraph(ApiStructure structure) {
        DependencyGraph graph = new DependencyGraph();
        
        structure.getEndpoints().forEach((endpoint_id, endpoint) -> {
            // Si l'endpoint a des paramètres path (ex: /todos/{id})
            // Il dépend probablement d'un endpoint LIST
            if (endpoint.hasPathParameters() && 
                (endpoint.getType() == EndpointType.READ_DETAIL || 
                 endpoint.getType() == EndpointType.UPDATE ||
                 endpoint.getType() == EndpointType.DELETE)) {
                
                // Chercher l'endpoint LIST correspondant
                String list_endpoint_id = findListEndpoint(endpoint, structure);
                if (list_endpoint_id != null) {
                    graph.addDependency(endpoint_id, list_endpoint_id);
                }
            }
        });
        
        return graph;
    }
    
    private String findListEndpoint(Endpoint detail_endpoint, ApiStructure structure) {
        String base_path = detail_endpoint.getPath().replaceAll("/\\{[^}]+\\}", "");
        
        return structure.getEndpoints().values().stream()
            .filter(ep -> ep.getType() == EndpointType.LIST)
            .filter(ep -> ep.getPath().equals(base_path))
            .map(Endpoint::getOperation_id)
            .findFirst()
            .orElse(null);
    }
    
    private void classifyEndpoints(ApiStructure structure) {
        // Déjà fait lors du parsing Swagger, rien à faire ici
        log.debug("Endpoint classification: {}", 
            structure.getEndpoints().values().stream()
                .collect(Collectors.groupingBy(Endpoint::getType, Collectors.counting())));
    }
}