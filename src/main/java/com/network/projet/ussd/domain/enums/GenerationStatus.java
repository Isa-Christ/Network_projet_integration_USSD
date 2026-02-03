package com.network.projet.ussd.domain.enums;

/**
 * Statut du processus de génération.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
public enum GenerationStatus {
    ANALYZING("Analyse de l'API en cours"),
    GENERATING("Génération des workflows"),
    COMPLETED("Génération terminée"),
    FAILED("Échec de la génération");

    private final String description;

    GenerationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}