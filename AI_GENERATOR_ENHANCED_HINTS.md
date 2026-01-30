# AI Generator - Utilisation Avancée des Hints

## Nouvelle Structure des Hints

### 1. Hints Simples (Single Entity) - Backward Compatible

```json
{
  "service_name": "Calculator",
  "primary_entity": "calculations",
  "target_language": "fr"
}
```

### 2. Hints Multi-Entités (NOUVEAU)

Pour des services complexes avec relations entre entités:

```json
{
  "service_name": "E-Commerce Store",
  "primary_entities": ["orders", "products", "customers"],
  "entity_relationships": {
    "orders": "has_many:order_items",
    "orders": "belongs_to:customers",
    "order_items": "belongs_to:products",
    "products": "has_many:categories"
  },
  "critical_states": [
    "authentication",
    "customer_selection",
    "cart_management",
    "payment_confirmation",
    "error_handling"
  ],
  "target_language": "fr"
}
```

## Exemple Complet: Todo Manager avec Relations

### Request

```bash
curl -X POST http://localhost:3001/api/admin/ai-generator/generate-proposals \
  -H "Content-Type: application/json" \
  -d '{
    "api_structure": {
      "endpoints": {
        "getTodos": {
          "method": "GET",
          "path": "/api/todos",
          "summary": "Récupérer tous les todos"
        },
        "createTodo": {
          "method": "POST",
          "path": "/api/todos",
          "summary": "Créer un nouveau todo"
        },
        "updateTodo": {
          "method": "PUT",
          "path": "/api/todos/{id}",
          "summary": "Modifier un todo existant"
        },
        "deleteTodo": {
          "method": "DELETE",
          "path": "/api/todos/{id}",
          "summary": "Supprimer un todo"
        }
      }
    },
    "hints": {
      "service_name": "Todo Manager",
      "primary_entities": ["todos", "categories", "users"],
      "entity_relationships": {
        "todos": "belongs_to:categories",
        "todos": "belongs_to:users",
        "categories": "has_many:todos",
        "users": "has_many:todos"
      },
      "critical_states": [
        "main_menu",
        "category_selection",
        "todo_list_view",
        "todo_creation",
        "todo_confirmation",
        "error_handling"
      ],
      "target_language": "fr",
      "max_menu_options": 5,
      "include_delete_operations": true,
      "include_update_operations": true
    }
  }'
```

## Réponse Attendue

Le LLM générere des workflows qui:

1. **Respecte les relations**: Affiche les categories → Les todos de chaque catégorie
2. **Inclut les états critiques**: Menu principal, sélection de catégorie, confirmation
3. **Transitions logiques**:
   - Accueil → Catégories → Todos → Actions
   - Chaque action → Confirmation → Résultat → Retour au menu
4. **Gestion d'erreurs**: États DISPLAY pour les erreurs, possibilité de réessai

### Exemple de Workflow Généré

```
Simple Workflow:
┌─────────────────────┐
│   1. Main Menu      │ (MENU)
│ 1. Voir mes todos   │
│ 2. Créer un todo    │
│ 0. Quitter          │
└──────┬──────────────┘
       │
       ├─► 2. Select Category (MENU)
       │   1. Travail
       │   2. Personnel
       │   0. Retour
       │
       ├─► 3. Todo List (DISPLAY)
       │   • Todo 1
       │   • Todo 2
       │   1. Modifier, 2. Supprimer
       │
       ├─► 4. Confirm Action (DISPLAY)
       │   Êtes-vous sûr?
       │   1. Confirmer, 0. Annuler
       │
       └─► 5. Result (DISPLAY)
           ✓ Action complétée!
           0. Retour au menu
```

## Nouvelles Capacités

| Feature            | Avant      | Après                    |
| ------------------ | ---------- | ------------------------ |
| Entités supportées | 1 seule    | Multiples (liste)        |
| Relations          | Pas gérées | Explicites avec mappings |
| États critiques    | À deviner  | Spécifiés dans hints     |
| Transitions        | Random     | Logiques et cohérentes   |
| Feedback LLM       | Limité     | Détaillé (reasoning)     |

## Meilleurs Résultats

Le LLM génère maintenant:

✅ Workflows sans états oubliés (authentication, confirmation, error handling)
✅ Transitions cohérentes entre entités liées
✅ Menus logiques basés sur les relations
✅ Gestion d'erreurs et validation appropriées
✅ Chemins utilisateur réalistes et complets

## Format de Relations Supportées

- `has_many:entity` - Une entité a plusieurs sous-entités
- `belongs_to:entity` - Cette entité appartient à une autre
- `many_to_many:entity` - Relation bidirectionnelle
- `has_one:entity` - Relation 1:1

Exemple:

```json
"entity_relationships": {
  "orders": "has_many:items|belongs_to:customers",
  "items": "belongs_to:orders|belongs_to:products",
  "products": "has_many:items"
}
```
