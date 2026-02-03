package com.network.projet.ussd.domain.model.aigeneration;

import com.network.projet.ussd.domain.enums.EndpointType;
import com.network.projet.ussd.domain.enums.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un endpoint API analysé depuis Swagger.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {
    private String operation_id;
    private String path;
    private HttpMethod method;
    private String summary;
    private String description;
    private EndpointType type;
    
    @Builder.Default
    private List<Parameter> parameters = new ArrayList<>();
    
    private boolean has_request_body;
    private String request_body_schema;
    
    private String response_schema;
    private boolean response_is_array;
    
    /**
     * Vérifie si l'endpoint a des paramètres obligatoires.
     */
    public boolean hasRequiredParameters() {
        return parameters.stream().anyMatch(Parameter::isRequired);
    }
    
    /**
     * Vérifie si l'endpoint a des paramètres path (ex: /todos/{id}).
     */
    public boolean hasPathParameters() {
        return parameters.stream()
            .anyMatch(p -> "path".equalsIgnoreCase(p.getIn_location()));
    }
}