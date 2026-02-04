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
      hints.getCritical_states().forEach(state -> prompt.append("  • ").append(state).append("\n"));
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
        endpoint.getParameters().forEach((param) -> prompt.append(String.format("%s (%s, %s) ",
            param.getName(),
            param.getType(),
            param.isRequired() ? "obligatoire" : "optionnel")));
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
    return SYSTEM_PROMPT;
  }

  private String getExpectedJsonStructure() {
    return """
        {
          "service_name": "Pick'n'Drop",
          "proposals": [
            {
              "name": "Parcours Client Complet",
              "description": "Flux authentifié avec consultation et commande",
              "complexity": "high",
              "states": [
                {
                  "id": "1",
                  "name": "MainMenu",
                  "type": "MENU",
                  "is_initial": true,
                  "message": "Bienvenue sur Pick'n'Drop\\n1. Se connecter\\n2. Créer compte\\n3. Tarifs\\n0. Quitter",
                  "transitions": [
                    {"input": "1", "nextState": "10"},
                    {"input": "2", "nextState": "20"},
                    {"input": "3", "nextState": "30"},
                    {"input": "0", "nextState": "99"}
                  ],
                  "reasoning": "Menu d'accueil standard"
                },
                {
                  "id": "10",
                  "name": "LoginEmail",
                  "type": "INPUT",
                  "is_initial": false,
                  "message": "Email:",
                  "parameter_name": "email",
                  "transitions": [
                    {"condition": "VALID", "nextState": "11"}
                  ]
                },
                {
                  "id": "11",
                  "name": "LoginProcess",
                  "type": "PROCESSING",
                  "is_initial": false,
                  "message": "Connexion...",
                  "linked_endpoint": "POST /auth/login",
                  "transitions": [
                    {"condition": "SUCCESS", "nextState": "50"},
                    {"condition": "ERROR", "nextState": "1"}
                  ],
                  "reasoning": "Appel API avec mapping automatique du token"
                },
                {
                  "id": "50",
                  "name": "UserMenu",
                  "type": "MENU",
                  "is_initial": false,
                  "message": "Bonjour {{user.name}}\\n1. Mes Colis\\n2. Nouvelle Course\\n0. Deconnexion",
                  "transitions": [
                    {"input": "1", "nextState": "60"},
                    {"input": "2", "nextState": "70"},
                    {"input": "0", "nextState": "1"}
                  ]
                },
                {
                    "id": "99",
                    "name": "Exit",
                    "type": "FINAL",
                    "message": "Au revoir!",
                    "transitions": []
                }
              ]
            }
          ]
        }

        RÈGLES CRITIQUES:
        1. 'linked_endpoint' DOIT être sous la forme "METHOD /path" (ex: "POST /auth/login").
        2. Les états PROCESSING doivent avoir des transitions SUCCESS et ERROR.
        3. Utilise {{variable}} pour dynamiser les messages.
        4. Assure toi que TOUTES les transitions pointent vers des IDs qui existent. Pas de liens morts.
        """;
  }

}