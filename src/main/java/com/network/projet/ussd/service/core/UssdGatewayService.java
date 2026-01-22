package com.network.projet.ussd.service.core;

import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.domain.model.automaton.State;
import com.network.projet.ussd.dto.request.UssdRequest;
import com.network.projet.ussd.exception.InvalidStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * UssdGatewayService - Main USSD flow orchestrator
 * 
 * @author Network Projet Team
 * @since 2026-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UssdGatewayService {

    private final ServiceRegistry serviceRegistry;
    private final SessionManager sessionManager;
    private final AutomatonEngine automatonEngine;

    private static final String MAIN_MENU_CODE = "*500#";

    /**
     * Processes a USSD request
     */
    public Mono<StateResult> processRequest(UssdRequest request, UssdSession session) {
        log.info("Processing USSD Request: code={}, phone={}, sessionId={}, currentState={}",
                request.getServiceCode(), request.getPhoneNumber(),
                session != null ? session.getSessionId() : "NEW",
                session != null ? session.getCurrentStateId() : "N/A");

        if (session == null || isSessionExpired(session)) {
            return initializeNewSession(request);
        } else {
            return continueExistingSession(request, session);
        }
    }

    /**
     * Initializes a new session
     */
    private Mono<StateResult> initializeNewSession(UssdRequest request) {
        if (MAIN_MENU_CODE.equals(request.getServiceCode())) {
            return initializeMainMenuSession(request);
        } else {
            return initializeServiceSession(request);
        }
    }

    /**
     * Continues an existing session
     */
    private Mono<StateResult> continueExistingSession(UssdRequest request, UssdSession session) {
        String input = request.getText();

        log.debug("Continuing session: sessionId={}, currentState={}, input='{}'",
                session.getSessionId(), session.getCurrentStateId(), input);

        return serviceRegistry.loadAutomaton(session.getServiceCode())
            .flatMap(automaton -> automatonEngine.processInput(session, automaton, input))
            .flatMap(result -> {
                log.debug("State execution result: nextState={}, continue={}",
                        result.getNextStateId(), result.isContinueSession());

                // CORRECTION: Mettre à jour et sauvegarder la session AVANT de retourner
                session.setCurrentStateId(result.getNextStateId());
                session.setUpdatedAt(LocalDateTime.now());
                
                if (!result.isContinueSession()) {
                    return sessionManager.terminateSession(session.getId())
                        .thenReturn(result);
                }
                
                return sessionManager.updateSession(session)
                    .doOnSuccess(updated -> log.debug("Session persisted: nextState={}",
                            updated.getCurrentStateId()))
                    .thenReturn(result);
            });
    }

    /**
     * Initializes session for main menu
     */
    private Mono<StateResult> initializeMainMenuSession(UssdRequest request) {
        return serviceRegistry.getAllActiveServices()
            .collectList()
            .flatMap(services -> {
                StringBuilder menu = new StringBuilder("🌐 USSD Gateway\nChoisissez un service:\n\n");
                for (int i = 0; i < services.size(); i++) {
                    menu.append(String.format("%d. %s\n", i + 1, services.get(i).getName()));
                }
                menu.append("\n0. Quitter");

                return sessionManager.getOrCreateSession(
                    request.getSessionId(),
                    request.getPhoneNumber(),
                    MAIN_MENU_CODE
                ).flatMap(session -> {
                    session.setCurrentStateId("MAIN_MENU");
                    return sessionManager.updateSession(session)
                        .thenReturn(StateResult.builder()
                            .message(menu.toString())
                            .continueSession(true)
                            .nextStateId("MAIN_MENU")
                            .build());
                });
            });
    }

    /**
     * Initializes session for a service
     */
    private Mono<StateResult> initializeServiceSession(UssdRequest request) {
        return serviceRegistry.getServiceByShortCode(request.getServiceCode())
            .flatMap(service -> serviceRegistry.loadAutomaton(service.getCode())
                .flatMap(automaton -> {
                    State initialState = findInitialState(automaton);
                    
                    return sessionManager.getOrCreateSession(
                        request.getSessionId(),
                        request.getPhoneNumber(),
                        service.getCode()
                    ).flatMap(session -> {
                        session.setCurrentStateId(initialState.getId());
                        
                        return sessionManager.updateSession(session)
                            .thenReturn(StateResult.builder()
                                .message(initialState.getMessage())
                                .continueSession(true)
                                .nextStateId(initialState.getId())
                                .build());
                    });
                }))
            .onErrorResume(error -> {
                log.error("Error initializing service session: {}", error.getMessage());
                return Mono.just(StateResult.builder()
                    .message("UNKNOWN APPLICATION")
                    .continueSession(false)
                    .build());
            });
    }

    /**
     * Finds the initial state in the automaton
     */
    private State findInitialState(AutomatonDefinition automaton) {
        return automaton.getStates().stream()
            .filter(state -> Boolean.TRUE.equals(state.getIsInitial()))
            .findFirst()
            .orElseThrow(() -> new InvalidStateException("No initial state found in automaton"));
    }

    /**
     * Checks if session has expired
     */
    private boolean isSessionExpired(UssdSession session) {
        return session.getExpiresAt() != null && 
               session.getExpiresAt().isBefore(LocalDateTime.now());
    }
}
