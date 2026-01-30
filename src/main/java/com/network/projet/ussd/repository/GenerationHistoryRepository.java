package com.network.projet.ussd.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository pour l'historique des générations.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Repository
public interface GenerationHistoryRepository extends ReactiveCrudRepository<GenerationHistoryEntity, UUID> {
    
    /**
     * Trouve l'historique par config_id.
     */
    @Query("SELECT * FROM generation_history WHERE config_id = :config_id ORDER BY created_at DESC")
    Flux<GenerationHistoryEntity> findByConfigIdOrderByCreatedAtDesc(UUID config_id);
    
    /**
     * Trouve l'historique par action.
     */
    Flux<GenerationHistoryEntity> findByAction(String action);
    
    /**
     * Insère un nouvel historique manuellement.
     */
    @Modifying
    @Query("INSERT INTO generation_history (config_id, admin_user, action, processing_time_ms, error_message, created_at) " +
           "VALUES (:config_id, :admin_user, :action, :processing_time_ms, :error_message, :created_at)")
    Mono<Integer> insertHistory(
        @Param("config_id") UUID config_id,
        @Param("admin_user") String admin_user,
        @Param("action") String action,
        @Param("processing_time_ms") Long processing_time_ms,
        @Param("error_message") String error_message,
        @Param("created_at") LocalDateTime created_at
    );
}