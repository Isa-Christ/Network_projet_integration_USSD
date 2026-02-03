package com.network.projet.ussd.domain.model.aigeneration;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Rapport de validation d'une configuration générée.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
public class ValidationReport {
    private boolean is_valid;
    private List<String> errors;
    private List<String> warnings;
    private List<ValidationCheck> checks;
    
    public ValidationReport() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.is_valid = true;
    }
    
    /**
     * Ajoute une vérification au rapport.
     */
    public void addCheck(String check_name, boolean passed) {
        ValidationCheck validation_check = new ValidationCheck(check_name, passed);
        checks.add(validation_check);
        if (!passed) {
            this.is_valid = false;
        }
    }
    
    /**
     * Ajoute une erreur critique.
     */
    public void addError(String error) {
        errors.add(error);
        this.is_valid = false;
    }
    
    /**
     * Ajoute un avertissement (n'invalide pas).
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    /**
     * Classe interne représentant une vérification.
     */
    @Data
    @AllArgsConstructor
    public static class ValidationCheck {
        private String check_name;
        private boolean passed;
    }
}