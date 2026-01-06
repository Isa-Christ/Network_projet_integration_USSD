package com.network.projet.ussd.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.model.UssdService;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.exception.ServiceNotFoundException;
import com.network.projet.ussd.repository.UssdServiceRepository;
import com.network.projet.ussd.service.core.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAdminService {
    
    private final UssdServiceRepository serviceRepository;
    private final ServiceRegistry serviceRegistry;
    private final ObjectMapper objectMapper;
    
    /**
     * Register a new USSD service
     */
    public Mono<UssdService> registerService(String jsonConfig) {
        log.info("Registering new USSD service");
        
        return Mono.fromCallable(() -> {
            // Parse and validate JSON
            AutomatonDefinition automaton = objectMapper.readValue(
                jsonConfig, 
                AutomatonDefinition.class
            );
            
            // Validate required fields
            if (automaton.getServiceCode() == null || automaton.getServiceCode().isEmpty()) {
                throw new IllegalArgumentException("Service code is required");
            }
            if (automaton.getServiceName() == null || automaton.getServiceName().isEmpty()) {
                throw new IllegalArgumentException("Service name is required");
            }
            if (automaton.getStates() == null || automaton.getStates().isEmpty()) {
                throw new IllegalArgumentException("At least one state is required");
            }
            
            // Build UssdService entity
            UssdService service = UssdService.builder()
                .code(automaton.getServiceCode())
                .name(automaton.getServiceName())
                .shortCode(automaton.getShortCode())
                .jsonConfig(jsonConfig)
                .apiBaseUrl(automaton.getApiConfig() != null 
                    ? automaton.getApiConfig().getBaseUrl() 
                    : null)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            return service;
        })
        .flatMap(service -> {
            // Check if service code already exists
            return serviceRepository.findByCode(service.getCode())
                .flatMap(existing -> Mono.<UssdService>error(
                    new IllegalArgumentException("Service code already exists: " + service.getCode())
                ))
                .switchIfEmpty(serviceRepository.save(service));
        })
        .doOnSuccess(service -> log.info("Service registered successfully: {}", service.getCode()))
        .doOnError(error -> log.error("Failed to register service", error));
    }
    
    /**
     * Get service by code
     */
    public Mono<UssdService> getServiceByCode(String code) {
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)));
    }
    
    /**
     * Get service by ID
     */
    public Mono<UssdService> getServiceById(Long id) {
        return serviceRepository.findById(id)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException("Service not found with id: " + id)));
    }
    
    /**
     * List all services
     */
    public Flux<UssdService> listAllServices() {
        return serviceRepository.findAll();
    }
    
    /**
     * List active services only
     */
    public Flux<UssdService> listActiveServices() {
        return serviceRepository.findByIsActiveTrue();
    }
    
    /**
     * Update service configuration
     */
    public Mono<UssdService> updateService(String code, String newJsonConfig) {
        log.info("Updating service: {}", code);
        
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)))
            .flatMap(existing -> Mono.fromCallable(() -> {
                // Validate new JSON
                AutomatonDefinition automaton = objectMapper.readValue(
                    newJsonConfig, 
                    AutomatonDefinition.class
                );
                
                // Ensure service code hasn't changed
                if (!automaton.getServiceCode().equals(code)) {
                    throw new IllegalArgumentException("Cannot change service code during update");
                }
                
                // Update fields
                existing.setName(automaton.getServiceName());
                existing.setShortCode(automaton.getShortCode());
                existing.setJsonConfig(newJsonConfig);
                existing.setApiBaseUrl(automaton.getApiConfig() != null 
                    ? automaton.getApiConfig().getBaseUrl() 
                    : null);
                existing.setUpdatedAt(LocalDateTime.now());
                
                return existing;
            }))
            .flatMap(serviceRepository::save)
            .doOnSuccess(service -> {
                serviceRegistry.invalidateCache(code);
                log.info("Service updated successfully: {}", code);
            })
            .doOnError(error -> log.error("Failed to update service: {}", code, error));
    }
    
    /**
     * Activate service
     */
    public Mono<UssdService> activateService(String code) {
        log.info("Activating service: {}", code);
        
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)))
            .flatMap(service -> {
                service.setIsActive(true);
                service.setUpdatedAt(LocalDateTime.now());
                return serviceRepository.save(service);
            })
            .doOnSuccess(service -> log.info("Service activated: {}", code));
    }
    
    /**
     * Deactivate service
     */
    public Mono<UssdService> deactivateService(String code) {
        log.info("Deactivating service: {}", code);
        
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)))
            .flatMap(service -> {
                service.setIsActive(false);
                service.setUpdatedAt(LocalDateTime.now());
                return serviceRepository.save(service);
            })
            .doOnSuccess(service -> {
                serviceRegistry.invalidateCache(code);
                log.info("Service deactivated: {}", code);
            });
    }
    
    /**
     * Toggle service status (activate/deactivate)
     */
    public Mono<UssdService> toggleServiceStatus(String code) {
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)))
            .flatMap(service -> {
                service.setIsActive(!service.getIsActive());
                service.setUpdatedAt(LocalDateTime.now());
                return serviceRepository.save(service);
            })
            .doOnSuccess(service -> {
                serviceRegistry.invalidateCache(code);
                log.info("Service status toggled: {} - Active: {}", code, service.getIsActive());
            });
    }
    
    /**
     * Delete service
     */
    public Mono<Void> deleteService(String code) {
        log.info("Deleting service: {}", code);
        
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)))
            .flatMap(service -> serviceRepository.delete(service))
            .doOnSuccess(v -> {
                serviceRegistry.invalidateCache(code);
                log.info("Service deleted: {}", code);
            })
            .doOnError(error -> log.error("Failed to delete service: {}", code, error));
    }
    
    /**
     * Check if service code is available
     */
    public Mono<Boolean> isServiceCodeAvailable(String code) {
        return serviceRepository.findByCode(code)
            .map(service -> false)
            .defaultIfEmpty(true);
    }
    
    /**
     * Check if short code is available
     */
    public Mono<Boolean> isShortCodeAvailable(String shortCode) {
        return serviceRepository.findByShortCode(shortCode)
            .map(service -> false)
            .defaultIfEmpty(true);
    }
    
    /**
     * Get service statistics
     */
    public Mono<ServiceStatistics> getServiceStatistics(String code) {
        return serviceRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new ServiceNotFoundException(code)))
            .map(service -> ServiceStatistics.builder()
                .serviceCode(service.getCode())
                .serviceName(service.getName())
                .isActive(service.getIsActive())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build());
    }
    
    /**
     * Inner class for service statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class ServiceStatistics {
        private String serviceCode;
        private String serviceName;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}