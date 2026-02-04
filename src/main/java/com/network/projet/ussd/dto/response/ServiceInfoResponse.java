package com.network.projet.ussd.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceInfoResponse {
    private Long id;
    private String code;
    private String name;
    private String shortCode;
    private String apiBaseUrl;
    private String jsonConfig;
    private Boolean isActive;
    private LocalDateTime createdAt;
}