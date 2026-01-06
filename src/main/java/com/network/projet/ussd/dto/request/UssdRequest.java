package com.network.projet.ussd.dto.request;

import lombok.Data;

@Data
public class UssdRequest {
    private String sessionId;
    private String serviceCode; // *500# or *500*1#
    private String phoneNumber;
    private String text; // User input
}