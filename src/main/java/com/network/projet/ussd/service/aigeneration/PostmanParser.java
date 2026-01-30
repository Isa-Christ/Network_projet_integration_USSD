package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.exception.SwaggerParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Parser pour Postman Collections (optionnel, non implémenté dans MVP).
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Service
@Slf4j
public class PostmanParser {
    
    /**
     * Parse une Postman Collection.
     */
    public Mono<ApiStructure> parse(String postman_json) {
        return Mono.error(new SwaggerParseException(
            "Postman Collection non supporté dans cette version. Utilisez Swagger/OpenAPI."
        ));
    }
}