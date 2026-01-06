package com.network.projet.ussd.domain.model.automaton;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {
    private String nextState;
    private String message;
    private Map<String, String> responseMapping;
}