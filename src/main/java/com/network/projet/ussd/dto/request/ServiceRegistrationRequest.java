package com.network.projet.ussd.dto.request;

import lombok.Data;

@Data
public class ServiceRegistrationRequest {
    private String jsonConfig; // Full automaton JSON
}