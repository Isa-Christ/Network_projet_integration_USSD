package com.network.projet.ussd.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository pour les configurations générées.
 * 
 * @author Network Project team
 * @since 2025-01-25
 */
@Repository
public interface GeneratedConfigRepository extends ReactiveCrudRepository<GeneratedConfigEntity, UUID> {
    
    /**
     * Trouve les configs par statut.
     */
    Flux<GeneratedConfigEntity> findByStatus(String status);
    
    /**
     * Trouve les configs par type de source.
     */
    @Query("SELECT * FROM generated_configs WHERE source_type = :source_type")
    Flux<GeneratedConfigEntity> findBySourceType(String source_type);
    
    /**
     * Trouve une config par URL source (pour cache).
     */
    @Query("SELECT * FROM generated_configs WHERE source_url = :source_url")
    Mono<GeneratedConfigEntity> findBySource_url(String source_url);
    
    /**
     * Compte les configs par statut.
     */
    @Query("SELECT COUNT(*) FROM generated_configs WHERE status = :status")
    Mono<Long> countByStatus(String status);
}