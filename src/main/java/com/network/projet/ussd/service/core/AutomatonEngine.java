package com.network.projet.ussd.service.core;

import com.network.projet.ussd.domain.enums.ActionType;
import com.network.projet.ussd.domain.enums.StateType;
import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.domain.model.automaton.*;
import com.network.projet.ussd.exception.InvalidStateException;
import com.network.projet.ussd.service.external.ApiInvoker;
import com.network.projet.ussd.service.validation.ValidationService;
import com.network.projet.ussd.util.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomatonEngine {
    
    private final SessionManager sessionManager;
    private final ValidationService validationService;
    private final ApiInvoker apiInvoker;
    private final TemplateEngine templateEngine;
    
    /**
     * Process user input and navigate to next state
     */
    public Mono<StateResult> processInput(
        UssdSession session,
        AutomatonDefinition automaton,
        String userInput
    ) {
        State currentState = automaton.getStateById(session.getCurrentStateId());
        Map<String, Object> collectedData = sessionManager.getCollectedData(session);
        
        log.info("Processing input '{}' at state '{}'", userInput, currentState.getId());
        
        return switch (currentState.getType()) {
            case MENU -> handleMenuState(session, automaton, currentState, userInput, collectedData);
            case INPUT -> handleInputState(session, automaton, currentState, userInput, collectedData);
            case DISPLAY -> handleDisplayState(session, automaton, currentState, userInput, collectedData);
            case FINAL -> handleFinalState(session, automaton, currentState, userInput, collectedData);
        };
    }
    
    /**
     * Handle MENU state
     */
    private Mono<StateResult> handleMenuState(
        UssdSession session,
        AutomatonDefinition automaton,
        State currentState,
        String userInput,
        Map<String, Object> collectedData
    ) {
        Transition matchedTransition = currentState.getTransitions().stream()
            .filter(t -> t.getInput() != null && t.getInput().equals(userInput))
            .findFirst()
            .orElse(null);
        
        if (matchedTransition == null) {
            String message = templateEngine.render(currentState.getMessage(), collectedData);
            return Mono.just(StateResult.builder()
                .message(message + "\n\n❌ Option invalide. Réessayez.")
                .nextStateId(currentState.getId())
                .shouldContinue(true)
                .build());
        }
        
        Mono<UssdSession> sessionMono = Mono.just(session);
        if (matchedTransition.getValue() != null && currentState.getStoreAs() != null) {
            sessionMono = sessionManager.storeData(session, currentState.getStoreAs(), matchedTransition.getValue());
        }
        
        return sessionMono.flatMap(s -> 
            navigateToState(s, automaton, matchedTransition.getNextState(), sessionManager.getCollectedData(s))
        );
    }
    
    /**
     * Handle INPUT state
     */
    private Mono<StateResult> handleInputState(
        UssdSession session,
        AutomatonDefinition automaton,
        State currentState,
        String userInput,
        Map<String, Object> collectedData
    ) {
        Transition specialTransition = currentState.getTransitions().stream()
            .filter(t -> t.getInput() != null && t.getInput().equals(userInput))
            .findFirst()
            .orElse(null);
        
        if (specialTransition != null) {
            return navigateToState(session, automaton, specialTransition.getNextState(), collectedData);
        }
        
        ValidationRule rule = currentState.getValidation();
        if (rule != null) {
            boolean isValid = validationService.validate(userInput, rule);
            
            if (!isValid) {
                Transition invalidTransition = currentState.getTransitions().stream()
                    .filter(t -> "INVALID".equals(t.getCondition()))
                    .findFirst()
                    .orElse(null);
                
                String errorMsg = invalidTransition != null && invalidTransition.getMessage() != null
                    ? invalidTransition.getMessage()
                    : "❌ Entrée invalide. Réessayez:";
                
                String fullMessage = templateEngine.render(currentState.getMessage(), collectedData) 
                    + "\n\n" + errorMsg;
                
                return Mono.just(StateResult.builder()
                    .message(fullMessage)
                    .nextStateId(currentState.getId())
                    .shouldContinue(true)
                    .build());
            }
        }
        
        return sessionManager.storeData(session, currentState.getStoreAs(), userInput)
            .flatMap(s -> {
                Transition validTransition = currentState.getTransitions().stream()
                    .filter(t -> "VALID".equals(t.getCondition()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidStateException("No VALID transition defined"));
                
                return navigateToState(s, automaton, validTransition.getNextState(), 
                    sessionManager.getCollectedData(s));
            });
    }
    
    /**
     * Handle DISPLAY state
     */
    private Mono<StateResult> handleDisplayState(
        UssdSession session,
        AutomatonDefinition automaton,
        State currentState,
        String userInput,
        Map<String, Object> collectedData
    ) {
        return handleMenuState(session, automaton, currentState, userInput, collectedData);
    }
    
    /**
     * Handle FINAL state
     */
    private Mono<StateResult> handleFinalState(
        UssdSession session,
        AutomatonDefinition automaton,
        State currentState,
        String userInput,
        Map<String, Object> collectedData
    ) {
        Action action = currentState.getAction();
        if (action != null && action.getType() == ActionType.API_CALL) {
            return apiInvoker.invoke(automaton.getApiConfig(), action, collectedData)
                .flatMap(apiResponse -> {
                    if (action.getOnSuccess() != null && action.getOnSuccess().getResponseMapping() != null) {
                        Map<String, Object> mergedData = sessionManager.getCollectedData(session);
                        action.getOnSuccess().getResponseMapping().forEach((key, jsonPath) -> {
                            Object value = extractFromResponse(apiResponse, jsonPath);
                            mergedData.put(key, value);
                        });
                        
                        return sessionManager.storeData(session, "_response", mergedData)
                            .flatMap(s -> renderFinalMessage(s, currentState, mergedData, userInput, automaton));
                    }
                    
                    return renderFinalMessage(session, currentState, collectedData, userInput, automaton);
                })
                .onErrorResume(error -> {
                    log.error("API call failed", error);
                    String errorMsg = action.getOnError() != null && action.getOnError().getMessage() != null
                        ? action.getOnError().getMessage()
                        : "❌ Erreur système. Veuillez réessayer plus tard.";
                    
                    return Mono.just(StateResult.builder()
                        .message(errorMsg)
                        .nextStateId(currentState.getId())
                        .shouldContinue(false)
                        .shouldTerminate(true)
                        .build());
                });
        }
        
        return renderFinalMessage(session, currentState, collectedData, userInput, automaton);
    }
    
    /**
     * Render final message
     */
    private Mono<StateResult> renderFinalMessage(
        UssdSession session,
        State finalState,
        Map<String, Object> collectedData,
        String userInput,
        AutomatonDefinition automaton
    ) {
        String message = templateEngine.render(finalState.getMessage(), collectedData);
        
        Transition continueTransition = finalState.getTransitions().stream()
            .filter(t -> t.getInput() != null && t.getInput().equals(userInput))
            .findFirst()
            .orElse(null);
        
        if (continueTransition != null) {
            return navigateToState(session, automaton, continueTransition.getNextState(), collectedData);
        }
        
        return Mono.just(StateResult.builder()
            .message(message)
            .nextStateId(finalState.getId())
            .shouldContinue(false)
            .shouldTerminate(true)
            .build());
    }
    
    /**
     * Navigate to state
     */
    private Mono<StateResult> navigateToState(
        UssdSession session,
        AutomatonDefinition automaton,
        String nextStateId,
        Map<String, Object> collectedData
    ) {
        State nextState = automaton.getStateById(nextStateId);
        
        return sessionManager.updateSessionState(session, nextStateId)
            .map(s -> {
                String message = templateEngine.render(nextState.getMessage(), collectedData);
                
                return StateResult.builder()
                    .message(message)
                    .nextStateId(nextStateId)
                    .shouldContinue(!nextState.isFinal())
                    .shouldTerminate(nextState.isFinal())
                    .build();
            });
    }
    
    /**
     * Extract from API response
     */
    private Object extractFromResponse(Map<String, Object> response, String jsonPath) {
        String[] parts = jsonPath.split("\\.");
        Object current = response;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
}