package com.network.projet.ussd.domain.enums;

/**
 * Niveau de complexité d'une proposition de workflow.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
public enum ProposalComplexity {
    SIMPLE("Simple", "Fonctionnalités basiques uniquement", 3, 8),
    STANDARD("Standard", "CRUD complet recommandé", 8, 15),
    ADVANCED("Avancé", "Toutes fonctionnalités avec extras", 15, 25);
    
    private final String display_name;
    private final String description;
    private final int min_states;
    private final int max_states;
    
    ProposalComplexity(String display_name, String description, 
                       int min_states, int max_states) {
        this.display_name = display_name;
        this.description = description;
        this.min_states = min_states;
        this.max_states = max_states;
    }
    
    public String getDisplayName() {
        return display_name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getMinStates() {
        return min_states;
    }
    
    public int getMaxStates() {
        return max_states;
    }
    
    /**
     * Détermine la complexité basée sur le nombre d'états.
     */
    public static ProposalComplexity fromStateCount(int state_count) {
        if (state_count <= SIMPLE.max_states) {
            return SIMPLE;
        } else if (state_count <= STANDARD.max_states) {
            return STANDARD;
        } else {
            return ADVANCED;
        }
    }
}