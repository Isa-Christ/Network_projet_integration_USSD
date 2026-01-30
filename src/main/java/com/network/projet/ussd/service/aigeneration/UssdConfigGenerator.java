// package com.network.projet.ussd.service.aigeneration;

// import com.network.projet.ussd.domain.enums.HttpMethod;
// import com.network.projet.ussd.domain.enums.StateType;
// import com.network.projet.ussd.domain.enums.ValidationType;
// import com.network.projet.ussd.domain.model.aigeneration.*;
// import com.network.projet.ussd.domain.model.automaton.*;
// import com.network.projet.ussd.util.StateIdGenerator;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
// import reactor.core.publisher.Mono;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// /**
//  * Générateur de configuration USSD finale.
//  * 
//  * @author Your Name
//  * @since 2025-01-26
//  */
// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class UssdConfigGenerator {
    
//     private final StateIdGenerator id_generator = new StateIdGenerator();
    
//     /**
//      * Génère la configuration USSD finale depuis une proposition.
//      */
//     public Mono<AutomatonDefinition> generateConfig(
//         ApiStructure api_structure,
//         WorkflowProposals workflow_proposals,
//         int selected_proposal_index
//     ) {
//         return Mono.fromCallable(() -> {
//             log.info("Generating final USSD config from proposal {}", selected_proposal_index);
            
//             if (selected_proposal_index >= workflow_proposals.getProposals().size()) {
//                 throw new IllegalArgumentException("Index de proposition invalide");
//             }
            
//             WorkflowProposal selected_proposal = workflow_proposals.getProposals().get(selected_proposal_index);
            
//             AutomatonDefinition config = new AutomatonDefinition();
//             config.setServiceCode(generateServiceCode(workflow_proposals.getService_name()));
//             config.setServiceName(workflow_proposals.getService_name());
//             config.setVersion("1.0.0");
//             config.setDescription("Généré automatiquement par AI Generator");
            
//             // API Config
//             config.setApiConfig(buildApiConfig(api_structure));
            
//             // Session Config
//             config.setSessionConfig(buildSessionConfig());
            
//             // States
//             List<State> states = convertProposalStates(
//                 selected_proposal.getStates(),
//                 api_structure,
//                 workflow_proposals
//             );
//             config.setStates(states);
            
//             log.info("Config generated: {} states", states.size());
//             return config;
//         });
//     }
    
//     private String generateServiceCode(String service_name) {
//         return service_name.toLowerCase()
//             .replaceAll("[^a-z0-9]+", "-")
//             .replaceAll("^-|-$", "");
//     }
    
//     private ApiConfig buildApiConfig(ApiStructure api_structure) {
//         ApiConfig api_config = new ApiConfig();
//         api_config.setBaseUrl(api_structure.getBase_url());
//         api_config.setTimeout((int) 10000L);
//         api_config.setRetryAttempts(2);
        
//         Authentication auth = new Authentication();
//         auth.setType(com.network.projet.ussd.domain.enums.AuthenticationType.NONE);
//         api_config.setAuthentication(auth);
        
//         return api_config;
//     }
    
//     private SessionConfig buildSessionConfig() {
//         SessionConfig session_config = new SessionConfig();
//         session_config.setTimeoutSeconds(60);
//         session_config.setMaxInactivitySeconds(30);
//         return session_config;
//     }
    
//     private List<State> convertProposalStates(
//         List<StateProposal> proposals,
//         ApiStructure api_structure,
//         WorkflowProposals workflow_proposals
//     ) {
//         List<State> states = new ArrayList<>();
        
//         for (StateProposal proposal : proposals) {
//             State state = new State();
//             state.setId(proposal.getId());
//             state.setName(proposal.getName());
//             state.setType(proposal.getType());
//             state.setIsInitial(proposal.is_initial());
//             state.setMessage(proposal.getMessage());
            
//             // Transitions
//             state.setTransitions(convertTransitions(proposal.getTransitions()));
            
//             // Si PROCESSING, ajouter action API
//             if (proposal.getType() == StateType.PROCESSING && proposal.getLinked_endpoint() != null) {
//                 Endpoint endpoint = api_structure.getEndpoints().get(proposal.getLinked_endpoint());
//                 if (endpoint != null) {
//                     state.setAction(buildApiAction(endpoint, workflow_proposals));
//                 }
//             }
            
//             // Si INPUT, ajouter validation
//             if (proposal.getType() == StateType.INPUT && proposal.getParameter_name() != null) {
//                 state.setStoreAs(proposal.getParameter_name());
//                 state.setValidation(buildValidation(proposal, workflow_proposals));
//             }
            
//             states.add(state);
//         }
        
//         return states;
//     }
    
//     private List<Transition> convertTransitions(List<Map<String, String>> proposal_transitions) {
//         List<Transition> transitions = new ArrayList<>();
        
//         for (Map<String, String> prop_trans : proposal_transitions) {
//             Transition transition = new Transition();
//             transition.setInput(prop_trans.get("input"));
//             transition.setCondition(prop_trans.get("condition"));
//             transition.setNextState(prop_trans.get("nextState"));
//             transition.setMessage(prop_trans.get("message"));
//             transitions.add(transition);
//         }
        
//         return transitions;
//     }
    
//     private Action buildApiAction(Endpoint endpoint, WorkflowProposals workflow_proposals) {
//         Action action = new Action();
//         action.setType(com.network.projet.ussd.domain.enums.ActionType.API_CALL);
//         action.setMethod(endpoint.getMethod());
//         action.setEndpoint(endpoint.getPath());
        
//         // Headers
//         if (endpoint.getMethod() == HttpMethod.POST || 
//             endpoint.getMethod() == HttpMethod.PUT ||
//             endpoint.getMethod() == HttpMethod.PATCH) {
//             Map<String, String> headers = new HashMap<>();
//             headers.put("Content-Type", "application/json");
//             action.setHeaders(headers);
//         }
        
//         // Body (si POST/PUT/PATCH)
//         if (endpoint.isHas_request_body()) {
//             action.setBody(new HashMap<>());
//         }
        
//         // OnSuccess
//         ActionResult on_success = new ActionResult();
//         on_success.setSuccess(true);
//         on_success.setResponseMapping(new HashMap<>());
//         on_success.getResponseMapping().put("data", "data");
//         action.setOnSuccess(on_success);
        
//         // OnError
//         ActionResult on_error = new ActionResult();
//         on_error.setSuccess(false);
//         on_error.setMessage("Erreur lors de l'opération");
//         action.setOnError(on_error);
        
//         return action;
//     }
    
//     private ValidationRule buildValidation(StateProposal proposal, WorkflowProposals workflow_proposals) {
//         ValidationRule rule = new ValidationRule();
        
//         // Chercher la config d'input dans workflow_proposals
//         if (workflow_proposals.getInput_states().containsKey(proposal.getLinked_endpoint())) {
//             List<InputConfig> input_configs = workflow_proposals.getInput_states().get(proposal.getLinked_endpoint());
            
//             InputConfig matching_config = input_configs.stream()
//                 .filter(ic -> ic.getParameter().equals(proposal.getParameter_name()))
//                 .findFirst()
//                 .orElse(null);
            
//             if (matching_config != null) {
//                 rule.setType(mapToValidationType(matching_config.getValidation()));
//                 rule.setMinLength(matching_config.getMin_length());
//                 rule.setMaxLength(matching_config.getMax_length());
//             }
//         }
        
//         // Fallback : validation TEXT par défaut
//         if (rule.getType() == null) {
//             rule.setType(ValidationType.TEXT);
//         }
        
//         return rule;
//     }
    
//     private ValidationType mapToValidationType(String validation_string) {
//         if (validation_string == null) return ValidationType.TEXT;
        
//         return switch (validation_string.toUpperCase()) {
//             case "TEXT" -> ValidationType.TEXT;
//             case "NUMERIC", "NUMBER" -> ValidationType.NUMERIC;
//             case "PHONE" -> ValidationType.PHONE;
//             case "EMAIL" -> ValidationType.EMAIL;
//             case "DECIMAL", "AMOUNT" -> ValidationType.DECIMAL;
//             case "ALPHANUMERIC" -> ValidationType.ALPHANUMERIC;
//             case "NAME" -> ValidationType.NAME;
//             default -> ValidationType.TEXT;
//         };
//     }
// }


package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.aigeneration.*;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.exception.AiGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Génère la configuration USSD finale (AutomatonDefinition).
 * 
 * @author Network Project Team
 * @since 2025-01-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UssdConfigGenerator {
    
    private final WorkflowToAutomatonConverter converter;
    
    /**
     * Génère AutomatonDefinition depuis une proposition sélectionnée.
     * 
     * @param apiStructure Structure de l'API analysée
     * @param proposals Propositions de workflows générées par l'IA
     * @param selectedIndex Index de la proposition à utiliser (0-based)
     * @return AutomatonDefinition complète prête à l'emploi
     */
    public Mono<AutomatonDefinition> generateConfig(
        ApiStructure apiStructure,
        WorkflowProposals proposals,
        int selectedIndex
    ) {
        return Mono.fromCallable(() -> {
            log.info("📝 Generating AutomatonDefinition from proposal index {}", selectedIndex);
            
            // Validation des paramètres
            if (apiStructure == null) {
                throw new AiGenerationException("ApiStructure cannot be null");
            }
            
            if (proposals == null || proposals.getProposals() == null) {
                throw new AiGenerationException("WorkflowProposals cannot be null");
            }
            
            if (proposals.getProposals().isEmpty()) {
                throw new AiGenerationException("No workflow proposals available");
            }
            
            // Valider l'index
            if (selectedIndex < 0 || selectedIndex >= proposals.getProposals().size()) {
                throw new AiGenerationException(
                    String.format("Invalid proposal index %d. Available: 0-%d", 
                                 selectedIndex, 
                                 proposals.getProposals().size() - 1)
                );
            }
            
            // Récupérer la proposition sélectionnée
            WorkflowProposal selected = proposals.getProposals().get(selectedIndex);
            
            log.info("✅ Selected proposal: '{}' ({} states)", 
                    selected.getName(), 
                    selected.getStates() != null ? selected.getStates().size() : 0);
            
            // Créer les hints depuis les proposals
            GenerationHints hints = GenerationHints.builder()
                .service_name(proposals.getService_name())
                .build();
            
            // Convertir en AutomatonDefinition
            AutomatonDefinition automaton = converter.convert(selected, apiStructure, hints);
            
            // Validation finale
            validateAutomaton(automaton);
            
            log.info("✅ AutomatonDefinition generated successfully: {} states", 
                    automaton.getStates().size());
            
            return automaton;
        });
    }
    
    /**
     * Valide qu'une AutomatonDefinition est complète et cohérente.
     */
    private void validateAutomaton(AutomatonDefinition automaton) {
        if (automaton.getServiceCode() == null || automaton.getServiceCode().trim().isEmpty()) {
            throw new AiGenerationException("ServiceCode cannot be empty");
        }
        
        if (automaton.getStates() == null || automaton.getStates().isEmpty()) {
            throw new AiGenerationException("AutomatonDefinition must have at least one state");
        }
        
        // Vérifier qu'il y a un état initial
        long initialStatesCount = automaton.getStates().stream()
            .filter(s -> s.getIsInitial() != null && s.getIsInitial())
            .count();
        
        if (initialStatesCount == 0) {
            throw new AiGenerationException("No initial state found");
        }
        
        if (initialStatesCount > 1) {
            log.warn("⚠️ Multiple initial states found ({}), only first will be used", initialStatesCount);
        }
        
        // Vérifier que toutes les transitions pointent vers des états existants
        validateTransitions(automaton);
    }
    
    /**
     * Valide que toutes les transitions pointent vers des états existants.
     */
    private void validateTransitions(AutomatonDefinition automaton) {
        var stateIds = automaton.getStates().stream()
            .map(s -> s.getId())
            .collect(java.util.stream.Collectors.toSet());
        
        automaton.getStates().forEach(state -> {
            if (state.getTransitions() != null) {
                state.getTransitions().forEach(transition -> {
                    String nextState = transition.getNextState();
                    if (nextState != null && !stateIds.contains(nextState)) {
                        log.warn("⚠️ State '{}' has transition to non-existent state '{}'", 
                                state.getId(), nextState);
                    }
                });
            }
        });
    }
}