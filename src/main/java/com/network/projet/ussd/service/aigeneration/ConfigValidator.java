package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.enums.StateType;
import com.network.projet.ussd.domain.model.aigeneration.ValidationReport;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.domain.model.automaton.State;
import com.network.projet.ussd.util.GraphAlgorithms;
import com.network.projet.ussd.util.MessageTruncator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

/**
 * Validateur de configuration générée.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Service
@Slf4j
public class ConfigValidator {
    
    @Value("${ai.generator.validation.max-message-length:182}")
    private int max_message_length;
    
    @Value("${ai.generator.validation.max-menu-options:8}")
    private int max_menu_options;
    
    @Value("${ai.generator.validation.min-states:1}")
    private int min_states;
    
    /**
     * Valide une configuration USSD.
     */
    public Mono<ValidationReport> validate(AutomatonDefinition config) {
        return Mono.fromCallable(() -> {
            log.info("Validating USSD config: {}", config.getServiceCode());
            
            ValidationReport report = new ValidationReport();
            
            // Validation de base
            validateBasicStructure(config, report);
            
            // Validation des états
            validateStates(config, report);
            
            // Validation du graphe
            validateGraph(config, report);
            
            // Validation USSD spécifique
            validateUssdConstraints(config, report);
            
            log.info("Validation complete: valid={}, errors={}, warnings={}", 
                report.is_valid(), report.getErrors().size(), report.getWarnings().size());
            
            return report;
        });
    }
    
    private void validateBasicStructure(AutomatonDefinition config, ValidationReport report) {
        if (config.getServiceCode() == null || config.getServiceCode().isEmpty()) {
            report.addError("Service code manquant");
        }
        
        if (config.getServiceName() == null || config.getServiceName().isEmpty()) {
            report.addError("Service name manquant");
        }
        
        if (config.getStates() == null || config.getStates().isEmpty()) {
            report.addError("Aucun état défini");
        }
        
        if (config.getStates() != null && config.getStates().size() < min_states) {
            report.addWarning("Nombre d'états très faible: " + config.getStates().size());
        }
    }
    
    private void validateStates(AutomatonDefinition config, ValidationReport report) {
        if (config.getStates() == null) return;
        
        // Vérifier état initial
        long initial_count = config.getStates().stream()
            .filter( State -> State.getIsInitial() == true)
            .count();
        
        if (initial_count == 0) {
            report.addError("Aucun état initial défini");
        } else if (initial_count > 1) {
            report.addError("Plusieurs états initiaux définis");
        }
        
        // Vérifier au moins un état FINAL
        boolean has_final = config.getStates().stream()
            .anyMatch(s -> s.getType() == StateType.FINAL);
        
        if (!has_final) {
            report.addError("Aucun état FINAL défini");
        }
        
        // Vérifier IDs uniques
        Set<String> seen_ids = new HashSet<>();
        for (State state : config.getStates()) {
            if (state.getId() == null) {
                report.addError("État sans ID: " + state.getName());
            } else if (seen_ids.contains(state.getId())) {
                report.addError("ID d'état dupliqué: " + state.getId());
            } else {
                seen_ids.add(state.getId());
            }
        }
        
        // Vérifier transitions
        for (State state : config.getStates()) {
            if (state.getType() != StateType.FINAL && 
                (state.getTransitions() == null || state.getTransitions().isEmpty())) {
                report.addError("État non-final sans transitions: " + state.getName());
            }
        }
    }
    
    private void validateGraph(AutomatonDefinition config, ValidationReport report) {
        if (config.getStates() == null || config.getStates().isEmpty()) return;
        
        // Vérifier accessibilité
        Set<String> reachable = GraphAlgorithms.findReachableStates(config.getStates());
        Set<String> all_ids = new HashSet<>();
        config.getStates().forEach(s -> all_ids.add(s.getId()));
        
        all_ids.removeAll(reachable);
        if (!all_ids.isEmpty()) {
            report.addError("États inaccessibles: " + all_ids);
        }
        
        // Vérifier terminaison
        boolean terminates = GraphAlgorithms.checkAllPathsTerminate(config.getStates());
        if (!terminates) {
            report.addWarning("Certains chemins pourraient ne pas terminer");
        }
    }
    
    private void validateUssdConstraints(AutomatonDefinition config, ValidationReport report) {
        if (config.getStates() == null) return;
        
        for (State state : config.getStates()) {
            // Vérifier longueur message
            if (state.getMessage() != null && MessageTruncator.isTooLong(state.getMessage())) {
                int excess = MessageTruncator.getExcessLength(state.getMessage());
                report.addWarning(String.format(
                    "État '%s': message trop long de %d caractères (max %d)",
                    state.getName(), excess, max_message_length
                ));
            }
            
            // Vérifier nombre d'options menu
            if (state.getType() == StateType.MENU && 
                state.getTransitions() != null &&
                state.getTransitions().size() > max_menu_options) {
                report.addWarning(String.format(
                    "État '%s': trop d'options (%d, max %d recommandé)",
                    state.getName(), state.getTransitions().size(), max_menu_options
                ));
            }
        }
    }
}