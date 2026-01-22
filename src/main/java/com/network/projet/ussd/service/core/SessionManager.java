package com.network.projet.ussd.service.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.domain.model.automaton.State;
import com.network.projet.ussd.repository.UssdSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SessionManager - Gestionnaire des sessions USSD
 * 
 * @author Network Projet Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManager {

    private final UssdSessionRepository sessionRepository;
    private final ServiceRegistry serviceRegistry;
    private final ObjectMapper objectMapper;

    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Récupère une session existante ou en crée une nouvelle
     */
    public Mono<UssdSession> getOrCreateSession(String sessionId, String phoneNumber, String serviceCode) {
        log.debug("Get or create session: sessionId={}, phone={}, serviceCode={}",
                sessionId, phoneNumber, serviceCode);

        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionRepository.findBySessionId(sessionId)
                    .flatMap(session -> {
                        if (!session.isActive() || session.isExpired()) {
                            log.info("Session {} is inactive/expired, creating new", sessionId);
                            return createNewSession(sessionId, phoneNumber, serviceCode);
                        }
                        return updateSessionExpiration(session);
                    })
                    .switchIfEmpty(createNewSession(sessionId, phoneNumber, serviceCode));
        }

        return sessionRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
                .flatMap(existingSession -> {
                    if (existingSession.isExpired()) {
                        return expireAndCreateNew(existingSession, phoneNumber, serviceCode);
                    }
                    return updateSessionExpiration(existingSession);
                })
                .switchIfEmpty(createNewSession(sessionId, phoneNumber, serviceCode));
    }

    /**
     * Met à jour une session existante
     */
    public Mono<UssdSession> updateSession(UssdSession session) {
        log.debug("Updating session: {}", session.getSessionId());

        session.preUpdate();
        return sessionRepository.save(session)
                .doOnSuccess(s -> log.debug("Session updated: {}", s.getSessionId()))
                .doOnError(e -> log.error("Failed to update session: {}", session.getSessionId(), e));
    }

    /**
     * Stocke une donnée dans la session
     */
    public Mono<Void> storeSessionData(String sessionId, String key, Object value) {
        log.debug("Storing session data: sessionId={}, key={}", sessionId, key);

        return sessionRepository.findBySessionId(sessionId)
                .flatMap(session -> {
                    try {
                        Map<String, Object> data = parseSessionData(session.getSessionData());
                        data.put(key, value);

                        session.setSessionData(objectMapper.writeValueAsString(data));
                        session.preUpdate();

                        return sessionRepository.save(session);
                    } catch (Exception e) {
                        log.error("Failed to store data for session: {}", sessionId, e);
                        return Mono.error(new RuntimeException("Failed to store session data", e));
                    }
                })
                .then()
                .doOnSuccess(v -> log.debug("Session data stored"))
                .doOnError(e -> log.error("Error storing session data", e));
    }

    /**
     * Récupère toutes les données collectées d'une session
     */
    public Mono<Map<String, Object>> getSessionData(String sessionId) {
        log.debug("Getting session data: sessionId={}", sessionId);

        return sessionRepository.findBySessionId(sessionId)
                .map(session -> parseSessionData(session.getSessionData()))
                .defaultIfEmpty(new HashMap<>())
                .doOnError(e -> log.error("Error retrieving session data", e));
    }

    /**
     * Termine une session
     */
    public Mono<Void> endSession(String sessionId) {
        log.info("Ending session: {}", sessionId);

        return sessionRepository.findBySessionId(sessionId)
                .flatMap(session -> {
                    session.terminate();
                    return sessionRepository.save(session);
                })
                .then()
                .doOnSuccess(v -> log.info("Session ended: {}", sessionId))
                .doOnError(e -> log.error("Error ending session: {}", sessionId, e));
    }

    /**
     * Termine une session par ID technique
     */
    public Mono<Void> terminateSession(Long id) {
        log.info("Terminating session by id: {}", id);

        return sessionRepository.findById(id)
                .flatMap(session -> {
                    session.terminate();
                    return sessionRepository.save(session);
                })
                .then()
                .doOnSuccess(v -> log.info("Session terminated: {}", id));
    }

    /**
     * Crée une nouvelle session
     */
    private Mono<UssdSession> createNewSession(String sessionId, String phoneNumber, String serviceCode) {
        log.info("Creating new session: sessionId={}, phone={}, serviceCode={}",
                sessionId, phoneNumber, serviceCode);

        return serviceRegistry.getServiceByShortCode(serviceCode)
                .flatMap(service -> serviceRegistry.loadAutomaton(service.getCode())
                        .map(automaton -> findInitialState(automaton))
                        .map(initialState -> {
                            UssdSession session = UssdSession.builder()
                                    .sessionId(sessionId) // UTILISER LE sessionId FOURNI
                                    .phoneNumber(phoneNumber)
                                    .serviceCode(service.getCode())
                                    .currentStateId(initialState.getId())
                                    .sessionData("{}")
                                    .isActive(true)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .expiresAt(LocalDateTime.now().plus(SESSION_TIMEOUT))
                                    .build();

                            session.prePersist();
                            return session;
                        }))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Service not found for code: {}, creating session with default state", serviceCode);
                    UssdSession session = UssdSession.builder()
                            .sessionId(sessionId) // UTILISER LE sessionId FOURNI
                            .phoneNumber(phoneNumber)
                            .serviceCode(serviceCode)
                            .currentStateId("1")
                            .sessionData("{}")
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plus(SESSION_TIMEOUT))
                            .build();
                    session.prePersist();
                    return Mono.just(session);
                }))
                .flatMap(sessionRepository::save)
                .doOnSuccess(s -> log.info("Session created: sessionId={}, phone={}",
                        s.getSessionId(), s.getPhoneNumber()))
                .doOnError(e -> log.error("Failed to create session for phone: {}", phoneNumber, e));
    }

    /**
     * Met à jour l'expiration de la session
     */
    private Mono<UssdSession> updateSessionExpiration(UssdSession session) {
        log.trace("Updating session expiration: {}", session.getSessionId());

        session.touch(SESSION_TIMEOUT);
        return sessionRepository.save(session);
    }

    /**
     * Expire une session et en crée une nouvelle
     */
    private Mono<UssdSession> expireAndCreateNew(UssdSession oldSession, String phoneNumber, String serviceCode) {
        log.info("Expiring session {} and creating new one", oldSession.getSessionId());

        oldSession.terminate();
        return sessionRepository.save(oldSession)
                .then(createNewSession(oldSession.getSessionId(), phoneNumber, serviceCode));
    }

    /**
     * Trouve l'état initial dans l'automate
     */
    private State findInitialState(com.network.projet.ussd.domain.model.automaton.AutomatonDefinition automaton) {
        return automaton.getStates().stream()
                .filter(state -> Boolean.TRUE.equals(state.getIsInitial()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No initial state found in automaton"));
    }

    /**
     * Tâche planifiée: Nettoie les sessions expirées toutes les minutes
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        log.debug("Running expired sessions cleanup (threshold: {})", now);

        sessionRepository.findByIsActiveTrueAndExpiresAtBefore(now)
                .flatMap(session -> {
                    log.info("Cleaning up expired session: sessionId={}, phone={}, expiresAt={}",
                            session.getSessionId(), session.getPhoneNumber(), session.getExpiresAt());

                    session.terminate();
                    return sessionRepository.save(session);
                })
                .subscribe(
                        session -> log.debug("Session expired: {}", session.getSessionId()),
                        error -> log.error("Error during session cleanup", error),
                        () -> log.trace("Session cleanup completed"));
    }

    /**
     * Parse les données de session JSON en Map
     */
    private Map<String, Object> parseSessionData(String sessionDataJson) {
        try {
            if (sessionDataJson == null || sessionDataJson.isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(
                    sessionDataJson,
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("Failed to parse session data: {}", sessionDataJson, e);
            return new HashMap<>();
        }
    }

    /**
     * Récupère une session par son sessionId
     * Utilisé par UssdController ligne 223
     * 
     * @param sessionId ID de la session
     * @return Mono<UssdSession> Session trouvée ou vide
     */
    public Mono<UssdSession> getSession(String sessionId) {
        log.debug("Getting session by sessionId: {}", sessionId);

        return sessionRepository.findBySessionId(sessionId)
                .doOnSuccess(session -> {
                    if (session != null) {
                        log.debug("Session found: sessionId={}, phone={}, currentState={}",
                                session.getSessionId(), session.getPhoneNumber(), session.getCurrentStateId());
                    } else {
                        log.debug("Session not found: {}", sessionId);
                    }
                })
                .doOnError(e -> log.error("Error retrieving session: {}", sessionId, e));
    }
}