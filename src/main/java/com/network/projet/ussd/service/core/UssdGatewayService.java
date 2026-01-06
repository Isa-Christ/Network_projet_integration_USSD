package com.network.projet.ussd.service.core;

import com.network.projet.ussd.domain.model.UssdService;
import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.dto.request.UssdRequest;
import com.network.projet.ussd.dto.response.UssdResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UssdGatewayService {
    
    private final ServiceRegistry serviceRegistry;
    private final SessionManager sessionManager;
    private final AutomatonEngine automatonEngine;
    
    private static final String MAIN_MENU_CODE = "*500#";
    
    public Mono<UssdResponse> handleUssdRequest(UssdRequest request) {
        log.info("USSD Request: code={}, phone={}, input={}", 
            request.getServiceCode(), request.getPhoneNumber(), request.getText());
        
        if (MAIN_MENU_CODE.equals(request.getServiceCode())) {
            return handleMainMenu(request);
        } else {
            return handleServiceRequest(request);
        }
    }
    
    private Mono<UssdResponse> handleMainMenu(UssdRequest request) {
        String input = request.getText();
        
        if (input == null || input.isEmpty()) {
            return serviceRegistry.getAllActiveServices()
                .collectList()
                .map(services -> {
                    StringBuilder menu = new StringBuilder("🌐 USSD Gateway\nChoisissez un service:\n\n");
                    for (int i = 0; i < services.size(); i++) {
                        menu.append(String.format("%d. %s\n", i + 1, services.get(i).getName()));
                    }
                    menu.append("\n0. Quitter");
                    
                    return UssdResponse.builder()
                        .message(menu.toString())
                        .continueSession(true)
                        .build();
                });
        }
        
        if (input.equals("0")) {
            return Mono.just(UssdResponse.builder()
                .message("Au revoir! 👋")
                .continueSession(false)
                .build());
        }
        
        int selection;
        try {
            selection = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return Mono.just(UssdResponse.builder()
                .message("Option invalide. Tapez *500# pour recommencer.")
                .continueSession(false)
                .build());
        }
        
        return serviceRegistry.getAllActiveServices()
            .collectList()
            .flatMap(services -> {
                if (selection < 1 || selection > services.size()) {
                    return Mono.just(UssdResponse.builder()
                        .message("Option invalide. Tapez *500# pour recommencer.")
                        .continueSession(false)
                        .build());
                }
                
                UssdService selectedService = services.get(selection - 1);
                return startServiceSession(request.getPhoneNumber(), selectedService);
            });
    }
    
    private Mono<UssdResponse> handleServiceRequest(UssdRequest request) {
        return serviceRegistry.getServiceByShortCode(request.getServiceCode())
            .flatMap(service -> {
                String input = request.getText();
                
                if (input == null || input.isEmpty()) {
                    return startServiceSession(request.getPhoneNumber(), service);
                }
                
                return continueServiceSession(request.getPhoneNumber(), service.getId(), service.getCode(), input);
            })
            .onErrorResume(error -> Mono.just(UssdResponse.builder()
                .message("UNKNOWN APPLICATION")
                .continueSession(false)
                .build()));
    }
    
    private Mono<UssdResponse> startServiceSession(String phoneNumber, UssdService service) {
        return serviceRegistry.loadAutomaton(service.getCode())
            .flatMap(automaton -> {
                String initialStateId = automaton.getInitialState().getId();
                
                return sessionManager.getOrCreateSession(phoneNumber, service.getId(), initialStateId)
                    .map(session -> {
                        String message = automaton.getInitialState().getMessage();
                        
                        return UssdResponse.builder()
                            .message(message)
                            .continueSession(true)
                            .build();
                    });
            });
    }
    
    private Mono<UssdResponse> continueServiceSession(String phoneNumber, Long serviceId, String serviceCode, String input) {
        return sessionManager.getOrCreateSession(phoneNumber, serviceId, "1")
            .flatMap(session -> serviceRegistry.loadAutomaton(serviceCode)
                .flatMap(automaton -> automatonEngine.processInput(session, automaton, input))
                .map(result -> {
                    if (result.isShouldTerminate()) {
                        sessionManager.terminateSession(session.getId()).subscribe();
                    }
                    
                    return UssdResponse.builder()
                        .message(result.getMessage())
                        .continueSession(result.isShouldContinue())
                        .build();
                })
            );
    }
}