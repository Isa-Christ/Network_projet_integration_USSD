package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.enums.StateType;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowTransition;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposal;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposals;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowState;
import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
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

        // Pour chaque endpoint, créer des états simples (Max 5 pour éviter surcharge)
        int option_number = 1;
        for (Endpoint endpoint : api_structure.getEndpoints().values()) {
            if (option_number > 5)
                break;

            // Ajouter transition au menu principal
            WorkflowTransition transition = WorkflowTransition.builder()
                    .input(String.valueOf(option_number))
                    .nextState(String.valueOf(state_id))
                    .build();

            if (main_menu.getTransitions() == null)
                main_menu.setTransitions(new ArrayList<>());
            main_menu.getTransitions().add(transition);

            // Créer états pour cet endpoint (Input -> Process -> Display)
            states.addAll(generateStatesForEndpoint(endpoint, state_id));

            option_number++;
            state_id += 3; // Réserver 3 IDs par endpoint
        }

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
                .name("Standard (Mode Secours)")
                .description("Flux généré automatiquement (IA indisponible)")
                .complexity("medium")
                .states(states)
                .estimated_states_count(states.size())
                .reasoning(
                        "Ce workflow a été généré sans IA suite à une erreur technique. Vous pouvez l'éditer manuellement.")
                .build();
    }

    private String buildMainMenuMessage(ApiStructure api_structure, GenerationHints hints) {
        StringBuilder message = new StringBuilder();
        message.append("=== ").append(hints.getService_name()).append(" ===\n\n"); // Titre clair avec vrais sauts de
                                                                                   // ligne

        int option = 1;
        for (Endpoint endpoint : api_structure.getEndpoints().values()) {
            if (option > 5)
                break;

            String action_text = getActionText(endpoint);
            message.append(option).append(". ").append(action_text).append("\n");
            option++;
        }

        message.append("\n0. Quitter"); // Séparateur
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

    private List<WorkflowState> generateStatesForEndpoint(Endpoint endpoint, int start_id) {
        List<WorkflowState> states = new ArrayList<>();

        // 1. État PROCESSING (Appel API)
        WorkflowState processing_state = WorkflowState.builder()
                .id(String.valueOf(start_id))
                .name("Process_" + sanitize(endpoint.getOperation_id()))
                .type(StateType.PROCESSING)
                .message("Traitement en cours...")
                .linked_endpoint(endpoint.getOperation_id())
                .transitions(new ArrayList<>())
                .build();

        // Transition vers affichage si succès
        WorkflowTransition success_transition = WorkflowTransition.builder()
                .condition("SUCCESS")
                .nextState(String.valueOf(start_id + 1))
                .build();
        processing_state.getTransitions().add(success_transition);

        // Transition vers menu si erreur
        WorkflowTransition error_transition = WorkflowTransition.builder()
                .condition("ERROR")
                .nextState("1") // Retour menu
                .build();
        processing_state.getTransitions().add(error_transition);

        states.add(processing_state);

        // 2. État DISPLAY (Résultat)
        WorkflowState display_state = WorkflowState.builder()
                .id(String.valueOf(start_id + 1))
                .name("Display_" + sanitize(endpoint.getOperation_id()))
                .type(StateType.DISPLAY)
                .message("Résultat: {{data}}\\n\\n0. Retour")
                .linked_endpoint(null)
                .transitions(new ArrayList<>())
                .build();

        WorkflowTransition back_transition = WorkflowTransition.builder()
                .input("0")
                .nextState("1") // Retour menu
                .build();
        display_state.getTransitions().add(back_transition);

        states.add(display_state);

        return states;
    }

    private String sanitize(String s) {
        return s == null ? "Unknown" : s.replaceAll("[^a-zA-Z0-9]", "");
    }
}