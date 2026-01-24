package com.network.projet.ussd.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UssdRequest {
    private String sessionId;
    private String ussdCode; // *500# or *500*1# (le code USSD composé)
    private String phoneNumber;
    private String text; // User input
}