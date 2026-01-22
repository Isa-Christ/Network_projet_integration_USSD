package com.network.projet.ussd.service.validation;

import lombok.Getter;

@Getter
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
    
    public boolean getIsValid() {
        return valid;
    }
    
    public boolean isValid() {
        return valid;
    }
}