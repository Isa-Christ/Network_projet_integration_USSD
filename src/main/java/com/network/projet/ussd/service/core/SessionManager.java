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
    public Mono<UssdSession> getOrCreateSession(String sessionId, String phoneNumber, String ussdCode) {
        log.debug("Get or create session: sessionId={}, phone={}, ussdCode={}",
                sessionId, phoneNumber, ussdCode);

        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionRepository.findBySessionId(sessionId)
                    .flatMap(session -> {
                        if (!session.isActive() || session.isExpired()) {
                            log.info("Session {} is inactive/expired, creating new", sessionId);
                            return createNewSession(sessionId, phoneNumber, ussdCode);
                        }
                        return updateSessionExpiration(session);
                    })
                    .switchIfEmpty(createNewSession(sessionId, phoneNumber, ussdCode));
        }

        return sessionRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
                .flatMap(existingSession -> {
                    if (existingSession.isExpired()) {
                        return expireAndCreateNew(existingSession, phoneNumber, ussdCode);
                    }
                    return updateSessionExpiration(existingSession);
                })
                .switchIfEmpty(createNewSession(sessionId, phoneNumber, ussdCode));
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
        log.debug(">>> STORE START: sessionId={}, key={}, value={}", sessionId, key, value);

        return sessionRepository.findBySessionId(sessionId)
                .switchIfEmpty(Mono.error(new RuntimeException("Session not found: " + sessionId)))
                .flatMap(session -> {
                    try {
                        Map<String, Object> data = parseSessionData(session.getSessionData());

                        log.debug(">>> BEFORE store - existing keys: {}", data.keySet());
                        data.put(key, value);
                        log.debug(">>> AFTER put - new keys: {}", data.keySet());
                        log.debug(">>> Value for '{}': {}", key, data.get(key));

                        String jsonData = objectMapper.writeValueAsString(data);
                        session.setSessionData(jsonData);
                        session.preUpdate();

                        log.debug(">>> Saving to DB...");
                        return sessionRepository.save(session)
                                .doOnSuccess(saved -> {
                                    log.debug(">>> SAVE SUCCESS");
                                    log.debug(">>> Saved sessionData: {}", saved.getSessionData());
                                })
                                .then();

                    } catch (Exception e) {
                        log.error("Failed to store data for session: {}", sessionId, e);
                        return Mono.error(new RuntimeException("Failed to store session data", e));
                    }
                })
                .doOnSuccess(v -> log.debug(">>> STORE END: Successfully stored {}={}", key, value))
                .doOnError(e -> log.error(">>> STORE ERROR for key: {}", key, e));
    }

    /**
     * Store multiple key-value pairs in ONE database operation (no race condition)
     */
    public Mono<Void> storeBatchData(String sessionId, Map<String, Object> dataToStore) {
        if (dataToStore == null || dataToStore.isEmpty()) {
            return Mono.empty();
        }

        log.debug(">>> BATCH STORE START: sessionId={}, keys={}", sessionId, dataToStore.keySet());

        return sessionRepository.findBySessionId(sessionId)
                .switchIfEmpty(Mono.error(new RuntimeException("Session not found: " + sessionId)))
                .flatMap(session -> {
                    try {
                        Map<String, Object> existingData = parseSessionData(session.getSessionData());

                        log.debug(">>> BEFORE batch - existing keys: {}", existingData.keySet());
                        existingData.putAll(dataToStore); // Ajoute TOUTES les nouvelles données d'un coup
                        log.debug(">>> AFTER batch - new keys: {}", existingData.keySet());

                        String jsonData = objectMapper.writeValueAsString(existingData);
                        session.setSessionData(jsonData);
                        session.preUpdate();

                        return sessionRepository.save(session)
                                .doOnSuccess(saved -> log.debug(">>> BATCH SAVED: {}", saved.getSessionData()))
                                .then();

                    } catch (Exception e) {
                        log.error("Failed to batch store data", e);
                        return Mono.error(new RuntimeException("Failed to batch store", e));
                    }
                })
                .doOnSuccess(v -> log.debug(">>> BATCH COMPLETE: {} keys stored", dataToStore.size()));
    }

    /**
     * Récupère toutes les données collectées d'une session
     * + injecte automatiquement le phoneNumber
     */
    public Mono<Map<String, Object>> getSessionData(String sessionId) {
        log.debug("Getting session data: sessionId={}", sessionId);

        return sessionRepository.findBySessionId(sessionId)
                .map(session -> {
                    Map<String, Object> data = parseSessionData(session.getSessionData());

                    // ✅ Injection automatique du numéro de téléphone
                    data.put("phoneNumber", session.getPhoneNumber());

                    return data;
                })
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
    private Mono<UssdSession> createNewSession(String sessionId, String phoneNumber, String ussdCode) {
        log.info("Creating new session: sessionId={}, phone={}, ussdCode={}",
                sessionId, phoneNumber, ussdCode);

        return serviceRegistry.getServiceByShortCode(ussdCode)
                .flatMap(service -> serviceRegistry.loadAutomaton(service.getCode())
                        .map(automaton -> findInitialState(automaton))
                        .map(initialState -> {
                            UssdSession session = UssdSession.builder()
                                    .sessionId(sessionId)
                                    .phoneNumber(phoneNumber)
                                    .serviceCode(service.getCode()) // CODE TECHNIQUE
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
                    log.warn("Service not found for ussdCode: {}, creating session with default state", ussdCode);
                    UssdSession session = UssdSession.builder()
                            .sessionId(sessionId)
                            .phoneNumber(phoneNumber)
                            .serviceCode(ussdCode) // Fallback
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
                .doOnSuccess(s -> log.info("Session created: sessionId={}, phone={}, serviceCode={}",
                        s.getSessionId(), s.getPhoneNumber(), s.getServiceCode()))
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
    private Mono<UssdSession> expireAndCreateNew(UssdSession oldSession, String phoneNumber, String ussdCode) {
        log.info("Expiring session {} and creating new one", oldSession.getSessionId());

        oldSession.terminate();
        return sessionRepository.save(oldSession)
                .then(createNewSession(oldSession.getSessionId(), phoneNumber, ussdCode));
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