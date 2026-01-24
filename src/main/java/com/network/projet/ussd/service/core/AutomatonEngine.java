package com.network.projet.ussd.service.core;

import java.util.List;

import com.network.projet.ussd.domain.enums.ActionType;
import com.network.projet.ussd.domain.enums.StateType;
import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.domain.model.automaton.*;
import com.network.projet.ussd.dto.ExternalApiResponse;
import com.network.projet.ussd.service.validation.ValidationResult;
import com.network.projet.ussd.exception.InvalidStateException;
import com.network.projet.ussd.service.external.ApiInvoker;
import com.network.projet.ussd.service.validation.ValidationService;
import com.network.projet.ussd.util.HandlebarsTemplateEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AutomatonEngine - Automaton execution engine
 * 
 * @author Network Projet Team
 * @since 2026-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomatonEngine {

	private final SessionManager sessionManager;
	private final ValidationService validationService;
	private final ApiInvoker apiInvoker;
	private final HandlebarsTemplateEngine templateEngine;

	/**
	 * Executes current automaton state with user input
	 */
	public Mono<StateResult> executeState(
			AutomatonDefinition automaton,
			UssdSession session,
			String userInput) {

		State currentState = automaton.getStateById(session.getCurrentStateId());

		log.info("Executing state: stateId={}, type={}, sessionId={}, input='{}'",
				currentState.getId(), currentState.getType(), session.getSessionId(), userInput);

		return sessionManager.getSessionData(session.getSessionId())
				.flatMap(collectedData -> {
					return switch (currentState.getType()) {
						case MENU -> executeMenuState(automaton, session, currentState, userInput, collectedData);
						case INPUT -> executeInputState(automaton, session, currentState, userInput, collectedData);
						case DISPLAY -> executeDisplayState(automaton, session, currentState, userInput, collectedData);
						case PROCESSING -> executeProcessingState(automaton, session, currentState, userInput, collectedData);
						case FINAL -> executeFinalState(automaton, session, currentState, userInput, collectedData);
					};
				})
				.doOnSuccess(result -> log.info("State execution completed: nextState={}, continue={}",
						result.getNextStateId(), result.isContinueSession()))
				.doOnError(error -> log.error("State execution failed: stateId={}", currentState.getId(), error));
	}

	/**
	 * Finds next state based on input and validation result
	 */
	public Mono<State> findNextState(
			AutomatonDefinition automaton,
			State state,
			String input,
			ValidationResult validationResult) {

		log.debug("Finding next state: currentState={}, input='{}', validationResult={}",
				state.getId(), input, validationResult != null ? validationResult.getIsValid() : "N/A");

		Transition directTransition = state.getTransitions().stream()
				.filter(t -> t.getInput() != null && t.getInput().equals(input))
				.findFirst()
				.orElse(null);

		if (directTransition != null) {
			log.debug("Found direct transition: nextState={}", directTransition.getNextState());
			return Mono.just(automaton.getStateById(directTransition.getNextState()));
		}

		if (validationResult != null) {
			String condition = validationResult.getIsValid() ? "VALID" : "INVALID";

			Transition conditionTransition = state.getTransitions().stream()
					.filter(t -> condition.equals(t.getCondition()))
					.findFirst()
					.orElse(null);

			if (conditionTransition != null) {
				log.debug("Found condition transition: condition={}, nextState={}",
						condition, conditionTransition.getNextState());
				return Mono.just(automaton.getStateById(conditionTransition.getNextState()));
			}
		}

		Transition defaultTransition = state.getTransitions().stream()
				.filter(t -> t.getInput() == null && t.getCondition() == null)
				.findFirst()
				.orElse(null);

		if (defaultTransition != null) {
			log.debug("Found default transition: nextState={}", defaultTransition.getNextState());
			return Mono.just(automaton.getStateById(defaultTransition.getNextState()));
		}

		log.warn("No next state found for state: {}", state.getId());
		return Mono.error(new InvalidStateException(
				"No valid transition found from state: " + state.getId()));
	}

	/**
	 * Processes and executes an action (API call, etc.)
	 */
	public Mono<ActionResult> processAction(Action action, UssdSession session, AutomatonDefinition automaton) {
		log.info("Processing action: type={}, sessionId={}", action.getType(), session.getSessionId());

		if (action.getType() != ActionType.API_CALL) {
			log.warn("Unsupported action type: {}", action.getType());
			return Mono.just(ActionResult.builder()
					.success(false)
					.build());
		}

		return sessionManager.getSessionData(session.getSessionId())
				.flatMap(collectedData -> apiInvoker.invoke(automaton.getApiConfig(), action, collectedData)
						.flatMap(apiResponse -> {
							log.info("API call successful for action");

							if (action.getOnSuccess() != null && action.getOnSuccess().getNextState() != null) {
								String nextStateId = action.getOnSuccess().getNextState();

								// RÉCUPÈRE LE RESPONSE MAPPING ICI
								Map<String, String> responseMapping = action.getOnSuccess().getResponseMapping();

								return storeApiResponseData(session, action, apiResponse, collectedData)
										.map(mergedData -> ActionResult.builder()
												.success(true)
												.nextState(nextStateId)
												.responseMapping(responseMapping) // AJOUTE LE MAPPING
												.responseData(mergedData)
												.apiResponse(apiResponse)
												.build());
							}

							return Mono.just(ActionResult.builder()
									.success(true)
									.responseData(collectedData)
									.apiResponse(apiResponse)
									.build());
						})
						.onErrorResume(error -> {
							log.error("API call failed for action", error);

							String nextStateId = null;
							if (action.getOnError() != null && action.getOnError().getNextState() != null) {
								nextStateId = action.getOnError().getNextState();
							}

							return Mono.just(ActionResult.builder()
									.success(false)
									.nextState(nextStateId)
									.errorMessage(error.getMessage())
									.exception(error)
									.build());
						}));
	}

	private Mono<StateResult> executeMenuState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> collectedData) {

		log.debug("Executing MENU state: {}", currentState.getId());

		Transition matchedTransition = currentState.getTransitions().stream()
				.filter(t -> t.getInput() != null && t.getInput().equals(userInput))
				.findFirst()
				.orElse(null);

		if (matchedTransition == null) {
			String message = templateEngine.render(currentState.getMessage(), collectedData);
			return Mono.just(StateResult.builder()
					.message(message + "\n\n❌ Option invalide. Réessayez.")
					.nextStateId(currentState.getId())
					.continueSession(true)
					.build());
		}

		Mono<Void> storeMono = Mono.empty();
		if (matchedTransition.getValue() != null && currentState.getStoreAs() != null) {
			storeMono = sessionManager.storeSessionData(
					session.getSessionId(),
					currentState.getStoreAs(),
					matchedTransition.getValue());
		}

		return storeMono.then(
				navigateToState(session, automaton, matchedTransition.getNextState()));
	}

	private Mono<StateResult> executeInputState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> collectedData) {

		log.debug("Executing INPUT state: {}", currentState.getId());

		Transition specialTransition = currentState.getTransitions().stream()
				.filter(t -> t.getInput() != null && t.getInput().equals(userInput))
				.findFirst()
				.orElse(null);

		if (specialTransition != null) {
			return navigateToState(session, automaton, specialTransition.getNextState());
		}

		ValidationRule rule = currentState.getValidation();
		if (rule != null) {
			return validationService.validate(userInput, rule.getType())
					.flatMap(validationResult -> {
						if (!validationResult.getIsValid()) {
							return handleInvalidInput(automaton, currentState, collectedData, validationResult);
						}
						return handleValidInput(session, automaton, currentState, userInput);
					});
		}

		return handleValidInput(session, automaton, currentState, userInput);
	}

	private Mono<StateResult> executeDisplayState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> collectedData) {

		log.debug("Executing DISPLAY state: {}", currentState.getId());
		return executeMenuState(automaton, session, currentState, userInput, collectedData);
	}

	private Mono<StateResult> executeProcessingState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> collectedData) {

		log.debug("Executing PROCESSING state: {}", currentState.getId());

		Action action = currentState.getAction();
		if (action != null) {
			return processAction(action, session, automaton)
					.flatMap(actionResult -> {
						if (actionResult.getNextState() != null) {
							return navigateToState(session, automaton, actionResult.getNextState());
						}

						Transition defaultTransition = currentState.getTransitions().stream()
								.filter(t -> "SUCCESS".equals(t.getCondition()) && actionResult.isSuccess())
								.findFirst()
								.orElseGet(() -> currentState.getTransitions().stream()
										.filter(t -> "ERROR".equals(t.getCondition()) && !actionResult.isSuccess())
										.findFirst()
										.orElse(null));

						if (defaultTransition != null) {
							return navigateToState(session, automaton, defaultTransition.getNextState());
						}

						return Mono.just(StateResult.builder()
								.message("Erreur de traitement")
								.continueSession(false)
								.build());
					});
		}

		return Mono.just(StateResult.builder()
				.message("Erreur: Aucune action définie")
				.continueSession(false)
				.build());
	}

	private Mono<StateResult> executeFinalState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> collectedData) {

		log.debug("Executing FINAL state: {}", currentState.getId());

		Action action = currentState.getAction();

		if (action != null && action.getType() == ActionType.API_CALL) {
			return processAction(action, session, automaton)
					.flatMap(actionResult -> {
						if (actionResult.isSuccess()) {
							return renderFinalMessage(session, automaton, currentState, userInput, collectedData);
						} else {
							return Mono.just(StateResult.builder()
									.message("Erreur lors du traitement final")
									.nextStateId(currentState.getId())
									.continueSession(false)
									.build());
						}
					});
		}

		return renderFinalMessage(session, automaton, currentState, userInput, collectedData);
	}

	private Mono<StateResult> handleValidInput(
			UssdSession session,
			AutomatonDefinition automaton,
			State currentState,
			String userInput) {

		return sessionManager.storeSessionData(session.getSessionId(), currentState.getStoreAs(), userInput)
				.then(sessionManager.getSessionData(session.getSessionId()))
				.flatMap(collectedData -> {
					Transition validTransition = currentState.getTransitions().stream()
							.filter(t -> "VALID".equals(t.getCondition()))
							.findFirst()
							.orElseThrow(() -> new InvalidStateException(
									"No VALID transition defined for state: " + currentState.getId()));

					String nextStateId = validTransition.getNextState();
					State nextState = automaton.getStateById(nextStateId);

					// Navigate to next state and update session
					session.setCurrentStateId(nextStateId);
					return sessionManager.updateSession(session)
							.then(Mono.defer(() -> {
								// If next state is PROCESSING, execute it immediately
								if (nextState.getType() == StateType.PROCESSING) {
									log.debug("Next state is PROCESSING - executing immediately without waiting for input");
									return executeProcessingState(automaton, session, nextState, "", collectedData);
								}

								// Otherwise, navigate normally
								return navigateToState(session, automaton, nextStateId);
							}));
				});
	}

	private Mono<StateResult> handleInvalidInput(
			AutomatonDefinition automaton,
			State currentState,
			Map<String, Object> collectedData,
			ValidationResult validationResult) {

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
				.continueSession(true)
				.build());
	}

	private Mono<StateResult> navigateToState(
			UssdSession session,
			AutomatonDefinition automaton,
			String nextStateId) {

		State nextState = automaton.getStateById(nextStateId);

		log.debug("Navigating to state: {}", nextStateId);

		session.setCurrentStateId(nextStateId);
		return sessionManager.updateSession(session)
				.then(sessionManager.getSessionData(session.getSessionId()))
				.flatMap(collectedData -> {
					// If next state is PROCESSING, execute it immediately
					if (nextState.getType() == StateType.PROCESSING) {
						log.debug("State {} is PROCESSING - executing immediately", nextStateId);
						return executeProcessingState(automaton, session, nextState, "", collectedData);
					}

					String message = templateEngine.render(nextState.getMessage(), collectedData);
					boolean isFinal = nextState.getType() == StateType.FINAL;

					return Mono.just(StateResult.builder()
							.message(message)
							.nextStateId(nextStateId)
							.continueSession(!isFinal)
							.build());
				});
	}

	private Mono<StateResult> renderFinalMessage(
			UssdSession session,
			AutomatonDefinition automaton,
			State finalState,
			String userInput,
			Map<String, Object> collectedData) {

		String message = templateEngine.render(finalState.getMessage(), collectedData);

		Transition continueTransition = finalState.getTransitions().stream()
				.filter(t -> t.getInput() != null && t.getInput().equals(userInput))
				.findFirst()
				.orElse(null);

		if (continueTransition != null) {
			return navigateToState(session, automaton, continueTransition.getNextState());
		}

		return Mono.just(StateResult.builder()
				.message(message)
				.nextStateId(finalState.getId())
				.continueSession(false)
				.build());
	}

	private Mono<Map<String, Object>> storeApiResponseData(
			UssdSession session,
			Action action,
			ExternalApiResponse apiResponse,
			Map<String, Object> collectedData) {

		Map<String, Object> mergedData = new java.util.HashMap<>(collectedData);

		if (action.getOnSuccess() != null) {
			log.debug(">>> action.getOnSuccess() = {}", action.getOnSuccess());
			log.debug(">>> action.getOnSuccess().getResponseMapping() = {}", action.getOnSuccess().getResponseMapping());
			Map<String, Object> extracted = extractResponseData(apiResponse);
			log.debug(">>> Extracted response data: {}", extracted);

			// Applique le responseMapping
			Map<String, String> responseMapping = action.getOnSuccess().getResponseMapping();
			if (responseMapping != null && !responseMapping.isEmpty()) {
				log.debug(">>> Applying responseMapping: {}", responseMapping);

				responseMapping.forEach((targetKey, sourcePath) -> {
					Object value = extracted.get(sourcePath);
					if (value != null) {
						mergedData.put(targetKey, value);
						log.debug(">>> Mapped '{}' -> '{}' with {} items", sourcePath, targetKey,
								value instanceof List ? ((List<?>) value).size() + " items" : "1 item");
					} else {
						log.warn(">>> Source path '{}' not found in extracted data", sourcePath);
					}
				});
			} else {
				// Si pas de mapping, merge tout
				mergedData.putAll(extracted);
			}
		}

		log.debug(">>> Final merged data keys: {}", mergedData.keySet());

		return sessionManager.storeSessionData(session.getSessionId(), "_apiResponse", mergedData)
				.thenReturn(mergedData);
	}

	private Map<String, Object> extractResponseData(ExternalApiResponse response) {
		Map<String, Object> extracted = new java.util.HashMap<>();

		Object data = response.getData();

		log.debug(">>> extractResponseData - data type: {}", data != null ? data.getClass().getName() : "null");
		log.debug(">>> extractResponseData - data instanceof List: {}", data instanceof List);
		log.debug(">>> extractResponseData - data instanceof Map: {}", data instanceof Map);

		if (data instanceof Map) {
			extracted.putAll((Map<String, Object>) data);
			log.debug(">>> Extracted as Map, keys: {}", extracted.keySet());
		} else if (data instanceof List) {
			extracted.put("data", data);
			log.debug(">>> Extracted as List with {} items, stored under key 'data'", ((List<?>) data).size());
		}

		log.debug(">>> Final extracted map: {}", extracted);

		return extracted;
	}

	@Deprecated
	public Mono<StateResult> processInput(
			UssdSession session,
			AutomatonDefinition automaton,
			String userInput) {

		// log.debug("Legacy processInput called");
		return executeState(automaton, session, userInput);
	}
}
