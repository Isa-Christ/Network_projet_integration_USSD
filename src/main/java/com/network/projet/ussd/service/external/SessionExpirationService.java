package com.network.projet.ussd.service.external;

import com.network.projet.ussd.repository.UssdSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SessionExpirationService - Service de nettoyage automatique des sessions
 * expirées
 * 
 * Rôle: Cronjob pour nettoyer périodiquement les sessions USSD inactives
 * Appelle: UssdSessionRepository
 * 
 * Responsabilités:
 * - Désactivation automatique des sessions expirées
 * - Suppression des anciennes sessions (si nécessaire)
 * - Monitoring et logging du nettoyage
 * 
 * @author Network Projet Team
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionExpirationService {

	private final UssdSessionRepository sessionRepository;

	@Value("${ussd.session.timeout-minutes:1}")
	private long sessionTimeoutMinutes;

	@Value("${ussd.session.cleanup-rate-ms:60000}")
	private long cleanupRateMs;

	@Value("${ussd.session.hard-delete-after-days:7}")
	private long hardDeleteAfterDays;

	// ========== MÉTHODE PRINCIPALE ==========

	/**
 * Nettoie les sessions expirées (désactive les sessions inactives)
 * Exécuté automatiquement toutes les 60 secondes par défaut
 * 
 * CORRECTIONS:
 * - Cherche les sessions ACTIVES (isActive=true) expirées, pas les inactives
 * 
 * @return Mono<Void> Complété quand le nettoyage est terminé
 */
@Scheduled(fixedRateString = "${ussd.session.cleanup-rate-ms:60000}")
public Mono<Void> cleanupExpiredSessions() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
    
    log.debug("Starting session cleanup: threshold={}, timeout={}min", 
        threshold, sessionTimeoutMinutes);

    AtomicInteger cleanedCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    return sessionRepository.findByIsActiveTrueAndUpdatedAtBefore(threshold)
        .flatMap(session -> {
            log.debug("Expiring session: id={}, phone={}, updatedAt={}", 
                session.getId(), session.getPhoneNumber(), session.getUpdatedAt());
            
            session.setIsActive(false);
            
            return sessionRepository.save(session)
                .doOnSuccess(s -> cleanedCount.incrementAndGet())
                .doOnError(e -> {
                    errorCount.incrementAndGet();
                    log.error("Failed to expire session: id={}", session.getId(), e);
                })
                .onErrorResume(e -> Mono.empty());
        })
        .then()  // Flux -> Mono<Void>
        .doOnSuccess(v -> {
            if (cleanedCount.get() > 0) {
                log.info("Session cleanup completed: {} sessions expired, {} errors", 
                    cleanedCount.get(), errorCount.get());
            } else {
                log.trace("Session cleanup completed: no expired sessions found");
            }
        })
        .doOnError(e -> log.error("Critical error during session cleanup", e))
        .onErrorResume(e -> Mono.empty());
}

	// ========== MÉTHODES SUPPLÉMENTAIRES ==========

	/**
 * Supprime définitivement les sessions très anciennes (hard delete)
 * Exécuté une fois par jour par défaut
 * 
 * CORRECTION:
 * - Utilise doOnSuccess() pour incrémenter le compteur AVANT la suppression
 * - delete() retourne déjà Mono<Void>
 * 
 * @return Mono<Void> Complété quand la suppression est terminée
 */
@Scheduled(cron = "${ussd.session.hard-delete-cron:0 0 2 * * *}")
public Mono<Void> hardDeleteOldSessions() {

    LocalDateTime threshold = LocalDateTime.now().minusDays(hardDeleteAfterDays);

    log.info(
        "Starting hard delete of old sessions: threshold={}, age={} days",
        threshold, hardDeleteAfterDays
    );

    AtomicInteger deletedCount = new AtomicInteger(0);

    return sessionRepository.findByIsActiveFalseAndUpdatedAtBefore(threshold)
        .flatMap(session ->
            sessionRepository.delete(session)
                .doOnSuccess(v -> deletedCount.incrementAndGet())
                .onErrorResume(e -> {
                    log.error("Failed to delete session: id={}", session.getId(), e);
                    return Mono.empty();
                })
        )
        .then()
        .doOnSuccess(v -> {
            if (deletedCount.get() > 0) {
                log.info(
                    "Hard delete completed: {} old sessions permanently deleted",
                    deletedCount.get()
                );
            } else {
                log.debug("Hard delete completed: no old sessions to delete");
            }
        })
        .doOnError(e -> log.error("Critical error during hard delete", e))
        .onErrorResume(e -> Mono.empty());
}

	/**
	 * Nettoie manuellement les sessions d'un utilisateur spécifique
	 * Utile pour les opérations admin ou de support
	 * 
	 * @param phoneNumber Numéro de téléphone de l'utilisateur
	 * @return Mono<Integer> Nombre de sessions nettoyées
	 */
	public Mono<Integer> cleanupUserSessions(String phoneNumber) {
		log.info("Manually cleaning sessions for user: phone={}", phoneNumber);

		AtomicInteger count = new AtomicInteger(0);

		return sessionRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
				.flatMap(session -> {
					session.setIsActive(false);
					return sessionRepository.save(session)
							.doOnSuccess(s -> count.incrementAndGet());
				})
				.then(Mono.fromCallable(count::get))
				.doOnSuccess(total -> log.info("Cleaned {} sessions for phone: {}", total, phoneNumber))
				.doOnError(e -> log.error("Error cleaning sessions for phone: {}", phoneNumber, e));
	}

	/**
	 * Récupère les statistiques de sessions pour monitoring
	 * 
	 * @return Mono<SessionStats> Statistiques actuelles
	 */
	public Mono<SessionStats> getSessionStats() {
		return Mono.zip(
				sessionRepository.countByIsActiveTrue(),
				sessionRepository.countByIsActiveFalse(),
				sessionRepository.count()).map(
						tuple -> SessionStats.builder()
								.activeSessions(tuple.getT1())
								.inactiveSessions(tuple.getT2())
								.totalSessions(tuple.getT3())
								.sessionTimeoutMinutes(sessionTimeoutMinutes)
								.build())
				.doOnSuccess(stats -> log.debug("Session stats: {}", stats));
	}

	/**
	 * Réactive une session expirée (utile pour le support client)
	 * 
	 * @param sessionId ID de la session à réactiver
	 * @return Mono<Boolean> true si réactivé avec succès
	 */
	public Mono<Boolean> reactivateSession(String sessionId) {
		log.info("Attempting to reactivate session: {}", sessionId);

		return sessionRepository.findBySessionId(sessionId)
				.flatMap(session -> {
					if (session.getIsActive()) {
						log.warn("Session already active: {}", sessionId);
						return Mono.just(false);
					}

					session.setIsActive(true);
					session.setUpdatedAt(LocalDateTime.now());

					return sessionRepository.save(session)
							.map(s -> true)
							.doOnSuccess(v -> log.info("Session reactivated: {}", sessionId));
				})
				.defaultIfEmpty(false)
				.doOnError(e -> log.error("Failed to reactivate session: {}", sessionId, e));
	}

	// ========== CLASSES INTERNES ==========

	/**
	 * Statistiques des sessions pour monitoring
	 */
	@lombok.Data
	@lombok.Builder
	public static class SessionStats {
		private Long activeSessions;
		private Long inactiveSessions;
		private Long totalSessions;
		private Long sessionTimeoutMinutes;

		@Override
		public String toString() {
			return String.format("SessionStats{active=%d, inactive=%d, total=%d, timeout=%dmin}",
					activeSessions, inactiveSessions, totalSessions, sessionTimeoutMinutes);
		}
	}

	// ========== MÉTHODES UTILITAIRES ==========

	/**
	 * Calcule le temps restant avant expiration d'une session
	 * 
	 * @param lastActivity Dernière activité de la session
	 * @return Duration Temps restant (peut être négatif si déjà expiré)
	 */
	public Duration getTimeUntilExpiration(LocalDateTime lastActivity) {
		LocalDateTime expirationTime = lastActivity.plusMinutes(sessionTimeoutMinutes);
		return Duration.between(LocalDateTime.now(), expirationTime);
	}

	/**
	 * Vérifie si une session est expirée
	 * 
	 * @param lastActivity Dernière activité de la session
	 * @return boolean true si la session est expirée
	 */
	public boolean isSessionExpired(LocalDateTime lastActivity) {
		return getTimeUntilExpiration(lastActivity).isNegative();
	}
}