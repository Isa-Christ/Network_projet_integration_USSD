// package com.network.projet.ussd.external;

// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.network.projet.ussd.exception.LlmApiException;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;
// import org.springframework.web.reactive.function.client.WebClient;
// import reactor.core.publisher.Mono;

// import java.time.Duration;
// import java.util.List;
// import java.util.Map;

// /**
//  * Client pour Cerebras Inference API (OpenAI-compatible) avec LLaMA.
//  */
// @Component
// @Slf4j
// public class AnthropicApiClient {  // Renomme en CerebrasApiClient plus tard

//     private final WebClient webClient;
//     private final ObjectMapper objectMapper;

//     @Value("${ai.generator.huggingface.model:llama3.1-8b}")
//     private String model;

//     @Value("${ai.generator.huggingface.timeout:60000}")
//     private long timeout;

//     @Value("${ai.generator.huggingface.max-tokens:4096}")
//     private int maxTokens;

//     public AnthropicApiClient(
//             @Qualifier("huggingfaceWebClient") WebClient huggingfaceWebClient,
//             ObjectMapper objectMapper
//     ) {
//         this.webClient = huggingfaceWebClient;
//         this.objectMapper = objectMapper;
//     }

//     public Mono<String> generateJson(String systemPrompt, String userPrompt) {
//         String enhancedSystem = systemPrompt + "\n\nRÈGLE CRITIQUE: Ta réponse doit être UNIQUEMENT du JSON valide, sans texte avant/après, sans markdown ni ```json.";

//         return generateChatCompletion(enhancedSystem, userPrompt)
//                 .map(this::extractContentFromResponse);
//     }

//     private Mono<String> generateChatCompletion(String systemPrompt, String userPrompt) {
//         log.info("Calling Cerebras API: model={}, max_tokens={}", model, maxTokens);

//         Map<String, Object> body = Map.of(
//                 "model", model,
//                 "messages", List.of(
//                         Map.of("role", "system", "content", systemPrompt),
//                         Map.of("role", "user", "content", userPrompt)
//                 ),
//                 "temperature", 0.0,          // Bas pour JSON déterministe
//                 "max_tokens", maxTokens,
//                 "stream", false
//         );

//         return webClient.post()
//                 .uri("/chat/completions")  // ← Endpoint OpenAI-compatible
//                 .bodyValue(body)
//                 .retrieve()
//                 .bodyToMono(String.class)
//                 .timeout(Duration.ofMillis(timeout))
//                 .flatMap(response -> {
//                     log.debug("Raw Cerebras response: {}", response);
//                     return Mono.just(response);
//                 })
//                 .onErrorMap(e -> {
//                     log.error("Cerebras API error: {}", e.getMessage());
//                     return new LlmApiException("Erreur Cerebras: " + e.getMessage(), e);
//                 });
//     }

//     private String extractContentFromResponse(String jsonResponse) {
//         try {
//             JsonNode root = objectMapper.readTree(jsonResponse);
//             JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
            
//             if (contentNode.isMissingNode() || contentNode.isNull()) {
//                 throw new LlmApiException("Aucun content dans la réponse Cerebras");
//             }
            
//             String content = contentNode.asText().trim();
            
//             // Nettoyage des balises markdown si présentes
//             content = content.replaceAll("^```json\\s*", "")
//                             .replaceAll("^```\\s*", "")
//                             .replaceAll("\\s*```$", "")
//                             .trim();
            
//             // Extraction du JSON pur : tout ce qui est entre la première { et la dernière }
//             int firstBrace = content.indexOf('{');
//             int lastBrace = content.lastIndexOf('}');
            
//             if (firstBrace == -1 || lastBrace == -1 || firstBrace >= lastBrace) {
//                 log.warn("Aucune accolade trouvée dans le contenu. Contenu brut: {}", content);
//                 throw new LlmApiException("Pas de JSON valide trouvé dans la réponse");
//             }
            
//             // Extraire uniquement le JSON
//             String jsonOnly = content.substring(firstBrace, lastBrace + 1);
            
//             // Validation : vérifier que c'est un JSON valide
//             try {
//                 objectMapper.readTree(jsonOnly);
//                 log.debug("JSON extrait et validé avec succès");
//                 return jsonOnly;
//             } catch (JsonProcessingException e) {
//                 log.error("Le JSON extrait n'est pas valide: {}", jsonOnly);
//                 throw new LlmApiException("JSON extrait invalide: " + e.getMessage(), e);
//             }
            
//         } catch (LlmApiException e) {
//             throw e; // Re-throw les exceptions métier
//         } catch (Exception e) {
//             log.error("Erreur parsing réponse Cerebras", e);
//             throw new LlmApiException("Réponse Cerebras invalide: " + e.getMessage(), e);
//         }
//     }
// }

package com.network.projet.ussd.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.exception.LlmApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client pour Cerebras Inference API (OpenAI-compatible) avec LLaMA.
 */
@Component
@Slf4j
public class AnthropicApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.generator.huggingface.model:llama3.1-8b}")
    private String model;

    @Value("${ai.generator.huggingface.timeout:60000}")
    private long timeout;

    @Value("${ai.generator.huggingface.max-tokens:4096}")
    private int maxTokens;

    public AnthropicApiClient(
            @Qualifier("huggingfaceWebClient") WebClient huggingfaceWebClient,
            ObjectMapper objectMapper
    ) {
        this.webClient = huggingfaceWebClient;
        this.objectMapper = objectMapper;
    }

    public Mono<String> generateJson(String systemPrompt, String userPrompt) {
        log.info("🎯 generateJson called");
        log.debug("📝 System prompt length: {} chars", systemPrompt.length());
        log.debug("📝 User prompt length: {} chars", userPrompt.length());
        
        String enhancedSystem = systemPrompt + "\n\n***RÈGLE ABSOLUE*** : TA RÉPONSE DOIT ÊTRE UNIQUEMENT du JSON valide, sans texte avant/après, sans markdown ni ```json. Commence directement par {.";
        
        log.debug("📝 Enhanced system prompt: {}", enhancedSystem.substring(0, Math.min(200, enhancedSystem.length())) + "...");

        return generateChatCompletion(enhancedSystem, userPrompt)
                .doOnNext(rawResponse -> {
                    log.debug("📦 Raw API response received: {} chars", rawResponse.length());
                    log.debug("📦 Response preview: {}", 
                             rawResponse.substring(0, Math.min(500, rawResponse.length())));
                })
                .map(this::extractContentFromResponse)
                .doOnNext(extractedJson -> {
                    log.info("✅ JSON extracted successfully: {} chars", extractedJson.length());
                    log.debug("📄 Extracted JSON preview: {}", 
                             extractedJson.substring(0, Math.min(500, extractedJson.length())));
                })
                .doOnError(error -> {
                    log.error("❌ Error in generateJson: {}", error.getMessage());
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException webError = (WebClientResponseException) error;
                        log.error("❌ HTTP Status: {}", webError.getStatusCode());
                        log.error("❌ Response body: {}", webError.getResponseBodyAsString());
                    }
                });
    }

    private Mono<String> generateChatCompletion(String systemPrompt, String userPrompt) {
        log.info("🌐 Calling Cerebras API: model={}, max_tokens={}", model, maxTokens);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.0,
                "max_tokens", maxTokens,
                "stream", false
        );

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            log.debug("📤 Request body: {}", bodyJson.substring(0, Math.min(500, bodyJson.length())) + "...");
        } catch (Exception e) {
            log.warn("⚠️ Could not serialize request body for logging");
        }

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> {
                    log.info("✅ Cerebras API call successful");
                    log.debug("📥 Response length: {} chars", response.length());
                })
                .doOnError(error -> {
                    log.error("❌ Cerebras API call failed: {}", error.getMessage());
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException webError = (WebClientResponseException) error;
                        log.error("❌ HTTP Status: {}", webError.getStatusCode());
                        log.error("❌ Headers: {}", webError.getHeaders());
                        log.error("❌ Response: {}", webError.getResponseBodyAsString());
                    }
                })
                .onErrorMap(e -> {
                    if (!(e instanceof LlmApiException)) {
                        log.error("❌ Mapping error to LlmApiException: {}", e.getMessage());
                        return new LlmApiException("Erreur Cerebras: " + e.getMessage(), e);
                    }
                    return e;
                });
    }

    private String extractContentFromResponse(String jsonResponse) {
        log.debug("🔍 Extracting content from response...");
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            log.debug("✅ JSON parsed successfully");
            
            // Vérifier la structure de la réponse
            if (!root.has("choices")) {
                log.error("❌ Response missing 'choices' field");
                log.error("❌ Response structure: {}", root.toPrettyString());
                throw new LlmApiException("Réponse Cerebras invalide: pas de champ 'choices'");
            }
            
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                log.error("❌ 'choices' array is empty");
                throw new LlmApiException("Réponse Cerebras invalide: 'choices' vide");
            }
            
            JsonNode contentNode = choices.get(0).path("message").path("content");
            
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                log.error("❌ No content in Cerebras response");
                log.error("❌ Message structure: {}", choices.get(0).path("message").toPrettyString());
                throw new LlmApiException("Aucun content dans la réponse Cerebras");
            }
            
            String content = contentNode.asText().trim();
            log.debug("📝 Raw content length: {} chars", content.length());
            log.debug("📝 Raw content preview: {}", 
                     content.substring(0, Math.min(300, content.length())));
            
            // Nettoyage des balises markdown
            content = content.replaceAll("^```json\\s*", "")
                            .replaceAll("^```\\s*", "")
                            .replaceAll("\\s*```$", "")
                            .trim();
            
            log.debug("🧹 After markdown cleanup: {} chars", content.length());
            
            // Extraction du JSON pur
            String jsonOnly = extractPureJson(content);
            
            if (jsonOnly == null || jsonOnly.isEmpty()) {
                log.error("❌ No JSON found in content");
                log.error("❌ Content was: {}", content);
                throw new LlmApiException("Pas de JSON valide trouvé dans la réponse");
            }
            
            log.debug("📦 Extracted JSON length: {} chars", jsonOnly.length());
            
            // Validation
            try {
                objectMapper.readTree(jsonOnly);
                log.info("✅ JSON validated successfully");
                return jsonOnly;
            } catch (JsonProcessingException e) {
                log.error("❌ Invalid JSON extracted");
                log.error("❌ JSON was: {}", jsonOnly.substring(0, Math.min(500, jsonOnly.length())));
                log.error("❌ Parse error: {}", e.getMessage());
                throw new LlmApiException("JSON extrait invalide: " + e.getMessage(), e);
            }
            
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error parsing Cerebras response", e);
            log.error("❌ Response was: {}", jsonResponse);
            throw new LlmApiException("Réponse Cerebras invalide: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extrait le JSON pur d'un texte qui peut contenir du texte avant/après.
     * Gère les objets {} et les tableaux [].
     */
    private String extractPureJson(String content) {
        if (content == null || content.isEmpty()) {
            log.warn("⚠️ extractPureJson received null/empty content");
            return null;
        }
        
        log.debug("🔍 Searching for JSON in content...");
        
        // Chercher un objet JSON {...}
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        
        // Chercher un tableau JSON [...]
        int firstBracket = content.indexOf('[');
        int lastBracket = content.lastIndexOf(']');
        
        log.debug("   firstBrace={}, lastBrace={}, firstBracket={}, lastBracket={}", 
                 firstBrace, lastBrace, firstBracket, lastBracket);
        
        // Déterminer quel type commence en premier
        boolean isObject = (firstBrace != -1) && (firstBracket == -1 || firstBrace < firstBracket);
        
        if (isObject && firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            log.debug("✅ Found JSON object from position {} to {}", firstBrace, lastBrace);
            
            // Si du texte existe avant le JSON, le logger
            if (firstBrace > 0) {
                String textBefore = content.substring(0, firstBrace);
                log.warn("⚠️ Text found before JSON: {}", textBefore.trim());
            }
            
            // Si du texte existe après le JSON, le logger
            if (lastBrace < content.length() - 1) {
                String textAfter = content.substring(lastBrace + 1);
                log.warn("⚠️ Text found after JSON: {}", textAfter.trim());
            }
            
            return content.substring(firstBrace, lastBrace + 1);
        } else if (!isObject && firstBracket != -1 && lastBracket != -1 && firstBracket < lastBracket) {
            log.debug("✅ Found JSON array from position {} to {}", firstBracket, lastBracket);
            return content.substring(firstBracket, lastBracket + 1);
        }
        
        log.error("❌ No valid JSON structure found");
        return null;
    }
}