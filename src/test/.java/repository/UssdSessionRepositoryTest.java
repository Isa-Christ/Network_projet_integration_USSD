package repository;

import domain.model.UssdSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

/**
 * Integration tests for UssdSessionRepository using Testcontainers
 * @author Ton Nom
 * @date 10/01/2026
 */
@DataR2dbcTest  // Configure un contexte de test R2DBC minimal
@Testcontainers  // Active Testcontainers
@DisplayName("UssdSessionRepository Integration Tests")
class UssdSessionRepositoryTest {

    // ============================================
    // CONFIGURATION TESTCONTAINERS
    // ============================================

    /**
     * Démarre un conteneur PostgreSQL pour les tests
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ussd_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Configure Spring pour utiliser le conteneur PostgreSQL
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    // ============================================
    // INJECTION DU REPOSITORY
    // ============================================

    @Autowired
    private UssdSessionRepository session_repository;

    // ============================================
    // SETUP
    // ============================================

    @BeforeEach
    void setUp() {
        // Nettoyer la base avant chaque test
        // session_repository.deleteAll().block();
    }

    // ============================================
    // TESTS CRUD DE BASE
    // ============================================

    @Test
    @DisplayName("Should save and retrieve session by ID")
    void shouldSaveAndRetrieveSessionById() {
        // GIVEN
        UssdSession session = new UssdSession();
        session.setPhoneNumber("237699123456");
        session.setSessionId("test_session_1");
        session.setCurrentState("MENU_PRINCIPAL");

        // WHEN & THEN (avec StepVerifier pour réactif)
        StepVerifier.create(
                        session_repository.save(session)  // Sauvegarde
                                .flatMap(saved -> session_repository.findById(saved.getId()))  // Puis récupère
                )
                .expectNextMatches(retrieved ->
                        retrieved.getPhoneNumber().equals("237699123456") &&
                                retrieved.getSessionId().equals("test_session_1")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find session by phone number when active")
    void shouldFindSessionByPhoneNumberWhenActive() {
        // GIVEN
        UssdSession session = new UssdSession();
        session.setPhoneNumber("237677998811");
        session.setSessionId("session_abc");
        session.setActive(true);

        // WHEN
        StepVerifier.create(
                        session_repository.save(session)
                                .then(session_repository.findByPhoneNumberAndIsActiveTrue("237677998811"))
                )
                // THEN
                .expectNextMatches(found ->
                        found.getPhoneNumber().equals("237677998811") &&
                                found.isActive()
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should not find session when inactive")
    void shouldNotFindSessionWhenInactive() {
        // GIVEN
        UssdSession session = new UssdSession();
        session.setPhoneNumber("237677998811");
        session.setActive(false);  // Inactive !

        // WHEN
        StepVerifier.create(
                        session_repository.save(session)
                                .then(session_repository.findByPhoneNumberAndIsActiveTrue("237677998811"))
                )
                // THEN
                .expectNextCount(0)  // Aucun résultat attendu
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find session by session ID")
    void shouldFindSessionBySessionId() {
        // GIVEN
        UssdSession session = new UssdSession();
        session.setPhoneNumber("237699123456");
        session.setSessionId("unique_session_xyz");

        // WHEN
        StepVerifier.create(
                        session_repository.save(session)
                                .then(session_repository.findBySessionId("unique_session_xyz"))
                )
                // THEN
                .expectNextMatches(found ->
                        found.getSessionId().equals("unique_session_xyz")
                )
                .verifyComplete();
    }

    // ============================================
    // TESTS REQUÊTE PERSONNALISÉE (EXPIRATION)
    // ============================================

    @Test
    @DisplayName("Should find expired sessions")
    void shouldFindExpiredSessions() {
        // GIVEN - Créer 2 sessions : 1 expirée, 1 valide
        UssdSession expired_session = new UssdSession();
        expired_session.setPhoneNumber("237699111111");
        expired_session.setSessionId("expired_1");
        expired_session.setExpiresAt(LocalDateTime.now().minusSeconds(60)); // Expirée il y a 1 minute
        expired_session.setActive(true);

        UssdSession active_session = new UssdSession();
        active_session.setPhoneNumber("237699222222");
        active_session.setSessionId("active_1");
        active_session.setExpiresAt(LocalDateTime.now().plusSeconds(60)); // Expire dans 1 minute
        active_session.setActive(true);

        // WHEN
        StepVerifier.create(
                        session_repository.save(expired_session)
                                .then(session_repository.save(active_session))
                                .thenMany(session_repository.findExpiredSessions())
                )
                // THEN - Ne doit trouver QUE la session expirée
                .expectNextMatches(session ->
                        session.getSessionId().equals("expired_1") &&
                                session.isExpired()
                )
                .expectNextCount(0)  // Pas d'autre résultat
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when no expired sessions exist")
    void shouldReturnEmptyWhenNoExpiredSessionsExist() {
        // GIVEN - Session valide uniquement
        UssdSession active_session = new UssdSession();
        active_session.setPhoneNumber("237699123456");
        active_session.setExpiresAt(LocalDateTime.now().plusSeconds(30));

        // WHEN
        StepVerifier.create(
                        session_repository.save(active_session)
                                .thenMany(session_repository.findExpiredSessions())
                )
                // THEN
                .expectNextCount(0)
                .verifyComplete();
    }

    // ============================================
    // TESTS DE SUPPRESSION
    // ============================================

    @Test
    @DisplayName("Should delete session by ID")
    void shouldDeleteSessionById() {
        // GIVEN
        UssdSession session = new UssdSession();
        session.setPhoneNumber("237699123456");

        // WHEN
        StepVerifier.create(
                        session_repository.save(session)
                                .flatMap(saved -> session_repository.deleteById(saved.getId())
                                        .then(session_repository.findById(saved.getId())))
                )
                // THEN
                .expectNextCount(0)  // La session ne doit plus exister
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete all sessions")
    void shouldDeleteAllSessions() {
        // GIVEN - Créer 3 sessions
        UssdSession session1 = new UssdSession();
        session1.setPhoneNumber("237699111111");

        UssdSession session2 = new UssdSession();
        session2.setPhoneNumber("237699222222");

        UssdSession session3 = new UssdSession();
        session3.setPhoneNumber("237699333333");

        // WHEN
        StepVerifier.create(
                        session_repository.save(session1)
                                .then(session_repository.save(session2))
                                .then(session_repository.save(session3))
                                .then(session_repository.deleteAll())
                                .thenMany(session_repository.findAll())
                )
                // THEN
                .expectNextCount(0)  // Aucune session ne doit rester
                .verifyComplete();
    }

    // ============================================
    // TESTS DES CAS LIMITES
    // ============================================

    @Test
    @DisplayName("Should handle multiple sessions for same phone number")
    void shouldHandleMultipleSessionsForSamePhoneNumber() {
        // GIVEN - 2 sessions pour le même numéro (une active, une inactive)
        UssdSession old_session = new UssdSession();
        old_session.setPhoneNumber("237699123456");
        old_session.setSessionId("old_session");
        old_session.setActive(false);

        UssdSession new_session = new UssdSession();
        new_session.setPhoneNumber("237699123456");
        new_session.setSessionId("new_session");
        new_session.setActive(true);

        // WHEN - Recherche de la session active
        StepVerifier.create(
                        session_repository.save(old_session)
                                .then(session_repository.save(new_session))
                                .then(session_repository.findByPhoneNumberAndIsActiveTrue("237699123456"))
                )
                // THEN - Ne doit retourner QUE la session active
                .expectNextMatches(found ->
                        found.getSessionId().equals("new_session") &&
                                found.isActive()
                )
                .verifyComplete();
    }
}