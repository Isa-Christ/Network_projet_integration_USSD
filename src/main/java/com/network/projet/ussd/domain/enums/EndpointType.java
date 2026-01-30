package com.network.projet.ussd.domain.enums;

/**
 * Type d'endpoint API (classification fonctionnelle).
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
public enum EndpointType {
    LIST,           // GET /resource (liste)
    READ_DETAIL,    // GET /resource/{id}
    CREATE,         // POST /resource
    UPDATE,         // PUT/PATCH /resource/{id}
    DELETE,         // DELETE /resource/{id}
    OTHER          // Autre type
}
