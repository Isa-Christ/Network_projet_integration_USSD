package com.network.projet.ussd.domain.model.automaton;

import com.network.projet.ussd.domain.enums.ActionType;
import com.network.projet.ussd.domain.enums.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Action {
    private ActionType type;
    private HttpMethod method;
    private String endpoint;
    private Map<String, String> headers;
    private Map<String, Object> body;
    private ActionResult onSuccess;
    private ActionResult onError;
}