package com.network.projet.ussd.domain.model.automaton;

import com.network.projet.ussd.domain.enums.ValidationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule {
    private ValidationType type;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private Double min;
    private Double max;
    private Boolean optional;
}