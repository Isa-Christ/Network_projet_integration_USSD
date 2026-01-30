package com.network.projet.ussd.util;

/**
 * Utilitaire pour tronquer les messages USSD à 182 caractères.
 * 
 * @author Netxork Project Team 
 * @since 2025-01-25
 */
public class MessageTruncator {
    
    private static final int MAX_LENGTH = 182;
    
    /**
     * Tronque un message si trop long.
     */
    public static String truncate(String message) {
        if (message == null) {
            return "";
        }
        
        if (message.length() <= MAX_LENGTH) {
            return message;
        }
        
        return message.substring(0, MAX_LENGTH - 3) + "...";
    }
    
    /**
     * Vérifie si un message est trop long.
     */
    public static boolean isTooLong(String message) {
        return message != null && message.length() > MAX_LENGTH;
    }
    
    /**
     * Retourne le nombre de caractères dépassés.
     */
    public static int getExcessLength(String message) {
        if (message == null || message.length() <= MAX_LENGTH) {
            return 0;
        }
        return message.length() - MAX_LENGTH;
    }
}