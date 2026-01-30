package com.network.projet.ussd.service.validation;

import org.springframework.stereotype.Service;

import com.network.projet.ussd.domain.enums.ValidationErrorCode;
import com.network.projet.ussd.domain.enums.ValidationType;

import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@Service
public class ValidationService {

    // Regex patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]{2,50}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+237)?[26]\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    /**
     * Valide une entrée utilisateur selon le type de validation spécifié
     */
    public Mono<ValidationResult> validate(String input, ValidationType validationType) {
        return Mono.fromCallable(() -> {
            if (input == null || input.trim().isEmpty()) {
                return ValidationResult.failure(ValidationErrorCode.REQUIRED_FIELD.getMessage());
            }

            String trimmedInput = input.trim();

            return switch (validationType) {
                case NAME -> validateName(trimmedInput);
                case PHONE -> validatePhone(trimmedInput);
                case EMAIL -> validateEmail(trimmedInput);
                case DECIMAL -> validateDecimal(trimmedInput);
                case NUMERIC -> validateNumeric(trimmedInput);
                case ALPHANUMERIC -> validateAlphanumeric(trimmedInput);
                case TEXT -> validateText(trimmedInput);
                case CUSTOM -> validateEmail(trimmedInput);
            };
        });
    }

    /**
     * Valide avec longueur min/max
     */
    public Mono<ValidationResult> validate(String input, ValidationType validationType, 
                                          Integer minLength, Integer maxLength) {
        return validate(input, validationType)
            .map(result -> {
                if (!result.isValid()) {
                    return result;
                }
                
                if (minLength != null && input.length() < minLength) {
                    return ValidationResult.failure(
                        ValidationErrorCode.MIN_LENGTH.getMessage() + " (" + minLength + " caractères)"
                    );
                }
                
                if (maxLength != null && input.length() > maxLength) {
                    return ValidationResult.failure(
                        ValidationErrorCode.MAX_LENGTH.getMessage() + " (" + maxLength + " caractères)"
                    );
                }
                
                return result;
            });
    }

    private ValidationResult validateName(String input) {
        if (NAME_PATTERN.matcher(input).matches()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(ValidationErrorCode.INVALID_NAME.getMessage());
    }

    private ValidationResult validatePhone(String input) {
        if (PHONE_PATTERN.matcher(input).matches()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(ValidationErrorCode.INVALID_PHONE.getMessage());
    }

    private ValidationResult validateEmail(String input) {
        if (EMAIL_PATTERN.matcher(input).matches()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(ValidationErrorCode.INVALID_EMAIL.getMessage());
    }

    private ValidationResult validateDecimal(String input) {
        if (DECIMAL_PATTERN.matcher(input).matches()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(ValidationErrorCode.INVALID_DECIMAL.getMessage());
    }

    private ValidationResult validateNumeric(String input) {
        if (NUMERIC_PATTERN.matcher(input).matches()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(ValidationErrorCode.INVALID_NUMERIC.getMessage());
    }

    private ValidationResult validateAlphanumeric(String input) {
        if (ALPHANUMERIC_PATTERN.matcher(input).matches()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure("Seuls les caractères alphanumériques sont autorisés");
    }

    private ValidationResult validateText(String input) {
        // Pour TEXT, on accepte presque tout sauf les caractères spéciaux dangereux
        if (input.matches(".*[<>\"'%;()&+].*")) {
            return ValidationResult.failure("Caractères non autorisés détectés");
        }
        return ValidationResult.success();
    }
}