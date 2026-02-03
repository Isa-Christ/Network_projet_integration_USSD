package com.network.projet.ussd.domain.model.aigeneration;

import lombok.Data;

import java.util.*;

/**
 * Graphe de dépendances entre endpoints.
 * Permet de détecter quels endpoints doivent être appelés avant d'autres.
 * 
 * Exemple: GET /todos/{id} dépend de GET /todos (pour obtenir la liste des IDs)
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
public class DependencyGraph {
    private Map<String, Set<String>> dependencies;  // endpoint_id -> set of prerequisite endpoint_ids
    private Map<String, Set<String>> dependents;    // endpoint_id -> set of dependent endpoint_ids
    
    public DependencyGraph() {
        this.dependencies = new HashMap<>();
        this.dependents = new HashMap<>();
    }
    
    /**
     * Ajoute une dépendance : endpoint_id dépend de prerequisite_id.
     * 
     * @param endpoint_id L'endpoint dépendant
     * @param prerequisite_id L'endpoint prérequis
     */
    public void addDependency(String endpoint_id, String prerequisite_id) {
        dependencies.computeIfAbsent(endpoint_id, k -> new HashSet<>())
            .add(prerequisite_id);
        
        dependents.computeIfAbsent(prerequisite_id, k -> new HashSet<>())
            .add(endpoint_id);
    }
    
    /**
     * Retourne les prérequis d'un endpoint.
     */
    public Set<String> getPrerequisites(String endpoint_id) {
        return dependencies.getOrDefault(endpoint_id, Collections.emptySet());
    }
    
    /**
     * Retourne les endpoints qui dépendent de celui-ci.
     */
    public Set<String> getDependents(String endpoint_id) {
        return dependents.getOrDefault(endpoint_id, Collections.emptySet());
    }
    
    /**
     * Vérifie si un endpoint a des prérequis.
     */
    public boolean hasDependencies(String endpoint_id) {
        return dependencies.containsKey(endpoint_id) && 
               !dependencies.get(endpoint_id).isEmpty();
    }
    
    /**
     * Retourne un tri topologique des endpoints.
     * Les endpoints sans dépendances viennent en premier.
     */
    public List<String> getTopologicalOrder() {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> temp_mark = new HashSet<>();
        
        Set<String> all_nodes = new HashSet<>(dependencies.keySet());
        all_nodes.addAll(dependents.keySet());
        
        for (String node : all_nodes) {
            if (!visited.contains(node)) {
                topologicalSortUtil(node, visited, temp_mark, result);
            }
        }
        
        Collections.reverse(result);
        return result;
    }
    
    private void topologicalSortUtil(String node, Set<String> visited, 
                                     Set<String> temp_mark, List<String> result) {
        if (temp_mark.contains(node)) {
            // Cycle détecté, on ignore
            return;
        }
        
        if (visited.contains(node)) {
            return;
        }
        
        temp_mark.add(node);
        
        Set<String> prereqs = dependencies.getOrDefault(node, Collections.emptySet());
        for (String prereq : prereqs) {
            topologicalSortUtil(prereq, visited, temp_mark, result);
        }
        
        temp_mark.remove(node);
        visited.add(node);
        result.add(node);
    }
    
    /**
     * Retourne les endpoints sans dépendances (points d'entrée potentiels).
     */
    public Set<String> getIndependentEndpoints() {
        Set<String> independent = new HashSet<>();
        Set<String> all_endpoints = new HashSet<>(dependencies.keySet());
        all_endpoints.addAll(dependents.keySet());
        
        for (String endpoint : all_endpoints) {
            if (!hasDependencies(endpoint)) {
                independent.add(endpoint);
            }
        }
        
        return independent;
    }
}