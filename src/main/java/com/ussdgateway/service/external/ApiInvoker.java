package com.ussdgateway.service.external;

import com.ussdgateway.domain.enums.AuthenticationType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class ApiInvoker {

    private final WebClient webClient;
    private final AuthenticationHandler authenticationHandler;

    public ApiInvoker(WebClient.Builder webClientBuilder, 
                     AuthenticationHandler authenticationHandler) {
        this.webClient = webClientBuilder.build();
        this.authenticationHandler = authenticationHandler;
    }

    /**
     * Appel API générique avec authentification
     */
    public Mono<String> invoke(String url, 
                               HttpMethod method, 
                               Object requestBody,
                               AuthenticationType authType,
                               String credentials) {
        
        WebClient.RequestBodySpec request = webClient
            .method(method)
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);

        // Ajouter le body si présent
        WebClient.RequestHeadersSpec<?> headersSpec = (requestBody != null)
            ? request.bodyValue(requestBody)
            : request;

        // Appliquer l'authentification
        headersSpec = authenticationHandler.applyAuthentication(
            headersSpec, authType, credentials
        );

        // Exécuter la requête avec timeout
        return headersSpec
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(e -> Mono.just("ERROR: " + e.getMessage()));
    }

    // Méthodes helper
    public Mono<String> get(String url, AuthenticationType authType, String credentials) {
        return invoke(url, HttpMethod.GET, null, authType, credentials);
    }

    public Mono<String> post(String url, Object body, AuthenticationType authType, String credentials) {
        return invoke(url, HttpMethod.POST, body, authType, credentials);
    }

    public Mono<String> put(String url, Object body, AuthenticationType authType, String credentials) {
        return invoke(url, HttpMethod.PUT, body, authType, credentials);
    }

    public Mono<String> delete(String url, AuthenticationType authType, String credentials) {
        return invoke(url, HttpMethod.DELETE, null, authType, credentials);
    }
}