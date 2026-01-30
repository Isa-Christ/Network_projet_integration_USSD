package com.network.projet.ussd.dto.request;

import com.network.projet.ussd.domain.enums.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour analyser une source API.
 * 
 * @author Your Name
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiSourceRequest {
    
    @NotNull(message = "Le type de source est obligatoire")
    private SourceType source_type;
    
    private String source_url;          // URL Swagger si SWAGGER_URL
    private String file_content;        // Contenu fichier si SWAGGER_FILE
    
    /**
     * Valide que la requête contient les données nécessaires selon le type.
     */
    public boolean isValid() {
        return switch (source_type) {
            case SWAGGER_URL -> source_url != null && !source_url.trim().isEmpty();
            case SWAGGER_FILE, POSTMAN -> file_content != null && !file_content.trim().isEmpty();
        };
    }
}