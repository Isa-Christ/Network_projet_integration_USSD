package com.network.projet.ussd.service.core;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO représentant le résultat de l'exécution d'un état USSD.
 */
@Data
@Builder
public class StateResult {

    /**
     * Message USSD à afficher à l'utilisateur.
     */
    private String message;

    /**
     * Indique si la session USSD doit continuer.
     * false => fin de session.
     */
    private boolean continueSession;

    /**
     * Identifiant du prochain état à exécuter.
     * Null si la session se termine.
     */
    private String nextStateId;

    /**
     * Données de session mises à jour après l'exécution de l'état.
     */
    private Map<String, Object> updatedSessionData;
}
