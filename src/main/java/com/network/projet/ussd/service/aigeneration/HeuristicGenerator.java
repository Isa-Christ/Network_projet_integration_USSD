package com.network.projet.ussd.service.aigeneration;

//import com.network.projet.ussd.domain.enums.EndpointType;
//import com.network.projet.ussd.domain.enums.ProposalComplexity;
import com.network.projet.ussd.domain.enums.StateType;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposal;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposals;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowState;
import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowState;
import com.network.projet.ussd.domain.model.aigeneration.GenerationHints;
import com.network.projet.ussd.domain.model.aigeneration.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Générateur heuristique (fallback si LLM échoue).
 * 
 * @author Your Name
 * @since 2025-01-26
 */
@Service
@Slf4j
public class HeuristicGenerator {
    
    /**
     * Génère un workflow basique sans LLM.
     */
    public Mono<WorkflowProposals> generateBasicWorkflow(ApiStructure api_structure, GenerationHints hints) {
        return Mono.fromCallable(() -> {
            log.info("Generating basic workflow heuristically");
            
            WorkflowProposals proposals = WorkflowProposals.builder()
                .service_name(hints.getService_name())
                .proposals(new ArrayList<>())
                .menu_texts(new HashMap<>())
                .response_summaries(new HashMap<>())
                .input_states(new HashMap<>())
                .build();
            
            // Générer une seule proposition "Standard"
            WorkflowProposal standard_proposal = generateStandardProposal(api_structure, hints);
            proposals.getProposals().add(standard_proposal);
            
            return proposals;
        });
    }
    
    private WorkflowProposal generateStandardProposal(ApiStructure api_structure, GenerationHints hints) {
        List<WorkflowState> states = new ArrayList<>();
        int state_id = 1;
        
        // État initial: Menu principal
        WorkflowState main_menu = WorkflowState.builder()
            .id(String.valueOf(state_id++))
            .name("MainMenu")
            .type(StateType.MENU)
            .is_initial(true)
            .message(buildMainMenuMessage(api_structure, hints))
            .transitions(new ArrayList<>())
            .build();
        
        states.add(main_menu);
        
        // Pour chaque endpoint, créer des états simples
        // int option_number = 1;
        // for (Endpoint endpoint : api_structure.getEndpoints().values()) {
        //     if (option_number > 7) break; // Max 7 options
            
        //     // Ajouter transition au menu principal
        //     Map<String, String> transition = new HashMap<>();
        //     transition.put("input", String.valueOf(option_number));
        //     transition.put("nextState", String.valueOf(state_id));
        //     main_menu.getTransitions().addAll(transition);
            
        //     // Créer états pour cet endpoint
        //     states.addAll(generateStatesForEndpoint(endpoint, state_id, api_structure));
            
        //     option_number++;
        //     state_id += 3; // Réserver 3 IDs par endpoint
        // }
        
        // État de sortie
        WorkflowState exit_state = WorkflowState.builder()
            .id("99")
            .name("Exit")
            .type(StateType.FINAL)
            .message("Merci d'avoir utilisé " + hints.getService_name())
            .transitions(new ArrayList<>())
            .build();
        
        states.add(exit_state);
        
        return WorkflowProposal.builder()
            .name("Standard")
            .description("Flux généré automatiquement (heuristique)")
            .complexity("medium")
            .states(states)
            .estimated_states_count(states.size())
            .reasoning("Généré par heuristique car LLM indisponible")
            .build();
    }
    
    private String buildMainMenuMessage(ApiStructure api_structure, GenerationHints hints) {
        StringBuilder message = new StringBuilder(hints.getService_name()).append("\n");
        
        int option = 1;
        for (Endpoint endpoint : api_structure.getEndpoints().values()) {
            if (option > 7) break;
            
            String action_text = getActionText(endpoint);
            message.append(option).append(". ").append(action_text).append("\n");
            option++;
        }
        
        message.append("0. Quitter");
        return message.toString();
    }
    
    private String getActionText(Endpoint endpoint) {
        String summary = endpoint.getSummary() != null ? endpoint.getSummary() : endpoint.getOperation_id();
        
        return switch (endpoint.getType()) {
            case LIST -> "Voir " + summary;
            case READ_DETAIL -> "Consulter " + summary;
            case CREATE -> "Créer " + summary;
            case UPDATE -> "Modifier " + summary;
            case DELETE -> "Supprimer " + summary;
            default -> summary;
        };
    }
    
    private List<WorkflowState> generateStatesForEndpoint(Endpoint endpoint, int start_id, ApiStructure api_structure) {
        List<WorkflowState> states = new ArrayList<>();
        
        // Si l'endpoint a des paramètres requis, créer état INPUT
        // if (endpoint.hasRequiredParameters()) {
        //     WorkflowState input_state = WorkflowState.builder()
        //         .id(String.valueOf(start_id))
        //         .name("Input" + endpoint.getOperation_id())
        //         .type(StateType.INPUT)
        //         .message("Entrez " + endpoint.getSummary() + ":")
        //         .linked_endpoint(endpoint.getOperation_id())
        //         .transitions(new ArrayList<>())
        //         .build();
            
        //     Map<String, String> transition = new HashMap<>();
        //     transition.put("condition", "VALID");
        //     transition.put("nextState", String.valueOf(start_id + 1));
        //     input_state.getTransitions().add(transition);
            
        //     states.add(input_state);
        // }
        
        // État PROCESSING pour appel API
        WorkflowState processing_state = WorkflowState.builder()
            .id(String.valueOf(start_id + 1))
            .name("Process" + endpoint.getOperation_id())
            .type(StateType.PROCESSING)
            .message("Traitement en cours...")
            .linked_endpoint(endpoint.getOperation_id())
            .transitions(new ArrayList<>())
            .build();
        
        Map<String, String> success_transition = new HashMap<>();
        // success_transition.put("condition", "SUCCESS");
        // success_transition.put("nextState", String.valueOf(start_id + 2));
        // processing_state.getTransitions().add(success_transition);
        
        states.add(processing_state);
        
        // État DISPLAY pour résultat
        WorkflowState display_state = WorkflowState.builder()
            .id(String.valueOf(start_id + 2))
            .name("Display" + endpoint.getOperation_id())
            .type(StateType.DISPLAY)
            .message("Résultat: {{data}}\n\n0. Retour")
            .linked_endpoint(endpoint.getOperation_id())
            .transitions(new ArrayList<>())
            .build();
        
        Map<String, String> back_transition = new HashMap<>();
        // back_transition.put("input", "0");
        // back_transition.put("nextState", "1");
        // display_state.getTransitions().add(back_transition);
        
        states.add(display_state);
        
        return states;
    }
}