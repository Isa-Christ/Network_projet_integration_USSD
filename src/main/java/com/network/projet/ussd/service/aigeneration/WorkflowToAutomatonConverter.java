package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.enums.*;
import com.network.projet.ussd.domain.model.aigeneration.*;
import com.network.projet.ussd.domain.model.automaton.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convertit WorkflowProposals en AutomatonDefinition.
 * 
 * @author Network Project Team
 * @since 2025-01-29
 */
@Component
@Slf4j
public class WorkflowToAutomatonConverter {
    
    /**
     * Convertit une proposition en AutomatonDefinition.
     */
    public AutomatonDefinition convert(
        WorkflowProposal proposal,
        ApiStructure apiStructure,
        GenerationHints hints
    ) {
        log.info("🔄 Converting proposal '{}' to AutomatonDefinition", proposal.getName());
        
        AutomatonDefinition automaton = new AutomatonDefinition();
        
        // Métadonnées de service
        automaton.setServiceCode(generateServiceCode(hints.getService_name()));
        automaton.setServiceName(hints.getService_name());
        automaton.setVersion("1.0.0");
        automaton.setShortCode(hints.getShort_code());
        automaton.setDescription(proposal.getDescription());
        
        // Configuration API
        automaton.setApiConfig(buildApiConfig(apiStructure));
        
        // Configuration session
        automaton.setSessionConfig(buildSessionConfig());
        
        // Conversion des états
        List<State> states = convertStates(proposal.getStates(), apiStructure);
        automaton.setStates(states);
        
        log.info("✅ Conversion complete: {} states generated", states.size());
        return automaton;
    }
    
    /**
     * Génère un serviceCode valide à partir du nom.
     * Ex: "Todo Manager" → "todo-manager"
     */
    private String generateServiceCode(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return "generated-service";
        }
        
        return serviceName
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }
    
    /**
     * Construit ApiConfig depuis ApiStructure.
     */
    private ApiConfig buildApiConfig(ApiStructure apiStructure) {
        Authentication auth = new Authentication();
        auth.setType(parseAuthType(apiStructure.getAuthentication_type()));
        
        Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put("Content-Type", "application/json");
        
        return ApiConfig.builder()
            .baseUrl(apiStructure.getBase_url())
            .timeout(10000)
            .retryAttempts(2)
            .authentication(auth)
            .headers(defaultHeaders)
            .build();
    }
    
    /**
     * Parse le type d'authentification.
     */
    private AuthenticationType parseAuthType(String authType) {
        if (authType == null || authType.equalsIgnoreCase("none")) {
            return AuthenticationType.NONE;
        } else if (authType.equalsIgnoreCase("bearer")) {
            return AuthenticationType.BEARER;
        } else if (authType.equalsIgnoreCase("apiKey") || authType.equalsIgnoreCase("api_key")) {
            return AuthenticationType.API_KEY;
        }
        return AuthenticationType.NONE;
    }
    
    /**
     * Construit SessionConfig par défaut.
     */
    private SessionConfig buildSessionConfig() {
        return new SessionConfig(60, 30);
    }
    
    /**
     * Convertit la liste d'états.
     */
    private List<State> convertStates(
        List<WorkflowState> workflowStates,
        ApiStructure apiStructure
    ) {
        if (workflowStates == null || workflowStates.isEmpty()) {
            log.warn("⚠️ No states to convert");
            return new ArrayList<>();
        }
        
        return workflowStates.stream()
            .map(ws -> convertState(ws, apiStructure))
            .collect(Collectors.toList());
    }
    
    /**
     * Convertit un état individuel.
     */
    private State convertState(WorkflowState ws, ApiStructure apiStructure) {
        log.debug("Converting state: {} ({})", ws.getName(), ws.getType());
        
        State state = new State();
        
        state.setId(ws.getId());
        state.setName(ws.getName());
        state.setType(parseStateType(ws.getType()));
        state.setIsInitial(ws.is_initial());
        state.setMessage(ws.getMessage());
        
        // Validation et storeAs (pour états INPUT)
        if (ws.getParameter_name() != null && !ws.getParameter_name().trim().isEmpty()) {
            state.setValidation(buildValidation(ws));
            state.setStoreAs(ws.getParameter_name());
        }
        
        // Action (pour états PROCESSING)
        if (ws.getLinked_endpoint() != null && !ws.getLinked_endpoint().trim().isEmpty()) {
            state.setAction(buildAction(ws, apiStructure));
        }
        
        // Transitions
        state.setTransitions(convertTransitions(ws.getTransitions()));
        
        return state;
    }
    
    /**
     * Parse le type d'état.
     */
    private StateType parseStateType(StateType type) {
        if (type == null) {
            return StateType.MENU;
        }
        
        return switch (type) {
            case MENU -> StateType.MENU;
            case INPUT -> StateType.INPUT;
            case PROCESSING -> StateType.PROCESSING;
            case DISPLAY -> StateType.DISPLAY;
            case FINAL -> StateType.FINAL;
            default -> {
                log.warn("⚠️ Unknown state type: {}, defaulting to MENU", type);
                yield StateType.MENU;
            }
        };
    }
    
    /**
     * Construit ValidationRule depuis WorkflowState.
     */
    private ValidationRule buildValidation(WorkflowState ws) {
        ValidationRule rule = new ValidationRule();
        
        String message = ws.getMessage() != null ? ws.getMessage().toLowerCase() : "";
        String paramName = ws.getParameter_name() != null ? ws.getParameter_name().toLowerCase() : "";
        
        // Inférer le type de validation depuis le contexte
        if (message.contains("id") || paramName.contains("id")) {
            // Validation numérique pour les IDs
            rule.setType(ValidationType.NUMERIC);
            rule.setMin(1.0);
            rule.setMax(9999.0);
        } else if (message.contains("phone") || paramName.contains("phone")) {
            // Validation pour numéros de téléphone
            rule.setType(ValidationType.PHONE);
        } else if (message.contains("email") || paramName.contains("email")) {
            // Validation pour emails
            rule.setType(ValidationType.CUSTOM);
            rule.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        } else {
            // Validation texte par défaut
            rule.setType(ValidationType.TEXT);
            rule.setMinLength(3);
            rule.setMaxLength(100);
        }
        
        rule.setOptional(false);
        
        return rule;
    }
    
    /**
     * Construit Action depuis WorkflowState et ApiStructure.
     */
    private Action buildAction(WorkflowState ws, ApiStructure apiStructure) {
        // Parser le linked_endpoint: "GET /todos" ou "POST /todos"
        String linkedEndpoint = ws.getLinked_endpoint().trim();
        String[] parts = linkedEndpoint.split("\\s+", 2);
        
        HttpMethod method;
        String endpoint;
        
        if (parts.length == 2) {
            method = parseHttpMethod(parts[0]);
            endpoint = parts[1];
        } else {
            // Fallback: juste un endpoint sans méthode
            method = HttpMethod.GET;
            endpoint = linkedEndpoint;
        }
        
        log.debug("Building action: {} {}", method, endpoint);
        
        // Construire l'action
        Action action = Action.builder()
            .type(ActionType.API_CALL)
            .method(method)
            .endpoint(endpoint)
            .headers(buildActionHeaders(method))
            .body(buildActionBody(method, ws))
            .build();
        
        // OnSuccess
        ActionResult onSuccess = new ActionResult();
        onSuccess.setNextState(findNextState(ws.getTransitions(), "SUCCESS"));
        onSuccess.setResponseMapping(buildResponseMapping(ws, method));
        action.setOnSuccess(onSuccess);
        
        // OnError
        ActionResult onError = new ActionResult();
        onError.setNextState(findNextState(ws.getTransitions(), "ERROR"));
        onError.setMessage("Operation failed. Try again.");
        action.setOnError(onError);
        
        return action;
    }
    
    /**
     * Parse HttpMethod depuis string (CORRIGÉ).
     */
    private HttpMethod parseHttpMethod(String methodStr) {
        if (methodStr == null) {
            return HttpMethod.GET;
        }
        
        // Comparer avec .name() puisque HttpMethod est un enum
        String upperMethod = methodStr.toUpperCase();
        
        for (HttpMethod method : HttpMethod.values()) {
            if (method.name().equals(upperMethod)) {
                return method;
            }
        }
        
        log.warn("⚠️ Unknown HTTP method: {}, defaulting to GET", methodStr);
        return HttpMethod.GET;
    }
    
    /**
     * Construit les headers pour l'action.
     */
    private Map<String, String> buildActionHeaders(HttpMethod method) {
        Map<String, String> headers = new HashMap<>();
        
        // Vérifier par nom puisque c'est un enum
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            headers.put("Content-Type", "application/json");
        }
        
        return headers;
    }
    
    /**
     * Construit le body pour l'action.
     */
    private Map<String, Object> buildActionBody(HttpMethod method, WorkflowState ws) {
        Map<String, Object> body = new HashMap<>();
        
        // Seulement pour POST, PUT, PATCH
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            // Si on a un parameter_name, l'utiliser comme template
            if (ws.getParameter_name() != null) {
                body.put("value", "{{" + ws.getParameter_name() + "}}");
            }
        }
        
        return body;
    }
    
    /**
     * Construit le responseMapping.
     */
    private Map<String, String> buildResponseMapping(WorkflowState ws, HttpMethod method) {
        Map<String, String> mapping = new HashMap<>();
        
        // Mapping par défaut basé sur la méthode HTTP
        if (method == HttpMethod.GET) {
            mapping.put("items", "data");
            mapping.put("list", "data");
        } else if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            mapping.put("id", "data.id");
            mapping.put("result", "data");
            
            // Si on a un parameter_name, mapper aussi ce champ
            if (ws.getParameter_name() != null) {
                mapping.put(ws.getParameter_name(), "data." + ws.getParameter_name());
            }
        }
        
        return mapping;
    }
    
    /**
     * Trouve le nextState pour une condition donnée.
     */
    private String findNextState(List<WorkflowTransition> transitions, String condition) {
        if (transitions == null) {
            return null;
        }
        
        return transitions.stream()
            .filter(t -> condition.equals(t.getCondition()))
            .map(WorkflowTransition::getNextState)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Convertit les transitions.
     */
    private List<Transition> convertTransitions(List<WorkflowTransition> workflowTransitions) {
        if (workflowTransitions == null) {
            return new ArrayList<>();
        }
        
        return workflowTransitions.stream()
            .map(this::convertTransition)
            .collect(Collectors.toList());
    }
    
    /**
     * Convertit une transition individuelle.
     */
    private Transition convertTransition(WorkflowTransition wt) {
        Transition t = new Transition();
        t.setInput(wt.getInput());
        t.setCondition(wt.getCondition());
        t.setNextState(wt.getNextState());
        t.setMessage(wt.getMessage());
        return t;
    }
}