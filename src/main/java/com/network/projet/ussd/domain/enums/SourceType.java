package com.network.projet.ussd.domain.enums;

/**
 * Type de source API pour génération automatique.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
public enum SourceType {
    SWAGGER_URL("URL Swagger/OpenAPI"),
    SWAGGER_FILE("Fichier Swagger uploadé"),
    POSTMAN("Collection Postman");

    private final String description;

    SourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}