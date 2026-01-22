package service;

import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.repository.UssdSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UssdSessionService using Mockito
 * @author Ton Nom
 * @date 10/01/2026
 */
@ExtendWith(MockitoExtension.class)  // Active Mockito
@DisplayName("UssdSessionService Unit Tests")
class UssdSessionServiceTest {

    @Mock  // Crée un "faux" repository
    private UssdSessionRepository session_repository;

    @InjectMocks  // Injecte les mocks dans le service
    private UssdSessionService session_service;

    private UssdSession test_session;

    @BeforeEach
    void setUp() {
        test_session = new UssdSession();
        test_session.setId(UUID.randomUUID());
        test_session.setPhoneNumber("237699123456");
        test_session.setSessionId("session_123");
        test_session.setCurrentState("MENU_PRINCIPAL");
    }

    // ============================================
    // TESTS CRÉATION DE SESSION
    // ============================================

    @Test
    @DisplayName("Should create new session when none exists")
    void shouldCreateNewSessionWhenNoneExists() {
        // GIVEN - Aucune session existante
        when(session_repository.findByPhoneNumberAndIsActiveTrue(anyString()))
                .thenReturn(Mono.empty());

        when(session_repository.save(any(UssdSession.class)))
                .thenReturn(Mono.just(test_session));

        // WHEN
        Mono<UssdSession> result = session_service.getOrCreateSession("237699123456");

        // THEN
        StepVerifier.create(result)
                .expectNextMatches(session ->
                        session.getPhoneNumber().equals("237699123456") &&
                                session.isActive()
                )
                .verifyComplete();

        // Vérifier que save a bien été appelé
        verify(session_repository, times(1)).save(any(UssdSession.class));
    }

    @Test
    @DisplayName("Should return existing session when found")
    void shouldReturnExistingSessionWhenFound() {
        // GIVEN - Session existante
        when(session_repository.findByPhoneNumberAndIsActiveTrue("237699123456"))
                .thenReturn(Mono.just(test_session));

        // WHEN
        Mono<UssdSession> result = session_service.getOrCreateSession("237699123456");

        // THEN
        StepVerifier.create(result)
                .expectNext(test_session)
                .verifyComplete();

        // Vérifier que save n'a PAS été appelé
        verify(session_repository, never()).save(any(UssdSession.class));
    }

    // ============================================
    // TESTS MISE À JOUR DE SESSION
    // ============================================

    @Test
    @DisplayName("Should update session state")
    void shouldUpdateSessionState() {
        // GIVEN
        String new_state = "MENU_LIVRAISON";
        when(session_repository.save(any(UssdSession.class)))
                .thenReturn(Mono.just(test_session));

        // WHEN
        Mono<UssdSession> result = session_service.updateSessionState(test_session, new_state);

        // THEN
        StepVerifier.create(result)
                .expectNextMatches(session ->
                        session.getCurrentState().equals(new_state)
                )
                .verifyComplete();
    }

    // ============================================
    // TESTS CLÔTURE DE SESSION
    // ============================================

    @Test
    @DisplayName("Should close session by setting inactive")
    void shouldCloseSessionBySettingInactive() {
        // GIVEN
        when(session_repository.save(any(UssdSession.class)))
                .thenReturn(Mono.just(test_session));

        // WHEN
        Mono<Void> result = session_service.closeSession(test_session);

        // THEN
        StepVerifier.create(result)
                .verifyComplete();

        verify(session_repository, times(1)).save(argThat(session ->
                !session.isActive()
        ));
    }
}