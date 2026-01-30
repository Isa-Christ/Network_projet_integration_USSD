package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.util.MessageTruncator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Optimiseur de configuration (troncature messages, etc.).
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Service
@Slf4j
public class ConfigOptimizer {
    
    /**
     * Optimise une configuration (tronque messages trop longs).
     */
    public Mono<AutomatonDefinition> optimize(AutomatonDefinition config) {
        return Mono.fromCallable(() -> {
            log.info("Optimizing config");
            
            if (config.getStates() != null) {
                config.getStates().forEach(state -> {
                    if (state.getMessage() != null && MessageTruncator.isTooLong(state.getMessage())) {
                        String original = state.getMessage();
                        String truncated = MessageTruncator.truncate(original);
                        state.setMessage(truncated);
                        log.debug("Truncated message for state {}: {} -> {} chars", 
                            state.getName(), original.length(), truncated.length());
                    }
                });
            }
            
            return config;
        });
    }
}