# AI Generator - Génération Automatique de Configurations USSD

## Prérequis

### 1. Installer Ollama (LLM local gratuit)

```bash
# Via Docker (recommandé)
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama

# Télécharger le modèle Llama 3.2
docker exec -it ollama ollama pull llama3.2

# Vérifier que Ollama fonctionne
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "prompt": "Hello",
  "stream": false
}'
```

### 2. Configuration

Vérifier `application.properties` :

```properties
ai.generator.enabled=true
ai.generator.ollama.base-url=http://localhost:11434
ai.generator.ollama.model=llama3.2
```

## Utilisation

### Workflow Complet (4 étapes)

#### Étape 1: Analyser l'API

```bash
curl -X POST http://localhost:3001/api/admin/ai-generator/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "source_type": "SWAGGER_URL",
    "source_url": "https://farcal-back.onrender.com/v3/api-docs"
  }'
```

#### Étape 2: Générer propositions

```bash
curl -X POST http://localhost:3001/api/admin/ai-generator/generate-proposals \
  -H "Content-Type: application/json" \
  -d '{
    "api_structure": { ... },
    "hints": {
      "service_name": "Calculator",
      "primary_entity": "calcs",
      "target_language": "fr"
    }
  }'
```

**Important: Les `hints` sont ESSENTIELS pour une meilleure qualité de génération:**

- `service_name`: Nom du service (influencie le contexte métier du LLM)
- `primary_entity`: Entité principale (améliore la pertinence des états)
- `target_language`: Langue des textes (fr, en, etc.)

Les hints guident le LLM pour générer des workflows adaptés au domaine métier réel.

#### Étape 3: Générer configuration

```bash
curl -X POST http://localhost:3001/api/admin/ai-generator/generate-config \
  -H "Content-Type: application/json" \
  -d '{
    "api_structure": { ... },
    "workflow_proposals": { ... },
    "selected_proposal_index": 1
  }'
```

#### Étape 4: Valider

```bash
curl -X POST http://localhost:3001/api/admin/ai-generator/validate-config \
  -H "Content-Type: application/json" \
  -d '{ ... config JSON ... }'
```

### One-Shot (tout en une fois)

```bash
curl -X POST http://localhost:3001/api/admin/ai-generator/auto-generate \
  -H "Content-Type: application/json" \
  -d '{
    "source_type": "SWAGGER_URL",
    "source_url": "https://jsonplaceholder.typicode.com/swagger.json",
    "hints": {
      "service_name": "Todo Manager",
      "primary_entity": "todos",
      "target_language": "fr"
    },
    "selected_proposal_index": 1
  }'
```

## Troubleshooting

### Ollama ne démarre pas

```bash
# Vérifier les logs
docker logs ollama

# Redémarrer
docker restart ollama
```

### Erreur "LLM_API_ERROR"

Vérifier que Ollama est accessible :

```bash
curl http://localhost:11434/api/tags
```

### Génération trop lente

Réduire `max-tokens` dans `application.properties` :

```properties
ai.generator.ollama.max-tokens=2000
```

## Performance

- Analyse API: ~2-5 secondes
- Génération propositions: ~30-60 secondes (dépend de la taille de l'API)
- Génération config: ~5-10 secondes
- Total (one-shot): ~40-75 secondes

## Limitations

- Maximum 50 endpoints par API
- Swagger/OpenAPI uniquement (pas Postman dans MVP)
- Ollama doit être installé localement
