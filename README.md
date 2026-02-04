# USSD Gateway - Service d'intégration USSD multi-services

> Plateforme moderne de services USSD permettant l'intégration d'APIs REST via une interface USSD intuitive

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/Spring-WebFlux-blue.svg)](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)

---

## Table des matières

- [Vue d'ensemble](#-vue-densemble)
- [Architecture](#-architecture)
- [Fonctionnalités](#-fonctionnalités)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration d'un service](#-configuration-dun-service)
- [Services disponibles](#-services-disponibles)
- [Authentification](#-authentification)
- [Développement](#-développement)
- [Conventions de code](#-conventions-de-code)
- [API Reference](#-api-reference)
- [Troubleshooting](#-troubleshooting)

---

## Vue d'ensemble

**USSD Gateway** est une plateforme qui transforme des APIs REST modernes en services USSD accessibles depuis n'importe quel téléphone mobile, même basique. Le système agit comme une **passerelle intelligente** entre les utilisateurs USSD et les services web.

### Concept clé : L'automate à états

Chaque service USSD est modélisé comme un **automate fini déterministe** où :
- **États** = Écrans USSD (menus, formulaires, affichages)
- **Transitions** = Actions utilisateur (choix de menu, saisie de données)
- **Actions** = Appels API, validations, traitements

**Syntaxe de modélisation** :
```
Numéro[I][F]-Nom: (transition1, état1), (transition2, état2), ...
```
- `[I]` = État initial
- `[F]` = État final

**Exemple** :
```
1[I]-MainMenu: (1, 2), (2, 5), (0, 99)
2-SelectCity: (1, 3), (2, 3), (99, 1)
3-FetchWeather: (SUCCESS, 4), (ERROR, 2)
4-DisplayWeather: (1, 2), (99, 1)
99[F]-ExitMessage:
```

### Architecture système

```
┌──────────────┐         ┌───────────────────┐         ┌─────────────┐
│  User USSD   │ ───────▶│  USSD Gateway     │ ──JWT──▶│ External    │
│ *500#        │         │  (Automaton FSM)  │         │    APIs     │
└──────────────┘         └───────────────────┘         └─────────────┘
       │                          │                           │
       │                          ├─ Session Manager          │
       │                          ├─ Automaton Engine         │
       │                          ├─ API Invoker              │
       │                          └─ Template Engine          │
       │                                 │                    │
       └────── Phone Number ─────────────┴────────────────────┘
                (Tracking)
```

---

## Architecture

### Stack technique

**Backend**
- **Java 17+** - Langage principal
- **Spring Boot 3.x** - Framework applicatif
- **Spring WebFlux** - Programmation réactive (non-blocking I/O)
- **R2DBC** - Accès base de données réactive
- **Liquibase** - Gestion des migrations de schéma
- **Lombok** - Réduction du code boilerplate

**Base de données**
- **PostgreSQL 15+** - Base de données principale
- **JSONB** - Stockage des données de session et configurations

**Templating & Communication**
- **Handlebars.java** - Moteur de templates pour messages USSD
- **WebClient** - Client HTTP réactif pour appels API externes

### Architecture en couches

```
┌─────────────────────────────────────────────────────────┐
│                     Controllers                          │
│  UssdController | ServiceAdminController                │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                      Services                            │
│  UssdGatewayService | AutomatonEngine | SessionManager  │
│  ApiInvoker | ValidationService | AuthenticationHandler │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   Domain Models                          │
│  State | Transition | Action | UssdSession              │
│  AutomatonDefinition | ApiConfig | Authentication       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   Repositories                           │
│  UssdSessionRepository | ServiceDefinitionRepository    │
└─────────────────────────────────────────────────────────┘
```

### Types d'états supportés

| Type | Description | Exemple |
|------|-------------|---------|
| `MENU` | Menu de sélection | "1. Option A\n2. Option B" |
| `INPUT` | Saisie utilisateur | "Entrez votre nom:" |
| `DISPLAY` | Affichage d'informations | "Votre solde: 5000 FCFA" |
| `PROCESSING` | Traitement/Appel API | "Chargement..." |
| `FINAL` | État terminal | "Merci, au revoir!" |

---

## Fonctionnalités

### Gestion des sessions
- Sessions USSD avec timeout configurable (30-120s)
- Stockage de données de session en JSONB
- Nettoyage automatique des sessions expirées
- Support multi-utilisateurs concurrent

### Moteur d'automate
- Exécution d'automates à états finis
- Navigation intelligente entre états
- Gestion des transitions conditionnelles
- Support des états de traitement (PROCESSING) automatiques

### Intégration API externe
- Appels HTTP réactifs (GET, POST, PUT, DELETE, PATCH)
- Support authentification : API_KEY, BEARER, BASIC, NONE
- Template rendering dans endpoints (ex: `/weather?q={{city}}`)
- Mapping de réponses API vers variables de session
- Gestion automatique des erreurs et retry

### Templating dynamique
- Moteur Handlebars pour messages USSD
- Support des boucles (`{{#each}}`)
- Support des conditions (`{{#if}}`)
- Helpers personnalisés (`{{add}}`, etc.)
- Interpolation de variables de session

### Validation des entrées
- Validation TEXT (longueur, regex)
- Validation NUMERIC (min, max)
- Messages d'erreur personnalisés
- Transitions conditionnelles (VALID/INVALID)

### Administration
- Enregistrement de services via API REST
- Stockage de configurations JSON
- Gestion multi-services
- Hot-reload de services

---

## Prérequis

- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Docker** (optionnel, pour déploiement)

---

## Installation

### 1. Cloner le repository

```bash
git clone https://github.com/Isa-Christ/Network_projet_integration_USSD.git
cd Network_projet_integration_USSD
```

### 2. Configurer la base de données

```sql
CREATE DATABASE ussd_gateway;
CREATE USER ussd_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ussd_gateway TO ussd_user;
```

### 3. Configuration application

Créer `src/main/resources/application.properties` :

```properties
spring.application.name=ussd-gateway

# ===== R2DBC Configuration =====
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/ussd_db
spring.r2dbc.username=your_username
spring.r2dbc.password=your_password

# Connection Pool
spring.r2dbc.pool.initial-size=10
spring.r2dbc.pool.max-size=50
spring.r2dbc.pool.max-idle-time=30m

# ===== Liquibase Configuration =====
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db-changelog-master.xml
spring.liquibase.url=jdbc:postgresql://localhost:5432/ussd_db
spring.liquibase.user=your_username
spring.liquibase.password=your_password
spring.liquibase.driver-class-name=org.postgresql.Driver

# ===== Server Configuration =====
server.port=8080
server.servlet.encoding.charset=UTF-8

# ===== USSD Configuration =====
# Code USSD du menu principal (par défaut: *500#)
ussd.main-menu.code=*500#

# Préfixe pour les codes de service (*500*1#, *500*2#, etc.)
ussd.service.code-prefix=*500*
ussd.service.code-suffix=#

# ===== Session Configuration =====
# Durée d'inactivité avant expiration (en minutes)
ussd.session.timeout-minutes=5

# Fréquence de nettoyage des sessions (en ms)
ussd.session.cleanup-rate-ms=60000

# Suppression définitive des anciennes sessions
ussd.session.hard-delete-after-days=7
ussd.session.hard-delete-cron=0 0 2 * * *

# ===== Logging =====
logging.level.com.network.projet.ussd=DEBUG
logging.level.org.springframework.r2dbc=DEBUG
logging.level.io.r2dbc.postgresql.QUERY=DEBUG
logging.level.liquibase=DEBUG

# ===== Jackson Configuration =====
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.deserialization.fail-on-unknown-properties=false

# ===== AI Generator Configuration (Optionnel) =====
ai.generator.enabled=true
ai.generator.provider=huggingface

# HuggingFace Configuration
ai.generator.huggingface.api-key=your-api-key
ai.generator.huggingface.base-url=https://api.cerebras.ai/v1
ai.generator.huggingface.model=llama3.1-8b
ai.generator.huggingface.timeout=60000
ai.generator.huggingface.max-tokens=8192

# Ollama Configuration (fallback local)
ai.generator.ollama.base-url=http://localhost:11434
ai.generator.ollama.model=llama3.2
ai.generator.ollama.timeout=600000

# Validation & Cache
ai.generator.validation.max-message-length=182
ai.generator.validation.max-menu-options=8
ai.generator.cache.enabled=true
ai.generator.cache.ttl-minutes=60
```

### 4. Lancer l'application

```bash
mvn clean install
mvn spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

---

## Configuration d'un service

### Structure JSON d'un service

```json
{
  "serviceCode": "weather-service",
  "serviceName": "Weather Information Service",
  "version": "1.0.0",
  "description": "Real-time weather information",
  
  "apiConfig": {
    "baseUrl": "http://api.openweathermap.org/data/2.5",
    "timeout": 10000,
    "retryAttempts": 2,
    "authentication": {
      "type": "API_KEY",
      "credentials": {
        "paramName": "appid",
        "apiKey": "your-api-key"
      }
    }
  },

  "sessionConfig": {
    "timeoutSeconds": 60,
    "maxInactivitySeconds": 30
  },
  
  "states": [
    {
      "id": "1",
      "name": "MainMenu",
      "type": "MENU",
      "isInitial": true,
      "message": "Weather Service\n1. Current weather\n2. Weather by city\n0. Exit",
      "transitions": [
        {"input": "1", "nextState": "2"},
        {"input": "2", "nextState": "5"},
        {"input": "0", "nextState": "99"}
      ]
    },
    {
      "id": "5",
      "name": "EnterCityName",
      "type": "INPUT",
      "message": "Enter city name:\n99. Cancel",
      "validation": {
        "type": "TEXT",
        "minLength": 2,
        "maxLength": 50,
        "pattern": "^[a-zA-Z\\s-]+$"
      },
      "storeAs": "customCity",
      "transitions": [
        {"condition": "VALID", "nextState": "6"},
        {"input": "99", "nextState": "1"},
        {"condition": "INVALID", "nextState": "5", "message": "Invalid city name"}
      ]
    },
    {
      "id": "6",
      "name": "FetchWeather",
      "type": "PROCESSING",
      "message": "Fetching weather...",
      "action": {
        "type": "API_CALL",
        "method": "GET",
        "endpoint": "/weather?q={{customCity}}&units=metric",
        "onSuccess": {
          "nextState": "7",
          "responseMapping": {
            "temperature": "main.temp",
            "description": "weather.0.description",
            "cityName": "name"
          }
        },
        "onError": {
          "nextState": "5",
          "message": "City not found"
        }
      },
      "transitions": [
        {"condition": "SUCCESS", "nextState": "7"},
        {"condition": "ERROR", "nextState": "5"}
      ]
    },
    {
      "id": "7",
      "name": "DisplayWeather",
      "type": "DISPLAY",
      "message": "Weather in {{cityName}}\n\nTemp: {{temperature}}C\nCondition: {{description}}\n\n99. Main menu",
      "transitions": [
        {"input": "99", "nextState": "1"}
      ]
    },
    {
      "id": "99",
      "name": "ExitMessage",
      "type": "FINAL",
      "message": "Thank you for using Weather Service!",
      "transitions": []
    }
  ]
}
```

### Enregistrer un service

```bash
POST http://localhost:8080/api/admin/services
Content-Type: application/json
{
  "jsonConfig": "{ ... votre JSON ... }"
}
```

Ou alors si vous avez les fichiers dans un dossier spécifique,

```bash
cd votre_dossier
POST http://localhost:8080/api/admin/services
Content-Type: application/json
{
  "jsonConfig": '"$(cat votre.json | jq -Rs .)"'
}
```

---

## Services disponibles

### 1. Weather Service
Service de météo en temps réel utilisant l'API OpenWeatherMap.

**Code de service** : `weather-service`

**Fonctionnalités** :
- Météo pour villes prédéfinies (Yaoundé, Douala, etc.)
- Recherche de météo par nom de ville personnalisé
- Affichage température, humidité, vent

**Automate** :
```
1[I]-MainMenu: (1, 2), (2, 5), (0, 99)
2-SelectCity: (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (99, 1)
3-FetchWeather: (SUCCESS, 4), (ERROR, 2)
4-DisplayWeather: (1, 2), (99, 1)
5-EnterCity: (VALID, 6), (99, 1), (INVALID, 5)
6-FetchCustomWeather: (SUCCESS, 7), (ERROR, 5)
7-DisplayCustomWeather: (1, 5), (2, 2), (99, 1)
99[F]-Exit:
```

### 2. Todo Manager Service
Gestionnaire de tâches utilisant JSONPlaceholder.

**Code de service** : `todo-manager`

**Fonctionnalités** :
- Voir la liste des tâches
- Ajouter une nouvelle tâche
- Marquer comme complétée
- Supprimer une tâche

### 3. PicknDrop Service (En développement)
Service de livraison de colis via USSD.

**Code de service** : `pickndrop-service`

**Fonctionnalités** :
- Enregistrement utilisateur avec PIN
- Envoi de colis
- Suivi de colis par numéro
- Historique des colis

---

## Authentification

### Architecture d'authentification

Le système USSD utilise une **authentification hybride** :

```
┌──────────────────────────────────────────────────────┐
│              Authentification USSD                   │
├──────────────────────────────────────────────────────┤
│                                                      │
│  1. User → USSD Gateway                              │
│     └─ Numéro de téléphone stocké                    │
│                                                      │
│  2. USSD Gateway → Service JWT                       │
│     └─ Token unique pour le service                  │
│                                                      │
│  3. Mapping: phone_number ↔ tokens                   │
│     └─ Stocké: generic_storage                       │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Types d'authentification API

#### 1. API_KEY (Query Parameter ou Header)

```json
{
  "authentication": {
    "type": "API_KEY",
    "credentials": {
      "paramName": "appid",
      "apiKey": "abc123xyz"
    }
  }
}
```

Génère : `GET /weather?q=Paris&appid=abc123xyz`

#### 2. BEARER (JWT Token)

```json
{
  "authentication": {
    "type": "BEARER",
    "credentials": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
}
```

Génère : `Authorization: Bearer eyJhbGc...`

#### 3. BASIC (Username:Password)

```json
{
  "authentication": {
    "type": "BASIC",
    "credentials": {
      "username": "user",
      "password": "pass"
    }
  }
}
```

Génère : `Authorization: Basic dXNlcjpwYXNz`

#### 4. NONE

```json
{
  "authentication": {
    "type": "NONE"
  }
}
```

## Développement

### Structure du projet

```
src/main/java/com/network/projet/ussd/
├── exception
│   ├── InvalidStateException.java
│   ├── ServiceNotFoundException.java
│   ├── SwaggerParseException.java
│   ├── SessionExpiredException.java
│   ├── ApiCallException.java
│   ├── AiGenerationException.java
│   ├── GlobalExceptionHandler.java
│   ├── LlmApiException.java
│   ├── ValidationException.java
├── controller
│   ├── UssdController.java
│   ├── admin
│   │   ├── ServiceAdminController.java
│   │   ├── AiGeneratorController.java
├── repository
│   ├── GenerationHistoryEntity.java
│   ├── GeneratedConfigEntity.java
│   ├── UssdServiceRepository.java
│   ├── GenerationHistoryRepository.java
│   ├── GeneratedConfigRepository.java
│   ├── UssdSessionRepository.java
│   ├── GenericStorageRepository.java
├── service
│   ├── core
│   │   ├── StateResult.java
│   │   ├── AutomatonEngine.java
│   │   ├── SessionManager.java
│   │   ├── ServiceRegistry.java
│   │   ├── UssdGatewayService.java
│   │   ├── GenericStorageService.java
│   │   ├── ConditionalEvaluator.java
│   ├── external
│   │   ├── ApiInvoker.java
│   │   ├── SessionExpirationService.java
│   │   ├── AuthenticationHandler.java
│   ├── admin
│   │   ├── ServiceAdminService.java
│   ├── aigeneration
│   │   ├── PostmanParser.java
│   │   ├── ConfigOptimizer.java
│   │   ├── ApiStructureCleaner.java
│   │   ├── LlmOrchestrator.java
│   │   ├── WorkflowToAutomatonConverter.java
│   │   ├── HeuristicGenerator.java
│   │   ├── UssdConfigGenerator.java
│   │   ├── ConfigValidator.java
│   │   ├── AiGeneratorService.java
│   │   ├── CostEstimator.java
│   │   ├── PromptBuilder.java
│   │   ├── SwaggerParser.java
│   │   ├── ApiSchemaAnalyzer.java
│   ├── validation
│   │   ├── ValidationResult.java
│   │   ├── ValidationService.java
├── dto
│   ├── request
│   │   ├── ApiSourceRequest.java
│   │   ├── ServiceRegistrationRequest.java
│   │   ├── GenerateProposalsRequest.java
│   │   ├── CustomizationRequest.java
│   │   ├── AutoGenerateRequest.java
│   │   ├── UssdRequest.java
│   │   ├── GenerateConfigRequest.java
│   ├── ExternalApiResponse.java
│   ├── response
│   │   ├── GenerationResult.java
│   │   ├── AutoGenerationResult.java
│   │   ├── ServiceInfoResponse.java
│   │   ├── UssdResponse.java
│   │   ├── ApiAnalysisResult.java
│   │   ├── ValidationReportResponse.java
├── config
│   ├── CorsConfig.java
│   ├── WebClientConfig.java
│   ├── AiGeneratorConfig.java
├── external
│   ├── AnthropicApiClient.java
│   ├── SwaggerFetcher.java
├── domain
│   ├── model
│   │   ├── GenericStorage.java
│   │   ├── UssdService.java
│   │   ├── automaton
│   │   │   ├── ApiConfig.java
│   │   │   ├── ValidationRule.java
│   │   │   ├── SessionConfig.java
│   │   │   ├── Transition.java
│   │   │   ├── State.java
│   │   │   ├── ActionResult.java
│   │   │   ├── Authentication.java
│   │   │   ├── Action.java
│   │   │   ├── AutomatonDefinition.java
│   │   ├── aigeneration
│   │   │   ├── ValidationReport.java
│   │   │   ├── GenerationHints.java
│   │   │   ├── InputConfig.java
│   │   │   ├── WorkflowState.java
│   │   │   ├── Parameter.java
│   │   │   ├── Schema.java
│   │   │   ├── Endpoint.java
│   │   │   ├── ResponseSummary.java
│   │   │   ├── WorkflowProposals.java
│   │   │   ├── WorkflowTransition.java
│   │   │   ├── WorkflowProposal.java
│   │   │   ├── DependencyGraph.java
│   │   │   ├── CostEstimate.java
│   │   │   ├── ApiStructure.java
│   │   │   ├── StateProposal.java
│   │   ├── UssdSession.java
│   ├── enums
│   │   ├── ValidationType.java
│   │   ├── AuthenticationType.java
│   │   ├── ValidationErrorCode.java
│   │   ├── ApiResponseStatus.java
│   │   ├── StateTypeDeserializer.java
│   │   ├── SourceType.java
│   │   ├── EndpointType.java
│   │   ├── GenerationStatus.java
│   │   ├── ProposalComplexity.java
│   │   ├── StateType.java
│   │   ├── ActionType.java
│   │   ├── HttpMethod.java
├── util
│   ├── GraphAlgorithms.java
│   ├── HandlebarsTemplateEngine.java
│   ├── MessageTruncator.java
│   ├── JsonMapper.java
│   ├── TemplateEngine.java
│   ├── ShortCodeGenerator.java
│   ├── StateIdGenerator.java
│   ├── JsonPathExtractor.java
├── NetworkProjetUssdApplication.java
```

### Ajouter un nouveau service

1. **Modéliser l'automate**

```
1[I]-Menu: (1, 2), (2, 3), (0, 99)
2-Input: (VALID, 4), (INVALID, 2)
4-Process: (SUCCESS, 5), (ERROR, 2)
5-Display: (99, 1)
99[F]-Exit:
```

2. **Créer le JSON de configuration**

Voir [Configuration d'un service](#-configuration-dun-service)

3. **Enregistrer via API**

```bash
curl -X POST http://localhost:8080/api/admin/services/register \
  -H "Content-Type: application/json" \
  -d @service.json
```

4. **Tester**

```bash
curl -X POST http://localhost:8080/api/ussd \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "serviceCode": "*500#",
    "phoneNumber": "237699000000",
    "text": "1"
  }'
```

### Debugging

Activer les logs détaillés dans `application.yml` :

```yaml
logging:
  level:
    com.network.projet.ussd: DEBUG
    org.springframework.r2dbc: DEBUG
    io.r2dbc.postgresql.QUERY: DEBUG
```

Logs utiles :

```
>>> collectedData keys: [city, temperature, ...]
>>> Mapping: main.temp -> temperature = 22.5
>>> Extracted API response data: [main, weather, wind, ...]
>>> Final merged data keys: [city, temperature, humidity, ...]
```

---

## Conventions de code

### Nomenclature

**Variables** : `snake_case` (anglais)
```java
String user_name;
LocalDate order_date;
```

**Constantes** : `UPPER_SNAKE_CASE`
```java
public static final int MAX_SIZE = 100;
public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
```

**Classes** : `PascalCase`
```java
public class CustomerService { }
public class OrderManager { }
```

**Méthodes** : `camelCase`
```java
public void calculateTotal() { }
public String getUserName() { }
```

**Packages** : `lowercase`, séparés par points
```java
com.network.projet.ussd.service
com.network.projet.ussd.domain.model
```

### Organisation du code

- **Une classe par fichier**
- **Longueur de ligne max** : 120 caractères
- **Indentation** : 4 espaces (pas de tabulations)
- **Accolades** : Style K&R
  ```java
  if (condition) {
      // code
  }
  ```

### Documentation

Chaque classe doit avoir un header Javadoc :

```java
/**
 * AutomatonEngine - Moteur d'exécution d'automates USSD
 * 
 * Responsabilités:
 * - Exécution des états de l'automate
 * - Gestion des transitions
 * - Appels API externes
 * 
 * @author Network Projet Team
 * @since 2026-01-22
 */
@Service
public class AutomatonEngine {
    // ...
}
```

Méthodes publiques doivent avoir une Javadoc :

```java
/**
 * Execute un état de l'automate
 * 
 * @param automaton Définition de l'automate
 * @param session Session USSD courante
 * @param userInput Saisie utilisateur
 * @return Résultat de l'exécution
 */
public Mono<StateResult> executeState(
        AutomatonDefinition automaton,
        UssdSession session,
        String userInput) {
    // ...
}
```

### Tests

- **Couverture minimale** : 80%
- **Nommage** : `shouldReturnErrorWhenInputIsInvalid`
- **Framework** : JUnit 5

```java
@Test
void shouldNavigateToNextStateWhenValidInput() {
    // Given
    UssdSession session = createTestSession();
    
    // When
    StateResult result = engine.executeState(automaton, session, "1").block();
    
    // Then
    assertThat(result.getNextStateId()).isEqualTo("2");
    assertThat(result.isContinueSession()).isTrue();
}
```

### Gestion des exceptions

Toujours utiliser des exceptions spécifiques :

```java
// ❌ Mauvais
catch (Exception e) { }

// ✅ Bon
catch (InvalidStateException e) {
    log.error("État invalide: {}", e.getMessage());
    return Mono.error(new UssdException("État non trouvé"));
}
```

### Git

**Branches** :
```
feature/nom_fonctionnalite
bugfix/description_bug
hotfix/description_rapide
```

**Commits** (anglais, impératif) :
```
Add payment gateway integration
Fix order calculation bug
Update session timeout configuration
```

---

## 🔌 API Reference

### USSD Endpoint

**POST** `/api/ussd`

Endpoint principal pour interactions USSD.

**Request** :
```json
{
  "sessionId": "sess_abc123",
  "serviceCode": "*500#",
  "phoneNumber": "237699000000",
  "text": "1"
}
```

**Response** :
```json
{
  "message": "Weather Service\n1. Current weather\n2. Weather by city\n0. Exit",
  "continueSession": true
}
```

**États de session** :
- `continueSession: true` → Session active, attend input
- `continueSession: false` → Session terminée

### Admin Endpoints

#### Enregistrer un service

**POST** `/api/admin/services/register`

```json
{
  "serviceDefinition": "{...JSON automate...}"
}
```

**Response** :
```json
{
  "serviceCode": "weather-service",
  "message": "Service registered successfully"
}
```

#### Lister les services

**GET** `/api/admin/services`

**Response** :
```json
[
  {
    "serviceCode": "weather-service",
    "serviceName": "Weather Information Service",
    "version": "1.0.0",
    "isActive": true
  }
]
```

---

## Licence

Ce projet est développé dans le cadre d'un projet du cours d'Administration Réseaux.
