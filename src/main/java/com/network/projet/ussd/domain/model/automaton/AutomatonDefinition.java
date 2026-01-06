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

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public ApiConfig getApiConfig() {
        return apiConfig;
    }

    public void setApiConfig(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }
}