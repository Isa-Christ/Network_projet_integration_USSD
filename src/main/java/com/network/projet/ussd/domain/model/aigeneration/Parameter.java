package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un paramètre d'endpoint API.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {
    private String name;
    private String in_location;  // path, query, header, body
    private String type;          // string, integer, boolean, etc.
    private String format;        // email, date-time, etc.
    private boolean required;
    private String description;
    private Integer min_length;
    private Integer max_length;
    private Object default_value;
}