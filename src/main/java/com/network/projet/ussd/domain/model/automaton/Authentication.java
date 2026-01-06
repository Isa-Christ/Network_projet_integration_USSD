package com.network.projet.ussd.domain.model.automaton;

import com.network.projet.ussd.domain.enums.AuthenticationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Authentication {
    private AuthenticationType type;
    private Map<String, String> credentials;
}