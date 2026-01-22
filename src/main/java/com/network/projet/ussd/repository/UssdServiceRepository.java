package com.network.projet.ussd.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import com.network.projet.ussd.domain.model.UssdService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Component
public interface UssdServiceRepository extends R2dbcRepository<UssdService, Long> {
    
    Mono<UssdService> findByCode(String code);
    
    Mono<UssdService> findByShortCode(String shortCode);
    
    Flux<UssdService> findByIsActiveTrue();
}