package com.network.projet.ussd.service.aigeneration;

import com.network.projet.ussd.domain.enums.EndpointType;
import com.network.projet.ussd.domain.enums.HttpMethod;
import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.Endpoint;
import com.network.projet.ussd.exception.SwaggerParseException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Parser pour fichiers Swagger/OpenAPI.
 * 
 * @author Your Name
 * @since 2025-01-26
 */
@Service
@Slf4j
public class SwaggerParser {
    
    /**
     * Parse un fichier Swagger JSON/YAML.
     */
    public Mono<ApiStructure> parse(String swagger_content) {
        return Mono.fromCallable(() -> {
            log.info("Parsing Swagger content");
            
            OpenAPI openapi = new OpenAPIV3Parser().readContents(swagger_content).getOpenAPI();
            
            if (openapi == null) {
                throw new SwaggerParseException("Fichier Swagger invalide ou vide");
            }
            
            ApiStructure api_structure = ApiStructure.builder()
                .api_title(openapi.getInfo().getTitle())
                .api_version(openapi.getInfo().getVersion())
                .base_url(extractBaseUrl(openapi))
                .authentication_type(detectAuthType(openapi))
                .endpoints(new HashMap<>())
                .schemas(new HashMap<>())
                .build();
            
            // Parser les endpoints
            if (openapi.getPaths() != null) {
                openapi.getPaths().forEach((path, path_item) -> {
                    parsePathItem(path, path_item, api_structure);
                });
            }
            
            // Parser les schemas
            if (openapi.getComponents() != null && openapi.getComponents().getSchemas() != null) {
                parseSchemas(openapi.getComponents().getSchemas(), api_structure);
            }
            
            log.info("Swagger parsed: {} endpoints, {} schemas", 
                api_structure.getEndpoints().size(),
                api_structure.getSchemas().size());
            
            return api_structure;
        });
    }
    
    private String extractBaseUrl(OpenAPI openapi) {
        if (openapi.getServers() != null && !openapi.getServers().isEmpty()) {
            Server server = openapi.getServers().get(0);
            return server.getUrl();
        }
        return "";
    }
    
    private String detectAuthType(OpenAPI openapi) {
        if (openapi.getComponents() != null && 
            openapi.getComponents().getSecuritySchemes() != null &&
            !openapi.getComponents().getSecuritySchemes().isEmpty()) {
            return openapi.getComponents().getSecuritySchemes()
                .values().iterator().next().getType().toString();
        }
        return "none";
    }
    
    private void parsePathItem(String path, PathItem path_item, ApiStructure api_structure) {
        Map<PathItem.HttpMethod, Operation> operations = path_item.readOperationsMap();
        
        operations.forEach((http_method, operation) -> {
            try {
                Endpoint endpoint = buildEndpoint(path, http_method, operation);
                String operation_id = endpoint.getOperation_id();
                api_structure.getEndpoints().put(operation_id, endpoint);
            } catch (Exception e) {
                log.warn("Erreur parsing endpoint {} {}: {}", http_method, path, e.getMessage());
            }
        });
    }
    
    private Endpoint buildEndpoint(String path, PathItem.HttpMethod http_method, Operation operation) {
        HttpMethod method = convertHttpMethod(http_method);
        
        Endpoint endpoint = Endpoint.builder()
            .operation_id(operation.getOperationId() != null ? operation.getOperationId() : generateOperationId(path, method))
            .path(path)
            .method(method)
            .summary(operation.getSummary())
            .description(operation.getDescription())
            .type(determineEndpointType(path, method))
            .parameters(new ArrayList<>())
            .has_request_body(false)
            .response_is_array(false)
            .build();
        
        // Parser paramètres
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(param -> {
                endpoint.getParameters().add(buildParameter(param));
            });
        }
        
        // Parser request body
        if (operation.getRequestBody() != null && 
            operation.getRequestBody().getContent() != null) {
            endpoint.setHas_request_body(true);
            MediaType media_type = operation.getRequestBody().getContent().get("application/json");
            if (media_type != null && media_type.getSchema() != null) {
                endpoint.setRequest_body_schema(media_type.getSchema().get$ref());
            }
        }
        
        // Parser response
        if (operation.getResponses() != null && operation.getResponses().get("200") != null) {
            var response_200 = operation.getResponses().get("200");
            if (response_200.getContent() != null) {
                MediaType media_type = response_200.getContent().get("application/json");
                if (media_type != null && media_type.getSchema() != null) {
                    Schema schema = media_type.getSchema();
                    endpoint.setResponse_schema(schema.get$ref());
                    endpoint.setResponse_is_array("array".equals(schema.getType()));
                }
            }
        }
        
        return endpoint;
    }
    
    private com.network.projet.ussd.domain.model.aigeneration.Parameter buildParameter(Parameter param) {
        return com.network.projet.ussd.domain.model.aigeneration.Parameter.builder()
            .name(param.getName())
            .in_location(param.getIn())
            .type(param.getSchema() != null ? param.getSchema().getType() : "string")
            .format(param.getSchema() != null ? param.getSchema().getFormat() : null)
            .required(Boolean.TRUE.equals(param.getRequired()))
            .description(param.getDescription())
            .build();
    }
    
    private void parseSchemas(Map<String, Schema> swagger_schemas, ApiStructure api_structure) {
        swagger_schemas.forEach((schema_name, schema) -> {
            api_structure.getSchemas().put(schema_name, schema);
        });
    }
    
    private HttpMethod convertHttpMethod(PathItem.HttpMethod http_method) {
        return switch (http_method) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case DELETE -> HttpMethod.DELETE;
            case PATCH -> HttpMethod.PATCH;
            default -> HttpMethod.GET;
        };
    }
    
    private EndpointType determineEndpointType(String path, HttpMethod method) {
        boolean has_path_param = path.contains("{");
        
        return switch (method) {
            case GET -> has_path_param ? EndpointType.READ_DETAIL : EndpointType.LIST;
            case POST -> EndpointType.CREATE;
            case PUT, PATCH -> EndpointType.UPDATE;
            case DELETE -> EndpointType.DELETE;
        };
    }
    
    private String generateOperationId(String path, HttpMethod method) {
        String cleaned_path = path.replaceAll("[{}/-]", "_");
        return method.name().toLowerCase() + cleaned_path;
    }
}