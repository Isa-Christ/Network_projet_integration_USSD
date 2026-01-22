package com.network.projet.ussd.domain.enums;

public enum ValidationErrorCode {
    INVALID_NAME("Le nom doit contenir uniquement des lettres"),
    INVALID_PHONE("Numéro de téléphone invalide (format: +237XXXXXXXXX ou 6XXXXXXXX)"),
    INVALID_EMAIL("Format d'email invalide"),
    INVALID_DECIMAL("Veuillez entrer un nombre valide"),
    INVALID_TEXT("Texte invalide"),
    INVALID_NUMERIC("Veuillez entrer uniquement des chiffres"),
    REQUIRED_FIELD("Ce champ est obligatoire"),
    MIN_LENGTH("La longueur minimale n'est pas respectée"),
    MAX_LENGTH("La longueur maximale est dépassée");

    private final String message;

    ValidationErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}