package com.ussdgateway.service.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.network.projet.ussd.domain.enums.ValidationType;
import com.network.projet.ussd.service.validation.ValidationResult;
import com.network.projet.ussd.service.validation.ValidationService;

import reactor.test.StepVerifier;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    void testValidName() {
        StepVerifier.create(validationService.validate("Jean Dupont", ValidationType.NAME))
            .expectNextMatches(ValidationResult::isValid)
            .verifyComplete();
    }

    @Test
    void testInvalidName() {
        StepVerifier.create(validationService.validate("Jean123", ValidationType.NAME))
            .expectNextMatches(result -> !result.isValid())
            .verifyComplete();
    }

    @Test
    void testValidPhone() {
        StepVerifier.create(validationService.validate("237612345678", ValidationType.PHONE))
            .expectNextMatches(ValidationResult::isValid)
            .verifyComplete();
        
        StepVerifier.create(validationService.validate("612345678", ValidationType.PHONE))
            .expectNextMatches(ValidationResult::isValid)
            .verifyComplete();
    }

    @Test
    void testInvalidPhone() {
        StepVerifier.create(validationService.validate("123456", ValidationType.PHONE))
            .expectNextMatches(result -> !result.isValid())
            .verifyComplete();
    }

    @Test
    void testValidEmail() {
        StepVerifier.create(validationService.validate("test@example.com", ValidationType.EMAIL))
            .expectNextMatches(ValidationResult::isValid)
            .verifyComplete();
    }

    @Test
    void testInvalidEmail() {
        StepVerifier.create(validationService.validate("invalid-email", ValidationType.EMAIL))
            .expectNextMatches(result -> !result.isValid())
            .verifyComplete();
    }

    @Test
    void testValidDecimal() {
        StepVerifier.create(validationService.validate("100.50", ValidationType.DECIMAL))
            .expectNextMatches(ValidationResult::isValid)
            .verifyComplete();
    }

    @Test
    void testMinMaxLength() {
        StepVerifier.create(validationService.validate("AB", ValidationType.TEXT, 3, 10))
            .expectNextMatches(result -> !result.isValid())
            .verifyComplete();
        
        StepVerifier.create(validationService.validate("ABC", ValidationType.TEXT, 3, 10))
            .expectNextMatches(ValidationResult::isValid)
            .verifyComplete();
    }

    @Test
    void testInvalidDecimal() {
        StepVerifier.create(validationService.validate("100,50", ValidationType.DECIMAL))
            .expectNextMatches(result -> !result.isValid())
            .verifyComplete();
    }
}