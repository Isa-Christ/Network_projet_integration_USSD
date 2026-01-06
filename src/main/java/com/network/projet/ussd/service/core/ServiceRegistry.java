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
     */
    public Mono<AutomatonDefinition> loadAutomaton(String serviceCode) {
        if (automatonCache.containsKey(serviceCode)) {
            return Mono.just(automatonCache.get(serviceCode));
        }
        
        return serviceRepository.findByCode(serviceCode)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException("Service not found: " + serviceCode)))
            .map(service -> {
                try {
                    AutomatonDefinition automaton = objectMapper.readValue(
                        service.getJsonConfig(), 
                        AutomatonDefinition.class
                    );
                    automatonCache.put(serviceCode, automaton);
                    return automaton;
                } catch (Exception e) {
                    log.error("Failed to parse automaton for service: {}", serviceCode, e);
                    throw new RuntimeException("Invalid automaton JSON", e);
                }
            });
    }
    
    /**
     * Get service by short code
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
     */
    public void invalidateCache(String serviceCode) {
        automatonCache.remove(serviceCode);
        log.info("Cache invalidated for service: {}", serviceCode);
    }
}