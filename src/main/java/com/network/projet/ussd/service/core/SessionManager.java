package com.network.projet.ussd.service.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.model.UssdSession;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManager {
    
    private final UssdSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    
    private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(30);
    
    /**
     * Get or create session for a phone number
     */
    public Mono<UssdSession> getOrCreateSession(String phoneNumber, Long serviceId, String initialStateId) {
        return sessionRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
            .flatMap(existingSession -> {
                if (isExpired(existingSession)) {
                    return expireAndCreateNew(phoneNumber, serviceId, initialStateId);
                }
                return touchSession(existingSession);
            })
            .switchIfEmpty(createNewSession(phoneNumber, serviceId, initialStateId));
    }
    
    /**
     * Create new session
     */
    private Mono<UssdSession> createNewSession(String phoneNumber, Long serviceId, String initialStateId) {
        UssdSession session = UssdSession.builder()
            .id(UUID.randomUUID().toString())
            .phoneNumber(phoneNumber)
            .serviceId(serviceId)
            .currentStateId(initialStateId)
            .collectedData("{}")
            .lastActivity(LocalDateTime.now())
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        return sessionRepository.save(session)
            .doOnSuccess(s -> log.info("New session created: {}", s.getId()));
    }
    
    /**
     * Update session state
     */
    public Mono<UssdSession> updateSessionState(UssdSession session, String newStateId) {
        session.setCurrentStateId(newStateId);
        session.setLastActivity(LocalDateTime.now());
        return sessionRepository.save(session);
    }
    
    /**
     * Store collected data
     */
    public Mono<UssdSession> storeData(UssdSession session, String key, Object value) {
        try {
            Map<String, Object> data = objectMapper.readValue(
                session.getCollectedData(), 
                new TypeReference<Map<String, Object>>() {}
            );
            data.put(key, value);
            session.setCollectedData(objectMapper.writeValueAsString(data));
            session.setLastActivity(LocalDateTime.now());
            return sessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to store data", e);
            return Mono.error(e);
        }
    }
    
    /**
     * Get collected data
     */
    public Map<String, Object> getCollectedData(UssdSession session) {
        try {
            return objectMapper.readValue(
                session.getCollectedData(),
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            log.error("Failed to parse collected data", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Touch session (update last activity)
     */
    private Mono<UssdSession> touchSession(UssdSession session) {
        session.setLastActivity(LocalDateTime.now());
        return sessionRepository.save(session);
    }
    
    /**
     * Terminate session
     */
    public Mono<Void> terminateSession(String sessionId) {
        return sessionRepository.findById(sessionId)
            .flatMap(session -> {
                session.setIsActive(false);
                return sessionRepository.save(session);
            })
            .then();
    }
    
    /**
     * Check if session is expired
     */
    private boolean isExpired(UssdSession session) {
        return Duration.between(session.getLastActivity(), LocalDateTime.now())
            .compareTo(SESSION_TIMEOUT) > 0;
    }
    
    /**
     * Expire old session and create new one
     */
    private Mono<UssdSession> expireAndCreateNew(String phoneNumber, Long serviceId, String initialStateId) {
        return sessionRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
            .flatMap(old -> {
                old.setIsActive(false);
                return sessionRepository.save(old);
            })
            .then(createNewSession(phoneNumber, serviceId, initialStateId));
    }
    
    /**
     * Scheduled job: Clean up expired sessions every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void cleanupExpiredSessions() {
        LocalDateTime threshold = LocalDateTime.now().minus(SESSION_TIMEOUT);
        
        sessionRepository.findByIsActiveTrueAndLastActivityBefore(threshold)
            .flatMap(session -> {
                session.setIsActive(false);
                return sessionRepository.save(session);
            })
            .subscribe(
                session -> log.debug("Session expired: {}", session.getId()),
                error -> log.error("Error cleaning sessions", error)
            );
    }
}