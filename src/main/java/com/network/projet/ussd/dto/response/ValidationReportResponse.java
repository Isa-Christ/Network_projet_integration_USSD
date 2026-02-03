package com.network.projet.ussd.dto.response;

import com.network.projet.ussd.domain.model.aigeneration.ValidationReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour rapport de validation.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReportResponse {
    private boolean is_valid;
    private ValidationReport report;
    private String summary;
    
    /**
     * Crée une réponse à partir d'un rapport de validation.
     */
    public static ValidationReportResponse fromReport(ValidationReport report) {
        String summary = generateSummary(report);
        return ValidationReportResponse.builder()
            .is_valid(report.is_valid())
            .report(report)
            .summary(summary)
            .build();
    }
    
    /**
     * Génère un résumé du rapport de validation.
     */
    private static String generateSummary(ValidationReport report) {
        if (report.is_valid()) {
            return "Configuration valide. " + report.getChecks().size() + " vérifications passées.";
        } else {
            return "Configuration invalide. " + report.getErrors().size() + " erreurs, " + 
                   report.getWarnings().size() + " avertissements.";
        }
    }
}