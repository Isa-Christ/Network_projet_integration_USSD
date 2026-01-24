package com.network.projet.ussd.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.model.UssdService;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.exception.ServiceNotFoundException;
import com.network.projet.ussd.repository.UssdServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistry {
    
    private final UssdServiceRepository serviceRepository;
    private final ObjectMapper objectMapper;
    
    private final Map<String, AutomatonDefinition> automatonCache = new ConcurrentHashMap<>();
    
    /**
     * Load automaton for a service (with caching)
     * @param code Technical service code (e.g. "todo-manager")
     */
    public Mono<AutomatonDefinition> loadAutomaton(String code) {
        if (automatonCache.containsKey(code)) {
            return Mono.just(automatonCache.get(code));
        }
        
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException("Service not found: " + code)))
            .map(service -> {
                try {
                    AutomatonDefinition automaton = objectMapper.readValue(
                        service.getJsonConfig(), 
                        AutomatonDefinition.class
                    );
                    automatonCache.put(code, automaton);
                    return automaton;
                } catch (Exception e) {
                    log.error("Failed to parse automaton for service: {}", code, e);
                    throw new RuntimeException("Invalid automaton JSON", e);
                }
            });
    }
    
    /**
     * Get service by USSD short code
     * @param shortCode USSD code (e.g. "*500*1#")
     */
    public Mono<UssdService> getServiceByShortCode(String shortCode) {
        return serviceRepository.findByShortCode(shortCode)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException("Unknown shortCode: " + shortCode)));
    }
    
    /**
     * List all active services
     */
    public Flux<UssdService> getAllActiveServices() {
        return serviceRepository.findByIsActiveTrue();
    }
    
    /**
     * Invalidate cache
     * @param code Technical service code
     */
    public void invalidateCache(String code) {
        automatonCache.remove(code);
        log.info("Cache invalidated for service: {}", code);
    }
}