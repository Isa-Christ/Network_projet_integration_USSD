package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.GenerationHints;
import org.springframework.stereotype.Component;

/**
 * Construction de prompts pour le LLM.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
Tu es un expert en conception de flux USSD conversationnels et en architecture d'APIs REST.

CONTRAINTES USSD STRICTES:
- Maximum 182 caractères par écran
- Navigation par chiffres (0-9) uniquement
- Menus simples et directs (max 7 options)
- Messages clairs et concis
- Transitions logiques entre états

RÈGLE CRITIQUE DE RÉPONSE:
- Réponds UNIQUEMENT en JSON valide
- AUCUN texte explicatif avant ou après le JSON
- AUCUN markdown (pas de ```json ou ```)
- Le JSON doit être directement parsable
- Respecte EXACTEMENT la structure fournie

TYPES D'ÉTATS DISPONIBLES:
1. MENU: Présente des options numérotées à l'utilisateur
2. INPUT: Collecte une donnée utilisateur (avec validation)
3. PROCESSING: Exécute un appel API (GET, POST, PATCH, DELETE)
4. DISPLAY: Affiche des résultats ou informations
5. FINAL: État terminal (sortie ou fin d'un flux)
""";

    /**
     * Construit le prompt pour génération de workflows.
     */
    public String buildWorkflowGenerationPrompt(ApiStructure api_structure, GenerationHints hints) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("=== MISSION ===\n");
        prompt.append("Génère un service USSD complet pour: ").append(hints.getService_name()).append("\n");
        prompt.append("Langue des messages: ").append(hints.getTargetLanguageOrDefault()).append("\n\n");
        
        // Entités métier
        prompt.append("=== ENTITÉS MÉTIER ===\n");
        if (!hints.getPrimary_entities().isEmpty()) {
            prompt.append("Entités principales: ").append(String.join(", ", hints.getPrimary_entities())).append("\n");
        } else if (hints.getPrimary_entity() != null) {
            prompt.append("Entité principale: ").append(hints.getPrimary_entity()).append("\n");
        }
        
        // États critiques obligatoires
        if (!hints.getCritical_states().isEmpty()) {
            prompt.append("\n⚠️ ÉTATS OBLIGATOIRES à inclure:\n");
            hints.getCritical_states().forEach(state -> 
                prompt.append("  • ").append(state).append("\n")
            );
        }
        
        // Complexité
        String complexity = hints.getComplexity() != null ? hints.getComplexity().toLowerCase() : "medium";
        prompt.append("\nNiveau de complexité: ").append(complexity).append("\n\n");
        
        // Endpoints disponibles
        prompt.append("=== ENDPOINTS API DISPONIBLES ===\n");
        prompt.append("Base URL: ").append(api_structure.getBase_url()).append("\n\n");
        api_structure.getEndpoints().forEach((id, endpoint) -> {
            prompt.append(String.format("• %s %s\n", endpoint.getMethod(), endpoint.getPath()));
            prompt.append(String.format("  Description: %s\n", endpoint.getSummary()));
            if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
                prompt.append("  Paramètres: ");
                endpoint.getParameters().forEach((param) -> 
                    prompt.append(String.format("%s (%s, %s) ", 
                        param.getName(), 
                        param.getType(), 
                        param.isRequired() ? "obligatoire" : "optionnel"))
                );
                prompt.append("\n");
            }
            prompt.append("\n");
        });
        
        // Architecture du workflow
        prompt.append("=== ARCHITECTURE DU WORKFLOW ===\n");
        prompt.append("Conçois un flux logique avec ces étapes:\n\n");
        
        prompt.append("1. MENU PRINCIPAL (État initial, type: MENU)\n");
        prompt.append("   - Présenter toutes les actions disponibles\n");
        prompt.append("   - Options: Créer, Lister, Modifier, Supprimer, Quitter\n");
        prompt.append("   - Toujours inclure option '0' pour sortir\n\n");
        
        prompt.append("2. COLLECTE DE DONNÉES (type: INPUT)\n");
        prompt.append("   - Un état INPUT par paramètre à collecter\n");
        prompt.append("   - Définir validation appropriée (TEXT, NUMERIC, etc.)\n");
        prompt.append("   - Stocker dans 'storeAs' pour réutilisation\n");
        prompt.append("   - Gérer transitions VALID/INVALID\n\n");
        
        prompt.append("3. APPELS API (type: PROCESSING)\n");
        prompt.append("   - Mapper chaque opération CRUD à son endpoint\n");
        prompt.append("   - GET: récupérer des données\n");
        prompt.append("   - POST: créer une ressource\n");
        prompt.append("   - PATCH/PUT: modifier une ressource\n");
        prompt.append("   - DELETE: supprimer une ressource\n");
        prompt.append("   - Définir onSuccess et onError avec nextState\n");
        prompt.append("   - Utiliser responseMapping pour stocker les réponses\n\n");
        
        prompt.append("4. AFFICHAGE RÉSULTATS (type: DISPLAY)\n");
        prompt.append("   - Afficher les données récupérées ou confirmations\n");
        prompt.append("   - Utiliser templates Handlebars: {{variable}}, {{#each items}}\n");
        prompt.append("   - Proposer navigation: retour menu, action suivante\n\n");
        
        prompt.append("5. CONFIRMATIONS (type: MENU optionnel)\n");
        prompt.append("   - Pour actions destructives (DELETE), ajouter confirmation\n");
        prompt.append("   - Ex: \"Êtes-vous sûr? 1. Oui 2. Non\"\n\n");
        
        prompt.append("6. ÉTATS FINAUX (type: FINAL)\n");
        prompt.append("   - Messages de succès/confirmation\n");
        prompt.append("   - Proposer actions suivantes ou retour menu\n");
        prompt.append("   - État de sortie (id: 99) sans transitions\n\n");
        
        // Règles de conception
        prompt.append("=== RÈGLES DE CONCEPTION ===\n");
        prompt.append("- IDs d'états: numérotation séquentielle (1, 2, 3..., 99 pour sortie)\n");
        prompt.append("- Un seul état avec is_initial: true\n");
        prompt.append("- Tous les états doivent être accessibles (pas d'états orphelins)\n");
        prompt.append("- Messages USSD ≤ 182 caractères MAXIMUM\n");
        prompt.append("- Transitions cohérentes: chaque input/condition doit avoir un nextState valide\n");
        prompt.append("- Variables stockées: utiliser storeAs puis {{variableName}} dans templates\n");
        prompt.append("- Gestion erreurs: toujours définir onError avec message et nextState\n\n");

        prompt.append("Tu es un générateur JSON strict.  \n");
        prompt.append("***********RÈGLE ABSOLUE********** :\n");
        prompt.append("   -  TA RÉPONSE DOIT ÊTRE UNIQUEMENT un objet JSON valide.\n");
        prompt.append("   - AUCUN texte, AUCUNE phrase, AUCUN mot avant le {.\n");
        prompt.append("   - AUCUN markdown, pas de ```json, pas de \"Voici\", pas d'explication.\n");
        prompt.append("   - Si tu ajoutes du texte, tu échoues complètement.\n");
        prompt.append("   - - Commence directement par {. \n");
        
        // Adapter selon complexité
        switch (complexity) {
            case "low" -> {
                prompt.append(" INSTRUCTIONS (Mode Simple):\n");
                prompt.append("- Génère un workflow minimal: 5-8 états\n");
                prompt.append("- Opérations basiques: Lister + Créer uniquement\n");
                prompt.append("- Pas de confirmations ou validations complexes\n");
                prompt.append("- Messages courts, menus avec 3-4 options max\n\n");
            }
            case "high" -> {
                prompt.append(" INSTRUCTIONS (Mode Détaillé):\n");
                prompt.append("- Génère un workflow complet: 12-20 états\n");
                prompt.append("- Toutes les opérations CRUD\n");
                prompt.append("- Validations strictes sur tous les inputs\n");
                prompt.append("- Confirmations avant actions destructives\n");
                prompt.append("- Gestion d'erreurs robuste\n");
                prompt.append("- Messages détaillés avec feedback précis\n\n");
            }
            default -> {
                prompt.append(" INSTRUCTIONS (Mode Standard):\n");
                prompt.append("- Génère un workflow équilibré: 8-12 états\n");
                prompt.append("- Opérations principales: Lister, Créer, Modifier, Supprimer\n");
                prompt.append("- Validations de base sur inputs critiques\n");
                prompt.append("- Confirmation pour suppressions uniquement\n");
                prompt.append("- Gestion d'erreurs standard\n\n");
            }
        }
        
        // Structure JSON exacte attendue
        prompt.append("=== STRUCTURE JSON EXACTE À PRODUIRE ===\n");
        prompt.append(getExpectedJsonStructure());

        prompt.append("\n⚠️ GÉNÈRE LA CONFIG FINALE COMPLÈTE (pas de proposals intermédiaire !)\n");
        prompt.append("Utilise la baseUrl de l'API fournie.\n");
        prompt.append("Adapte les endpoints aux opérations CRUD de l'API.\n");

        prompt.append("\n=== CHECKLIST FINALE AVANT GÉNÉRATION ===\n");
        prompt.append("Vérifie que ton JSON contient:\n");
        prompt.append("☐ serviceCode, serviceName, version, description\n");
        prompt.append("☐ apiConfig avec baseUrl, timeout, retryAttempts, authentication\n");
        prompt.append("☐ sessionConfig avec timeoutSeconds, maxInactivitySeconds\n");
        prompt.append("☐ states: un tableau d'objets (PAS de strings)\n");
        prompt.append("☐ Chaque état a: id, name, type, message, transitions\n");
        prompt.append("☐ États INPUT ont: validation, storeAs\n");
        prompt.append("☐ États PROCESSING ont: action avec type, method, endpoint, onSuccess, onError\n");
        prompt.append("☐ Ceci est très important : Premier état a 'is_initial': true\n");
        prompt.append("☐ État 99 est de type FINAL avec transitions vides\n");
        prompt.append("☐ Toutes les transitions pointent vers des états existants\n");
        prompt.append("☐ Messages ≤ 182 caractères\n");
        prompt.append("☐ Variables utilisées sont bien stockées dans storeAs\n\n");
        
        prompt.append("⚠️ RAPPEL FINAL: Génère UNIQUEMENT le JSON, sans texte avant/après, sans markdown.\n");
        
        return prompt.toString();
    }

    public String getSystemPrompt() {
        return SYSTEM_PROMPT ;
    }

    private String getExpectedJsonStructure() {
        return """
{
  "service_name": "Todo Manager",
  "proposals": [
    {
      "name": "Workflow Simple",
      "description": "Flux basique avec consultation et création",
      "complexity": "low",
      "states": [
        {
          "id": "1",
          "name": "MainMenu",
          "type": "MENU",
          "is_initial": true,
          "message": "Menu Principal\\n1. Voir liste\\n2. Créer\\n0. Quitter",
          "linked_endpoint": null,
          "parameter_name": null,
          "transitions": [
            {"input": "1", "nextState": "2"},
            {"input": "2", "nextState": "5"},
            {"input": "0", "nextState": "99"}
          ],
          "reasoning": "Menu principal de navigation"
        },
        {
          "id": "2",
          "name": "LoadList",
          "type": "PROCESSING",
          "is_initial": false,
          "message": "Chargement...",
          "linked_endpoint": "GET /resources",
          "parameter_name": null,
          "transitions": [
            {"condition": "SUCCESS", "nextState": "3"},
            {"condition": "ERROR", "nextState": "1"}
          ],
          "reasoning": "Récupère les données via API"
        },
        {
          "id": "3",
          "name": "DisplayList",
          "type": "DISPLAY",
          "is_initial": false,
          "message": "Liste:\\n{{#each items}}\\n{{id}}. {{name}}\\n{{/each}}\\n\\n99. Menu",
          "linked_endpoint": null,
          "parameter_name": null,
          "transitions": [
            {"input": "99", "nextState": "1"}
          ],
          "reasoning": "Affiche les résultats"
        },
        {
          "id": "5",
          "name": "EnterValue",
          "type": "INPUT",
          "is_initial": false,
          "message": "Entrez une valeur:\\n99. Annuler",
          "linked_endpoint": null,
          "parameter_name": "inputValue",
          "transitions": [
            {"condition": "VALID", "nextState": "6"},
            {"input": "99", "nextState": "1"}
          ],
          "reasoning": "Collecte une donnée utilisateur"
        },
        {
          "id": "6",
          "name": "CreateResource",
          "type": "PROCESSING",
          "is_initial": false,
          "message": "Création...",
          "linked_endpoint": "POST /resources",
          "parameter_name": null,
          "transitions": [
            {"condition": "SUCCESS", "nextState": "7"},
            {"condition": "ERROR", "nextState": "5"}
          ],
          "reasoning": "Crée via API"
        },
        {
          "id": "7",
          "name": "Success",
          "type": "FINAL",
          "is_initial": false,
          "message": "✓ Créé!\\n\\n1. Nouveau\\n99. Menu",
          "linked_endpoint": null,
          "parameter_name": null,
          "transitions": [
            {"input": "1", "nextState": "5"},
            {"input": "99", "nextState": "1"}
          ],
          "reasoning": "Confirmation"
        },
        {
          "id": "99",
          "name": "Exit",
          "type": "FINAL",
          "is_initial": false,
          "message": "Au revoir!",
          "linked_endpoint": null,
          "parameter_name": null,
          "transitions": [],
          "reasoning": "Sortie du service"
        }
      ],
      "estimated_states_count": 7,
      "reasoning": "Workflow simple et efficace couvrant les besoins de base"
    },
    {
      "name": "Workflow Complet",
      "description": "Flux détaillé avec toutes les opérations CRUD",
      "complexity": "medium",
      "states": [
        {
          "id": "1",
          "name": "MainMenu",
          "type": "MENU",
          "is_initial": true,
          "message": "Menu\\n1. Voir\\n2. Créer\\n3. Modifier\\n4. Supprimer\\n0. Quitter",
          "linked_endpoint": null,
          "parameter_name": null,
          "transitions": [
            {"input": "1", "nextState": "2"},
            {"input": "2", "nextState": "5"},
            {"input": "3", "nextState": "10"},
            {"input": "4", "nextState": "15"},
            {"input": "0", "nextState": "99"}
          ],
          "reasoning": "Menu complet avec toutes les options"
        },
        {
          "id": "99",
          "name": "Exit",
          "type": "FINAL",
          "is_initial": false,
          "message": "Au revoir!",
          "linked_endpoint": null,
          "parameter_name": null,
          "transitions": [],
          "reasoning": "Sortie"
        }
      ],
      "estimated_states_count": 15,
      "reasoning": "Workflow complet avec validations et gestion d'erreurs robuste"
    }
  ],
  "menu_texts": {},
  "response_summaries": {},
  "input_states": {}
}

⚠️ STRUCTURE CRITIQUE:
- L'objet racine contient "service_name" et "proposals" (array)
- Chaque élément de "proposals" contient: name, description, complexity, states, estimated_states_count, reasoning
- "states" est un ARRAY d'OBJETS (pas de strings!)
- Chaque état a: id, name, type, is_initial, message, linked_endpoint, parameter_name, transitions, reasoning
- Types valides: MENU, INPUT, DISPLAY, PROCESSING, FINAL
- Messages ≤ 182 caractères
- Transitions: {"input": "1", "nextState": "2"} OU {"condition": "SUCCESS", "nextState": "3"}
""";
    }

    // private String getExpectedJsonStructure() {
    // return """
    //   {
    //     "serviceCode": "todo-manager",
    //     "serviceName": "Todo Manager Service",
    //     "version": "1.0.0",
    //     "description": "Task management service",
        
    //     "apiConfig": {
    //       "baseUrl": "https://jsonplaceholder.typicode.com",
    //       "timeout": 10000,
    //       "retryAttempts": 2,
    //       "authentication": {
    //         "type": "NONE"
    //       }
    //     },

    //     "sessionConfig": {
    //       "timeoutSeconds": 60,
    //       "maxInactivitySeconds": 30
    //     },
        
    //     "states": [
    //       {
    //         "id": "1",
    //         "name": "TodoMainMenu",
    //         "type": "MENU",
    //         "isInitial": true,
    //         "message": "Todo Manager\\n1. View my tasks\\n2. Add new task\\n3. Mark as complete\\n4. Delete task\\n0. Exit",
    //         "transitions": [
    //           {"input": "1", "nextState": "2"},
    //           {"input": "2", "nextState": "5"},
    //           {"input": "3", "nextState": "8"},
    //           {"input": "4", "nextState": "11"},
    //           {"input": "0", "nextState": "99"}
    //         ]
    //       },
    //       // ... (le reste du todo-service.json, copie TOUT le JSON complet ici)
    //       {
    //         "id": "99",
    //         "name": "ExitMessage",
    //         "type": "FINAL",
    //         "message": "Thank you for using Todo Manager!\\n\\nDial *123*1# anytime to return.",
    //         "transitions": []
    //       }
    //     ]
    //   }

    //   ******STRUCTURE CRITIQUE (RESPECTE EXACTEMENT)****** :
    //   - Objet racine avec serviceCode, serviceName, version, description
    //   - apiConfig avec baseUrl, timeout, retryAttempts, authentication.type
    //   - sessionConfig avec timeoutSeconds, maxInactivitySeconds
    //   - states : ARRAY d'OBJETS (id, name, type, isInitial, message, transitions, action si PROCESSING)
    //   - Pour PROCESSING : action avec type=API_CALL, method, endpoint, onSuccess (nextState + responseMapping), onError
    //   - Pour INPUT : validation + storeAs
    //   - Pour MENU : transitions avec input/nextState
    //   - Messages ≤ 182 chars
    //   - Transitions : {"input": "...", "nextState": "..."} ou {"condition": "SUCCESS", "nextState": "..."}
    //   """;
    //   }

}