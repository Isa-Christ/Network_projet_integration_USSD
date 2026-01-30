package com.network.projet.ussd.util;

/**
 * Générateur d'IDs séquentiels pour états.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
public class StateIdGenerator {
    
    private int current_id = 1;
    
    /**
     * Génère le prochain ID.
     */
    public String next() {
        return String.valueOf(current_id++);
    }
    
    /**
     * Réinitialise le compteur.
     */
    public void reset() {
        current_id = 1;
    }
    
    /**
     * Réserve un bloc d'IDs.
     */
    public void reserve(int count) {
        current_id += count;
    }
}