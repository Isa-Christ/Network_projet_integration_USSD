package com.network.projet.ussd.util;

import com.network.projet.ussd.domain.model.automaton.State;

import java.util.*;

/**
 * Algorithmes de graphes pour validation de configurations.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
public class GraphAlgorithms {
    
    /**
     * Trouve tous les états accessibles depuis l'état initial.
     */
    public static Set<String> findReachableStates(List<State> states) {
        Set<String> reachable = new HashSet<>();
        
        State initial_state = states.stream()
            .filter(State -> State.getIsInitial() == true)
            .findFirst()
            .orElse(null);
        
        if (initial_state == null) {
            return reachable;
        }
        
        Map<String, State> state_map = buildStateMap(states);
        Queue<String> queue = new LinkedList<>();
        queue.add(initial_state.getId());
        reachable.add(initial_state.getId());
        
        while (!queue.isEmpty()) {
            String current_id = queue.poll();
            State current_state = state_map.get(current_id);
            
            if (current_state != null && current_state.getTransitions() != null) {
                current_state.getTransitions().forEach(transition -> {
                    String next_id = transition.getNextState();
                    if (next_id != null && !reachable.contains(next_id)) {
                        reachable.add(next_id);
                        queue.add(next_id);
                    }
                });
            }
        }
        
        return reachable;
    }
    
    /**
     * Vérifie si tous les chemins mènent à un état FINAL.
     */
    public static boolean checkAllPathsTerminate(List<State> states) {
        Map<String, State> state_map = buildStateMap(states);
        Set<String> visited = new HashSet<>();
        
        State initial_state = states.stream()
            .filter(State -> State.getIsInitial() == true)
            .findFirst()
            .orElse(null);
        
        if (initial_state == null) {
            return false;
        }
        
        return canReachFinal(initial_state.getId(), state_map, visited);
    }
    
    private static boolean canReachFinal(String state_id, 
                                        Map<String, State> state_map,
                                        Set<String> visited) {
        if (visited.contains(state_id)) {
            return false;  // Cycle sans FINAL
        }
        
        State state = state_map.get(state_id);
        if (state == null) {
            return false;
        }
        
        if ("FINAL".equals(state.getType().name())) {
            return true;
        }
        
        visited.add(state_id);
        
        if (state.getTransitions() == null || state.getTransitions().isEmpty()) {
            return false;  // Deadlock
        }
        
        for (var transition : state.getTransitions()) {
            if (canReachFinal(transition.getNextState(), state_map, visited)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static Map<String, State> buildStateMap(List<State> states) {
        Map<String, State> map = new HashMap<>();
        states.forEach(state -> map.put(state.getId(), state));
        return map;
    }
}