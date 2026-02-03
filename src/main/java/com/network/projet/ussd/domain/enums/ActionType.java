package com.network.projet.ussd.domain.enums;

public enum ActionType {
    API_CALL,
    STORE_DATA,
    SEND_SMS,
    STORAGE_SAVE,    // Sauvegarder dans generic_storage
    STORAGE_LOAD,    // Charger depuis generic_storage
    STORAGE_APPEND,  // Ajouter à un tableau dans storage
    STORAGE_DELETE,   // Supprimer une clé
    NONE

}