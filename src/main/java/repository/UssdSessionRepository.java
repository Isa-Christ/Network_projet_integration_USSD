package repository;

import domain.model.UssdSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Repository for USSD Session management (reactive)
 * @author Ton Nom
 * @date 06/01/2026
 */
public interface UssdSessionRepository extends R2dbcRepository<UssdSession, UUID> {

    /**
     * Find active session by phone number
     * @param phone_number User phone number
     * @return Mono of UssdSession
     */
    Mono<UssdSession> findByPhoneNumberAndIsActiveTrue(String phone_number);

    /**
     * Find session by session ID
     * @param session_id Session identifier
     * @return Mono of UssdSession
     */
    Mono<UssdSession> findBySessionId(String session_id);

    /**
     * Find all expired sessions
     * @return Flux of expired sessions
     */
    @Query("SELECT * FROM ussd_sessions WHERE expires_at < NOW() AND is_active = true")
    Flux<UssdSession> findExpiredSessions();
}