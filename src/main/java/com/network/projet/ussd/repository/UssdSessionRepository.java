package com.network.projet.ussd.repository;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.network.projet.ussd.domain.model.UssdSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * UssdSessionRepository - Repository R2DBC pour la persistence des sessions USSD
 * 
 * @author Network Projet Team
 */
@Repository
public interface UssdSessionRepository extends ReactiveCrudRepository<UssdSession, Long> {

    // ========== RECHERCHE PAR SESSION ID ==========
    
    /**
     * Trouve une session par son sessionId (UUID du client)
     * 
     * @param sessionId UUID de la session
     * @return Mono<UssdSession> Session ou empty
     */
    Mono<UssdSession> findBySessionId(String sessionId);

    // ========== RECHERCHE PAR TÉLÉPHONE ==========

    /**
     * Trouve la session active d'un utilisateur
     */
    Mono<UssdSession> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    /**
     * Trouve toutes les sessions actives d'un utilisateur
     */
    Flux<UssdSession> findAllByPhoneNumberAndIsActiveTrue(String phoneNumber);

    /**
     * Trouve toutes les sessions d'un utilisateur
     */
    Flux<UssdSession> findByPhoneNumber(String phoneNumber);

    /**
     * Trouve une session active pour un code de service spécifique
     */
    Mono<UssdSession> findByPhoneNumberAndServiceCodeAndIsActiveTrue(String phoneNumber, String serviceCode);

    // ========== RECHERCHE PAR SERVICE ==========

    /**
     * Trouve toutes les sessions d'un service code
     */
    Flux<UssdSession> findByServiceCodeAndIsActiveTrue(String serviceCode);

    /**
     * Compte les sessions actives d'un service
     */
    Mono<Long> countByServiceCodeAndIsActiveTrue(String serviceCode);

    // ========== RECHERCHE PAR ÉTAT ==========

    /**
     * Trouve toutes les sessions actives
     */
    Flux<UssdSession> findByIsActiveTrue();

    /**
     * Trouve toutes les sessions inactives
     */
    Flux<UssdSession> findByIsActiveFalse();

    /**
     * Compte les sessions actives
     */
    Mono<Long> countByIsActiveTrue();

    /**
     * Compte les sessions inactives
     */
    Mono<Long> countByIsActiveFalse();

    // ========== RECHERCHE PAR EXPIRATION ==========

    /**
     * Trouve les sessions actives expirées
     * Utilisé par le cleanup scheduler dans SessionManager
     */
    Flux<UssdSession> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime threshold);

    /**
     * Trouve les sessions actives dont updatedAt est avant le seuil
     * Utilisé par SessionExpirationService.cleanupExpiredSessions()
     */
    Flux<UssdSession> findByIsActiveTrueAndUpdatedAtBefore(LocalDateTime cutoffTime);

    /**
     * Trouve les sessions inactives avant une date
     * Utilisé par SessionExpirationService.hardDeleteOldSessions()
     */
    Flux<UssdSession> findByIsActiveFalseAndUpdatedAtBefore(LocalDateTime cutoffTime);

    /**
     * Trouve les sessions créées avant une date
     */
    Flux<UssdSession> findByCreatedAtBefore(LocalDateTime cutoffTime);

    // ========== RECHERCHE PAR ÉTAT DE L'AUTOMATE ==========

    /**
     * Trouve les sessions actives dans un état spécifique
     */
    Flux<UssdSession> findByCurrentStateIdAndIsActiveTrue(String stateId);

    /**
     * Compte les sessions dans un état spécifique
     */
    Mono<Long> countByCurrentStateIdAndIsActiveTrue(String stateId);

    // ========== REQUÊTES PERSONNALISÉES ==========

    /**
     * Supprime définitivement les sessions créées avant une date
     */
    @Modifying
    @Query("DELETE FROM ussd_sessions WHERE created_at < :cutoffDate")
    Mono<Integer> deleteOldSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Supprime les sessions inactives créées avant une date
     */
    @Modifying
    @Query("DELETE FROM ussd_sessions WHERE is_active = false AND created_at < :cutoffDate")
    Mono<Integer> deleteInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Désactive en masse les sessions expirées
     */
    @Modifying
    @Query("UPDATE ussd_sessions SET is_active = false, updated_at = CURRENT_TIMESTAMP " +
           "WHERE is_active = true AND expires_at < :threshold")
    Mono<Integer> bulkExpireSessions(@Param("threshold") LocalDateTime threshold);

    /**
     * Trouve les sessions avec le plus d'inactivité (top N)
     */
    @Query("SELECT * FROM ussd_sessions " +
           "WHERE is_active = true " +
           "ORDER BY expires_at ASC " +
           "LIMIT :limit")
    Flux<UssdSession> findMostInactiveSessions(@Param("limit") int limit);

    /**
     * Trouve les sessions les plus récentes (top N)
     */
    @Query("SELECT * FROM ussd_sessions " +
           "ORDER BY created_at DESC " +
           "LIMIT :limit")
    Flux<UssdSession> findRecentSessions(@Param("limit") int limit);

    /**
     * Trouve les sessions d'un utilisateur pour une période donnée
     */
    @Query("SELECT * FROM ussd_sessions " +
           "WHERE phone_number = :phoneNumber " +
           "AND created_at BETWEEN :startDate AND :endDate " +
           "ORDER BY created_at DESC")
    Flux<UssdSession> findUserSessionsInPeriod(
        @Param("phoneNumber") String phoneNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}