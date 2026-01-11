package service;

import repository.UssdSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for automatic session expiration
 * @author Ton Nom
 * @date 06/01/2026
 */
@Service
public class SessionExpirationService {

    private final UssdSessionRepository session_repository;

    public SessionExpirationService(UssdSessionRepository session_repository) {
        this.session_repository = session_repository;
    }

    /**
     * Clean expired sessions every 60 seconds
     */
    @Scheduled(fixedRate = 60000) // Toutes les 60 secondes
    public void cleanExpiredSessions() {
        session_repository.findExpiredSessions()
                .flatMap(session -> {
                    session.setIsActive(false);
                    return session_repository.save(session);
                })
                .subscribe(); // Lance le processus réactif
    }
}