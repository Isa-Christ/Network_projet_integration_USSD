package com.network.projet.ussd.service.core;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.network.projet.ussd.domain.enums.ActionType;
import com.network.projet.ussd.domain.enums.StateType;
import com.network.projet.ussd.domain.model.UssdSession;
import com.network.projet.ussd.domain.model.automaton.Action;
import com.network.projet.ussd.domain.model.automaton.ActionResult;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.domain.model.automaton.State;
import com.network.projet.ussd.domain.model.automaton.Transition;
import com.network.projet.ussd.domain.model.automaton.ValidationRule;
import com.network.projet.ussd.dto.ExternalApiResponse;
import com.network.projet.ussd.service.validation.ValidationResult;
import com.network.projet.ussd.exception.ApiCallException;
import com.network.projet.ussd.exception.InvalidStateException;
import com.network.projet.ussd.service.external.ApiInvoker;
import com.network.projet.ussd.service.validation.ValidationService;
import com.network.projet.ussd.util.HandlebarsTemplateEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AutomatonEngine - Moteur d'exécution de l'automate USSD
 * 
 * @author Network Projet Team
 * @version 3.1 - Strategic merge after git conflict
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomatonEngine {

	private final SessionManager sessionManager;
	private final ValidationService validationService;
	private final ApiInvoker apiInvoker;
	private final HandlebarsTemplateEngine templateEngine;
	private final GenericStorageService storageService;
	private final ConditionalEvaluator conditionalEvaluator;
	private final ObjectMapper objectMapper;

	// ========================================================================
	// FLUX D'EXÉCUTION PRINCIPAL
	// ========================================================================

	public Mono<StateResult> executeState(
			AutomatonDefinition automaton,
			UssdSession session,
			String userInput) {

		State currentState = automaton.getStateById(session.getCurrentStateId());
		String cleanInput = userInput != null ? userInput.trim() : "";

		log.info("Executing state: stateId={}, type={}, sessionId={}, input='{}' (cleaned)",
				currentState.getId(), currentState.getType(), session.getSessionId(), cleanInput);

		return executePreActions(currentState, session, automaton)
				.then(sessionManager.getSessionData(session.getSessionId()))
				.flatMap(sessionData -> executeStateByType(automaton, session, currentState, cleanInput, sessionData))
				.flatMap(result -> executePostActions(currentState, session, automaton).thenReturn(result))
				.doOnSuccess(result -> log.info("State execution completed: nextState={}, continue={}",
						result.getNextStateId(), result.isContinueSession()))
				.doOnError(error -> log.error("State execution failed: stateId={}", currentState.getId(), error));
	}

	private Mono<StateResult> executeStateByType(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		StateType type = currentState.getType() != null ? currentState.getType() : StateType.MENU;

		return switch (type) {
			case MENU -> executeMenuState(automaton, session, currentState, userInput, sessionData);
			case INPUT -> executeInputState(automaton, session, currentState, userInput, sessionData);
			case DISPLAY -> executeDisplayState(automaton, session, currentState, userInput, sessionData);
			case PROCESSING -> executeProcessingState(automaton, session, currentState, userInput, sessionData);
			case FINAL -> executeFinalState(automaton, session, currentState, userInput, sessionData);
		};
	}

	// ========================================================================
	// PRE-ACTIONS & POST-ACTIONS
	// ========================================================================

	private Mono<Void> executePreActions(State state, UssdSession session, AutomatonDefinition automaton) {
		return executeActions(state.getPreActions(), session, automaton, "PRE");
	}

	private Mono<Void> executePostActions(State state, UssdSession session, AutomatonDefinition automaton) {
		return executeActions(state.getPostActions(), session, automaton, "POST");
	}

	private Mono<Void> executeActions(List<Action> actions, UssdSession session, AutomatonDefinition automaton,
			String phase) {
		if (actions == null || actions.isEmpty()) {
			return Mono.empty();
		}

		log.debug("Executing {} {}-actions for session {}", actions.size(), phase, session.getSessionId());

		return Flux.fromIterable(actions)
				.concatMap(action -> executeAction(action, session, automaton))
				.then()
				.doOnSuccess(v -> log.debug("{}-actions completed", phase));
	}

	private Mono<Void> executeAction(Action action, UssdSession session, AutomatonDefinition automaton) {
		return sessionManager.getSessionData(session.getSessionId())
				.flatMap(sessionData -> {
					return switch (action.getType()) {
						case STORAGE_LOAD -> executeStorageLoad(action, session);
						case STORAGE_SAVE -> executeStorageSave(action, session, sessionData);
						case STORAGE_APPEND -> executeStorageAppend(action, session, sessionData);
						case STORAGE_DELETE -> executeStorageDelete(action, session);
						case API_CALL -> executeApiCallAction(action, session, automaton, sessionData);
						default -> {
							log.warn("Unsupported action type: {}", action.getType());
							yield Mono.empty();
						}
					};
				});
	}

	// ========================================================================
	// OPÉRATIONS DE STOCKAGE (DÉVELOPPEMENT)
	// ========================================================================

	private Mono<Void> executeStorageLoad(Action action, UssdSession session) {
		String storageKey = action.getStorageKey();
		String storeAs = action.getStoreAs();

		if (storageKey == null) {
			log.warn("STORAGE_LOAD action missing storageKey");
			return Mono.empty();
		}

		return storageService.load(session.getPhoneNumber(), session.getServiceCode(), storageKey)
				.flatMap(value -> {
					if (storeAs != null && value != null) {
						return sessionManager.storeSessionData(session.getSessionId(), storeAs, value);
					}
					return Mono.empty();
				})
				.then()
				.doOnSuccess(v -> log.debug("Storage loaded: key={}, storeAs={}", storageKey, storeAs));
	}

	private Mono<Void> executeStorageSave(Action action, UssdSession session, Map<String, Object> sessionData) {
		String storageKey = action.getStorageKey();
		Object value = action.getValue();

		if (storageKey == null || value == null) {
			log.warn("STORAGE_SAVE action missing storageKey or value");
			return Mono.empty();
		}

		Object resolvedValue = resolveValue(value, sessionData);
		log.debug("Saving to storage: key={}, value={}", storageKey, resolvedValue);

		return storageService.save(session.getPhoneNumber(), session.getServiceCode(), storageKey, resolvedValue);
	}

	private Mono<Void> executeStorageAppend(Action action, UssdSession session, Map<String, Object> sessionData) {
		String storageKey = action.getStorageKey();
		Object value = action.getValue();

		if (storageKey == null || value == null) {
			log.warn("STORAGE_APPEND action missing storageKey or value");
			return Mono.empty();
		}

		Object resolvedValue = resolveValue(value, sessionData);
		log.debug("Appending to storage: key={}, item={}", storageKey, resolvedValue);

		return storageService.append(session.getPhoneNumber(), session.getServiceCode(), storageKey, resolvedValue);
	}

	private Mono<Void> executeStorageDelete(Action action, UssdSession session) {
		String storageKey = action.getStorageKey();

		if (storageKey == null) {
			log.warn("STORAGE_DELETE action missing storageKey");
			return Mono.empty();
		}

		log.debug("Deleting from storage: key={}", storageKey);
		return storageService.delete(session.getPhoneNumber(), session.getServiceCode(), storageKey);
	}

	// ========================================================================
	// EXÉCUTEURS D'ÉTATS
	// ========================================================================

	private Mono<StateResult> executeMenuState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		log.debug("Executing MENU state: {}", currentState.getId());

		if (userInput == null || userInput.trim().isEmpty()) {
			String message = templateEngine.render(currentState.getMessage(), sessionData);
			return Mono.just(StateResult.builder()
					.message(message)
					.nextStateId(currentState.getId())
					.continueSession(true)
					.build());
		}

		return findMatchingTransition(currentState, userInput, sessionData)
				.flatMap(transition -> {
					// Stockage de valeur si nécessaire
					if (transition.getValue() != null && currentState.getStoreAs() != null) {
						return sessionManager.storeSessionData(
								session.getSessionId(),
								currentState.getStoreAs(),
								transition.getValue())
								.then(navigateToState(session, automaton, transition.getNextState()));
					}
					return navigateToState(session, automaton, transition.getNextState());
				})
				.switchIfEmpty(Mono.defer(() -> {
					String message = templateEngine.render(currentState.getMessage(), sessionData);
					return Mono.just(StateResult.builder()
							.message(message + "\n\n❌ Option invalide. Réessayez.")
							.nextStateId(currentState.getId())
							.continueSession(true)
							.build());
				}));
	}

	private Mono<StateResult> executeInputState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		log.debug("Executing INPUT state: {}", currentState.getId());

		// Vérifier les transitions spéciales (ex: "99" pour retour)
		return findMatchingTransition(currentState, userInput, sessionData)
				.flatMap(transition -> navigateToState(session, automaton, transition.getNextState()))
				.switchIfEmpty(Mono.defer(() -> validateAndProcessInput(
						automaton, session, currentState, userInput, sessionData)));
	}

	private Mono<StateResult> executeDisplayState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		log.debug("Executing DISPLAY state: {}", currentState.getId());
		return executeMenuState(automaton, session, currentState, userInput, sessionData);
	}

	private Mono<StateResult> executeProcessingState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		log.debug("Executing PROCESSING state: {}", currentState.getId());

		Action action = currentState.getAction();

		// Si aucune action n'est définie, utiliser les transitions conditionnelles
		if (action == null) {
			log.debug("No action defined - checking conditional transitions");
			return findMatchingTransition(currentState, userInput, sessionData)
					.flatMap(transition -> navigateToState(session, automaton, transition.getNextState()))
					.switchIfEmpty(Mono.error(new InvalidStateException(
							"No matching transition for PROCESSING state: " + currentState.getId())));
		}

		// Exécuter l'action si elle est définie
		return executeApiAction(action, session, automaton, sessionData)
				.flatMap(actionResult -> handleActionResult(
						automaton, session, currentState, actionResult, sessionData));
	}

	private Mono<StateResult> executeFinalState(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		log.debug("Executing FINAL state: {}", currentState.getId());

		Action action = currentState.getAction();

		if (action != null && action.getType() == ActionType.API_CALL) {
			return executeApiAction(action, session, automaton, sessionData)
					.flatMap(actionResult -> {
						if (actionResult.isSuccess()) {
							return renderMessage(currentState, sessionData, false);
						}
						return Mono.just(StateResult.builder()
								.message("Erreur lors du traitement final")
								.nextStateId(currentState.getId())
								.continueSession(false)
								.build());
					});
		}

		return renderMessage(currentState, sessionData, false);
	}

	// ========================================================================
	// GESTION DES TRANSITIONS (PRIORITÉ IA - FUZZY MATCHING)
	// ========================================================================

	private Mono<Transition> findMatchingTransition(State state, String userInput, Map<String, Object> sessionData) {
		if (state.getTransitions() == null || state.getTransitions().isEmpty()) {
			return Mono.empty();
		}

		return Flux.fromIterable(state.getTransitions())
				.filter(t -> matchesTransition(t, userInput, sessionData))
				.next();
	}

	private boolean matchesTransition(Transition transition, String userInput, Map<String, Object> sessionData) {
		// Vérification de la correspondance d'entrée (pour les menus)
		if (transition.getInput() != null) {
			String expected = transition.getInput().trim();
			String actual = userInput.trim();

			// Match exact
			if (expected.equals(actual))
				return true;

			// Match fuzzy pour la robustesse (IA Generation)
			if (expected.startsWith(actual + ".") || expected.endsWith(" " + actual))
				return true;

			return false;
		}

		// Vérification de la condition (pour les PROCESSING)
		if (transition.getCondition() != null) {
			// Conditions spéciales gérées séparément
			if ("VALID".equals(transition.getCondition()) ||
					"INVALID".equals(transition.getCondition()) ||
					"SUCCESS".equals(transition.getCondition()) ||
					"ERROR".equals(transition.getCondition())) {
				return false;
			}

			// Évaluation des expressions conditionnelles
			return conditionalEvaluator.evaluate(transition.getCondition(), sessionData);
		}

		return false;
	}

	// ========================================================================
	// VALIDATION D'INPUT
	// ========================================================================

	private Mono<StateResult> validateAndProcessInput(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			String userInput,
			Map<String, Object> sessionData) {

		ValidationRule rule = currentState.getValidation();

		if (rule == null) {
			return storeAndNavigate(session, automaton, currentState, userInput);
		}

		// Passer minLength et maxLength (Robustesse Develop)
		return validationService.validate(
				userInput,
				rule.getType(),
				rule.getMinLength(),
				rule.getMaxLength())
				.flatMap(validationResult -> {
					if (!validationResult.getIsValid()) {
						return handleInvalidInput(currentState, sessionData, validationResult);
					}
					return storeAndNavigate(session, automaton, currentState, userInput);
				});
	}

	private Mono<StateResult> storeAndNavigate(
			UssdSession session,
			AutomatonDefinition automaton,
			State currentState,
			String userInput) {

		String storeKey = currentState.getStoreAs();

		log.debug(">>> STORE AND NAVIGATE - storeKey: {}, userInput: {}", storeKey, userInput);

		return sessionManager.storeSessionData(session.getSessionId(), storeKey, userInput)
				.doOnSuccess(v -> log.debug(">>> Data stored successfully"))
				.then(sessionManager.getSession(session.getSessionId())) // RELOAD SESSION
				.flatMap(reloadedSession -> {
					Transition validTransition = currentState.getTransitions().stream()
							.filter(t -> "VALID".equals(t.getCondition()))
							.findFirst()
							.orElseThrow(() -> new InvalidStateException(
									"No VALID transition for state: " + currentState.getId()));

					log.debug(">>> Found VALID transition to state: {}", validTransition.getNextState());

					// MISE À JOUR DE L'ÉTAT DANS LA SESSION
					reloadedSession.setCurrentStateId(validTransition.getNextState());

					return sessionManager.updateSession(reloadedSession) // SAUVEGARDE EN BASE
							.then(navigateToNextState(reloadedSession, automaton, validTransition.getNextState()));
				})
				.doOnSuccess(result -> log.debug(">>> Navigation completed to state: {}", result.getNextStateId()));
	}

	private Mono<StateResult> handleInvalidInput(
			State currentState,
			Map<String, Object> sessionData,
			ValidationResult validationResult) {

		Transition invalidTransition = currentState.getTransitions().stream()
				.filter(t -> "INVALID".equals(t.getCondition()))
				.findFirst()
				.orElse(null);

		String errorMsg = invalidTransition != null && invalidTransition.getMessage() != null
				? invalidTransition.getMessage()
				: "❌ Entrée invalide. Réessayez:";

		String fullMessage = templateEngine.render(currentState.getMessage(), sessionData) + "\n\n" + errorMsg;

		return Mono.just(StateResult.builder()
				.message(fullMessage)
				.nextStateId(currentState.getId())
				.continueSession(true)
				.build());
	}

	// ========================================================================
	// EXÉCUTION D'API (DÉVELOPPEMENT - SUPPORT LISTES/OBJETS)
	// ========================================================================

	private Mono<Void> executeApiCallAction(Action action, UssdSession session, AutomatonDefinition automaton,
			Map<String, Object> sessionData) {

		return executeApiAction(action, session, automaton, sessionData)
				.flatMap(actionResult -> {
					if (action.getStoreAs() != null && actionResult.isSuccess()) {
						return storeApiResponseData(session, action, actionResult.getApiResponse(), sessionData)
								.then();
					}
					return Mono.empty();
				})
				.onErrorResume(error -> {
					log.error("API call failed in action", error);
					return Mono.empty();
				});
	}

	private Mono<ActionResult> executeApiAction(
			Action action,
			UssdSession session,
			AutomatonDefinition automaton,
			Map<String, Object> sessionData) {

		log.info("Executing API call: sessionId={}", session.getSessionId());

		return apiInvoker.invoke(automaton.getApiConfig(), action, sessionData)
				.flatMap(apiResponse -> {
					log.info("API call successful");

					String nextStateId = action.getOnSuccess() != null
							? action.getOnSuccess().getNextState()
							: null;

					Map<String, String> responseMapping = action.getOnSuccess() != null
							? action.getOnSuccess().getResponseMapping()
							: null;

					return storeApiResponseData(session, action, apiResponse, sessionData)
							.map(mergedData -> ActionResult.builder()
									.success(true)
									.nextState(nextStateId)
									.responseMapping(responseMapping)
									.responseData(mergedData)
									.apiResponse(apiResponse)
									.build());
				})
				.onErrorResume(error -> {
					log.error("API call failed", error);

					String nextStateId = action.getOnError() != null
							? action.getOnError().getNextState()
							: null;

					String errorMessage = extractErrorMessage(error, action);
					sessionData.put("apiErrorMessage", errorMessage);

					if (nextStateId != null) {
						session.setCurrentStateId(nextStateId);
					}

					return sessionManager.updateSession(session)
							.then(Mono.just(ActionResult.builder()
									.success(false)
									.nextState(nextStateId)
									.errorMessage(errorMessage)
									.exception(error)
									.build()));
				});
	}

	private String extractErrorMessage(Throwable error, Action action) {
		if (error instanceof ApiCallException apiEx) {
			try {
				JsonNode errorJson = objectMapper.readTree(apiEx.getResponseBody());
				if (errorJson.has("error"))
					return errorJson.get("error").asText();
				else if (errorJson.has("message"))
					return errorJson.get("message").asText();
				else if (errorJson.has("msg"))
					return errorJson.get("msg").asText();
			} catch (Exception e) {
				log.warn("Could not parse API error response", e);
			}
		}

		return action.getOnError() != null && action.getOnError().getMessage() != null
				? action.getOnError().getMessage()
				: error.getMessage();
	}

	private Mono<StateResult> handleActionResult(
			AutomatonDefinition automaton,
			UssdSession session,
			State currentState,
			ActionResult actionResult,
			Map<String, Object> sessionData) {

		if (actionResult.getNextState() != null) {
			if (!actionResult.isSuccess() && actionResult.getErrorMessage() != null) {
				return navigateToStateWithMessage(session, automaton,
						actionResult.getNextState(), actionResult.getErrorMessage());
			}
			return navigateToState(session, automaton, actionResult.getNextState());
		}

		return findTransitionByActionResult(currentState, actionResult, sessionData)
				.flatMap(transition -> navigateToState(session, automaton, transition.getNextState()))
				.switchIfEmpty(Mono.just(StateResult.builder()
						.message("Erreur de traitement")
						.continueSession(false)
						.build()));
	}

	private Mono<Transition> findTransitionByActionResult(
			State state,
			ActionResult actionResult,
			Map<String, Object> sessionData) {

		String conditionToMatch = actionResult.isSuccess() ? "SUCCESS" : "ERROR";

		return Flux.fromIterable(state.getTransitions())
				.filter(t -> conditionToMatch.equals(t.getCondition()))
				.next();
	}

	// ========================================================================
	// NAVIGATION
	// ========================================================================

	private Mono<StateResult> navigateToState(
			UssdSession session,
			AutomatonDefinition automaton,
			String nextStateId) {

		State nextState = automaton.getStateById(nextStateId);
		log.debug("Navigating to state: {} (type: {})", nextStateId, nextState.getType());

		return sessionManager.getSession(session.getSessionId())
				.flatMap(reloadedSession -> {
					reloadedSession.setCurrentStateId(nextStateId);
					return sessionManager.updateSession(reloadedSession);
				})
				.then(navigateToNextState(session, automaton, nextStateId));
	}

	private Mono<StateResult> navigateToNextState(
			UssdSession session,
			AutomatonDefinition automaton,
			String nextStateId) {

		State nextState = automaton.getStateById(nextStateId);
		StateType type = nextState.getType() != null ? nextState.getType() : StateType.MENU;

		log.debug(">>> NAVIGATE TO NEXT STATE: {} (type: {})", nextStateId, type);

		return sessionManager.getSessionData(session.getSessionId())
				.flatMap(sessionData -> {
					session.setCurrentStateId(nextStateId);
					log.debug(">>> Session updated in memory: currentState={}", session.getCurrentStateId());

					if (type == StateType.PROCESSING) {
						log.debug("Auto-executing PROCESSING state: {}", nextStateId);
						return executeProcessingState(automaton, session, nextState, "", sessionData);
					}

					return renderMessage(nextState, sessionData, type != StateType.FINAL);
				})
				.doOnSuccess(result -> log.debug(">>> Navigate completed: nextStateId={}", result.getNextStateId()));
	}

	private Mono<StateResult> navigateToStateWithMessage(
			UssdSession session,
			AutomatonDefinition automaton,
			String nextStateId,
			String errorMessage) {

		return navigateToState(session, automaton, nextStateId)
				.map(result -> StateResult.builder()
						.message(errorMessage + "\n\n" + result.getMessage())
						.nextStateId(result.getNextStateId())
						.continueSession(result.isContinueSession())
						.build());
	}

	private Mono<StateResult> renderMessage(State state, Map<String, Object> sessionData, boolean continueSession) {
		String message = templateEngine.render(state.getMessage(), sessionData);

		return Mono.just(StateResult.builder()
				.message(message)
				.nextStateId(state.getId())
				.continueSession(continueSession)
				.build());
	}

	// ========================================================================
	// GESTION DES RÉPONSES API (SUPPORT LISTES)
	// ========================================================================

	private Mono<Map<String, Object>> storeApiResponseData(
			UssdSession session,
			Action action,
			ExternalApiResponse apiResponse,
			Map<String, Object> collectedData) {

		Map<String, Object> mergedData = new HashMap<>(collectedData);
		Object responseData = apiResponse.getData();

		if (action.getOnSuccess() != null) {
			Map<String, String> responseMapping = action.getOnSuccess().getResponseMapping();

			if (responseMapping != null && !responseMapping.isEmpty()) {
				Map<String, Object> dataToStore = new HashMap<>();

				responseMapping.forEach((targetKey, sourcePath) -> {
					Object value = null;
					if (".".equals(sourcePath)) {
						value = responseData;
					} else {
						value = extractNestedValue(responseData, sourcePath);
					}

					if (value != null) {
						dataToStore.put(targetKey, value);
						mergedData.put(targetKey, value);
					}
				});

				return sessionManager.storeBatchData(session.getSessionId(), dataToStore)
						.thenReturn(mergedData);
			}

			if (responseData instanceof Map) {
				mergedData.putAll((Map<String, Object>) responseData);
			}
		}

		return Mono.just(mergedData);
	}

	private Object extractNestedValue(Object data, String path) {
		if (path == null || path.isEmpty() || data == null) {
			return null;
		}

		if (".".equals(path)) {
			return data;
		}

		String[] parts = path.split("\\.");
		Object current = data;

		for (String part : parts) {
			if (part.isEmpty())
				continue;

			if (current instanceof Map) {
				current = ((Map<?, ?>) current).get(part);
			} else if (current instanceof List) {
				try {
					int index = Integer.parseInt(part);
					List<?> list = (List<?>) current;
					current = (index >= 0 && index < list.size()) ? list.get(index) : null;
				} catch (NumberFormatException e) {
					return null;
				}
			} else {
				return null;
			}

			if (current == null)
				return null;
		}

		return current;
	}

	// ========================================================================
	// MÉTHODES UTILITAIRES
	// ========================================================================

	@SuppressWarnings("unchecked")
	private Object resolveValue(Object value, Map<String, Object> sessionData) {
		if (value == null)
			return null;

		if (value instanceof String) {
			return templateEngine.render((String) value, sessionData);
		}

		if (value instanceof Map) {
			Map<String, Object> resolved = new HashMap<>();
			((Map<String, Object>) value).forEach((key, val) -> resolved.put(key, resolveValue(val, sessionData)));
			return resolved;
		}

		if (value instanceof List) {
			return ((List<Object>) value).stream()
					.map(item -> resolveValue(item, sessionData))
					.toList();
		}

		return value;
	}

	@Deprecated
	public Mono<StateResult> processInput(UssdSession session, AutomatonDefinition automaton, String userInput) {
		return executeState(automaton, session, userInput);
	}

	@Deprecated
	public Mono<ActionResult> processAction(Action action, UssdSession session, AutomatonDefinition automaton) {
		return sessionManager.getSessionData(session.getSessionId())
				.flatMap(sessionData -> executeApiAction(action, session, automaton, sessionData));
	}
}