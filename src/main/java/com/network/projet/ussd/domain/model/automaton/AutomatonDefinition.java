package com.network.projet.ussd.domain.model.automaton;

import com.network.projet.ussd.exception.InvalidStateException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomatonDefinition {
    private String serviceCode;
    private String serviceName;
    private String version;
    private String shortCode;
    private String description;
    private ApiConfig apiConfig;
    private SessionConfig sessionConfig;
    private List<State> states;
    
    public State getInitialState() {
        return states.stream()
            .filter(s -> s.getIsInitial() != null && s.getIsInitial())
            .findFirst()
            .orElse(states.get(0));
    }
    
    public State getStateById(String id) {
        return states.stream()
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new InvalidStateException("State not found: " + id));
    }
}