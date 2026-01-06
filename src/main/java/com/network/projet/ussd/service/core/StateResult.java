package com.network.projet.ussd.service.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StateResult {
    private String message;
    private String nextStateId;
    private boolean shouldContinue;
    private boolean shouldTerminate;
}