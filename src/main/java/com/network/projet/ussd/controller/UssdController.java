package com.network.projet.ussd.controller;

import org.springframework.beans.factory.annotation.Value;
import com.network.projet.ussd.dto.request.UssdRequest;
import com.network.projet.ussd.dto.response.UssdResponse;
import com.network.projet.ussd.exception.ServiceNotFoundException;
import com.network.projet.ussd.service.core.SessionManager;
import com.network.projet.ussd.service.core.UssdGatewayService;
import com.network.projet.ussd.service.core.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * UssdController - Main entry point for USSD requests
 * 
 * @author USSD Team
 * @since 2026-01-22
 */
@Slf4j
@RestController
@RequestMapping("/api/ussd")
@RequiredArgsConstructor
public class UssdController {

    private final UssdGatewayService ussdGatewayService;
    private final SessionManager sessionManager;
    private final ServiceRegistry serviceRegistry;

    @Value("${ussd.main-menu.code:*500#}")
    private String MAIN_MENU_CODE;

    @Value("${ussd.service.code-prefix:*500*}")
    private String SERVICE_CODE_PREFIX;

    @Value("${ussd.service.code-suffix:#}")
    private String SERVICE_CODE_SUFFIX;

    /**
     * Main USSD endpoint
     */
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<UssdResponse> handleUssdRequest(@RequestBody UssdRequest request) {
        log.info("USSD Request - Session: {}, Code: {}, Phone: {}, Text: '{}'",
                request.getSessionId(), request.getServiceCode(), 
                request.getPhoneNumber(), request.getText());

        return validateRequest(request)
            .flatMap(this::routeRequest)
            .onErrorResume(this::handleError)
            .doOnSuccess(response -> log.info("USSD Response - Continue: {}, Message: {} chars",
                    response.isContinueSession(), response.getMessage().length()));
    }

    /**
     * Routes the request based on serviceCode and text
     */
    private Mono<UssdResponse> routeRequest(UssdRequest request) {
        String serviceCode = request.getServiceCode();
        String text = request.getText();
        
        // Check if session exists first
        return sessionManager.getSession(request.getSessionId())
            .flatMap(existingSession -> {
                // Session exists - continue it
                log.debug("Route: Continuing existing session, currentState={}",
                        existingSession.getCurrentStateId());
                return processServiceRequest(request, existingSession);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // No session - new request
                
                // CAS 1: Main menu
                if (MAIN_MENU_CODE.equals(serviceCode) && isTextEmpty(text)) {
                    log.debug("Route: Main Menu");
                    return showMainMenu();
                }
                
                // CAS 2: Selection from menu
                if (MAIN_MENU_CODE.equals(serviceCode) && !isTextEmpty(text)) {
                    log.debug("Route: Menu Selection → Service");
                    return handleMenuSelection(request);
                }
                
                // CAS 3: Direct service
                log.debug("Route: Direct Service");
                return createAndProcessServiceRequest(request);
            }));
    }

    /**
     * Displays main menu with all services
     */
    private Mono<UssdResponse> showMainMenu() {
        return serviceRegistry.getAllActiveServices()
            .collectList()
            .map(services -> {
                if (services.isEmpty()) {
                    return UssdResponse.builder()
                        .message("Aucun service disponible.")
                        .continueSession(false)
                        .build();
                }
                
                StringBuilder menu = new StringBuilder("🏠 MENU PRINCIPAL\n\n");
                for (int i = 0; i < services.size(); i++) {
                    menu.append(String.format("%d. %s\n", i + 1, services.get(i).getName()));
                }
                menu.append("\n0. Quitter");
                
                log.info("Main menu displayed with {} services", services.size());
                
                return UssdResponse.builder()
                    .message(menu.toString())
                    .continueSession(true)
                    .build();
            });
    }

    /**
     * Handles selection from main menu
     */
    private Mono<UssdResponse> handleMenuSelection(UssdRequest request) {
        String selection = request.getText().trim();
        
        if ("0".equals(selection)) {
            return Mono.just(UssdResponse.builder()
                .message("Merci d'avoir utilisé nos services. À bientôt!")
                .continueSession(false)
                .build());
        }
        
        int serviceNumber;
        try {
            serviceNumber = Integer.parseInt(selection);
        } catch (NumberFormatException e) {
            return Mono.just(UssdResponse.builder()
                .message("❌ Entrée invalide. Entrez un numéro.")
                .continueSession(false)
                .build());
        }
        
        if (serviceNumber < 1) {
            return Mono.just(UssdResponse.builder()
                .message("❌ Numéro invalide.")
                .continueSession(false)
                .build());
        }
        
        return serviceRegistry.getAllActiveServices()
            .collectList()
            .flatMap(services -> {
                if (serviceNumber > services.size()) {
                    return Mono.just(UssdResponse.builder()
                        .message("❌ Service inexistant. Choisissez entre 1 et " + services.size())
                        .continueSession(false)
                        .build());
                }
                
                String targetServiceCode = SERVICE_CODE_PREFIX + serviceNumber + SERVICE_CODE_SUFFIX;
                
                log.info("Menu selection {} → Service code {}", serviceNumber, targetServiceCode);
                
                UssdRequest serviceRequest = UssdRequest.builder()
                    .sessionId(request.getSessionId())
                    .phoneNumber(request.getPhoneNumber())
                    .serviceCode(targetServiceCode)
                    .text("")
                    .build();
                
                return createAndProcessServiceRequest(serviceRequest);
            });
    }

    /**
     * Creates new session and processes service request
     */
    private Mono<UssdResponse> createAndProcessServiceRequest(UssdRequest request) {
        return sessionManager.getOrCreateSession(
                request.getSessionId(),
                request.getPhoneNumber(),
                request.getServiceCode()
            )
            .flatMap(session -> {
                log.debug("New session created - ID: {}, State: {}",
                    session.getSessionId(), session.getCurrentStateId());
                
                return processServiceRequest(request, session);
            });
    }

    /**
     * Processes request for existing session
     */
    private Mono<UssdResponse> processServiceRequest(UssdRequest request, 
            com.network.projet.ussd.domain.model.UssdSession session) {
        
        log.debug("Processing request - SessionId: {}, CurrentState: {}, Input: '{}'",
            session.getSessionId(), session.getCurrentStateId(), request.getText());
        
        return ussdGatewayService.processRequest(request, session)
            .map(stateResult -> UssdResponse.builder()
                .message(stateResult.getMessage())
                .continueSession(stateResult.isContinueSession())
                .build());
    }

    /**
     * Global error handling
     */
    private Mono<UssdResponse> handleError(Throwable error) {
        log.error("USSD Error: {}", error.getMessage(), error);
        
        String message = switch (error) {
            case ServiceNotFoundException e -> "Service indisponible. Contactez le support.";
            case IllegalArgumentException e -> "Requête invalide: " + e.getMessage();
            default -> "Erreur technique. Réessayez plus tard.";
        };
        
        return Mono.just(UssdResponse.builder()
            .message(message)
            .continueSession(false)
            .build());
    }

    /**
     * Request validation
     */
    private Mono<UssdRequest> validateRequest(UssdRequest request) {
        if (isNullOrEmpty(request.getSessionId())) {
            return Mono.error(new IllegalArgumentException("SessionId requis"));
        }
        
        if (isNullOrEmpty(request.getPhoneNumber())) {
            return Mono.error(new IllegalArgumentException("PhoneNumber requis"));
        }
        
        if (isNullOrEmpty(request.getServiceCode())) {
            return Mono.error(new IllegalArgumentException("ServiceCode requis"));
        }
        
        if (request.getText() == null) {
            request.setText("");
        }
        
        log.debug("Request validated");
        return Mono.just(request);
    }

    // ========== DEBUG ENDPOINTS ==========

    @GetMapping("/health")
    public Mono<String> healthCheck() {
        return Mono.just("USSD Gateway is running");
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Mono<String> terminateSession(@PathVariable String sessionId) {
        log.info("Terminating session: {}", sessionId);
        return sessionManager.endSession(sessionId)
            .thenReturn("Session terminated")
            .onErrorResume(e -> Mono.just("Error: " + e.getMessage()));
    }

    @GetMapping("/sessions/{sessionId}")
    public Mono<String> getSessionInfo(@PathVariable String sessionId) {
        return sessionManager.getSession(sessionId)
            .flatMap(session -> sessionManager.getSessionData(sessionId)
                .map(data -> String.format("Session %s - State: %s, Data: %s",
                    sessionId, session.getCurrentStateId(), data.toString())))
            .switchIfEmpty(Mono.just("Session not found"))
            .onErrorResume(e -> Mono.just("Error: " + e.getMessage()));
    }

    // ========== UTILITIES ==========

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isTextEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
}