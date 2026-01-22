package com.network.projet.ussd.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UssdRequest {
    private String sessionId;
    private String serviceCode; // *500# or *500*1#
    private String phoneNumber;
    private String text; // User input
}