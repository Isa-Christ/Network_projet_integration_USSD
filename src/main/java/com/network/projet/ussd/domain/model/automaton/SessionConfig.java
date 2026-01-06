package com.network.projet.ussd.domain.model.automaton;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionConfig {
    private Integer timeoutSeconds;
    private Integer maxInactivitySeconds;
}