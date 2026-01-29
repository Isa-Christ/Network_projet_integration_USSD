package com.network.projet.ussd.service.aigeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.model.aigeneration.ApiStructure;
import com.network.projet.ussd.domain.model.aigeneration.GenerationHints;
import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposals;
import com.network.projet.ussd.exception.LlmApiException;
import com.network.projet.ussd.external.AnthropicApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Orchestrateur LLM supportant HuggingFace (distant) et Ollama (local).
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmOrchestrator {
    
    private final AnthropicApiClient llm_client;
    private final PromptBuilder prompt_builder;
    private final HeuristicGenerator heuristic_generator;
    private final ObjectMapper object_mapper;
    
    @Value("${ai.generator.fallback.max-retries:3}")
    private int max_retries;
    
    @Value("${ai.generator.fallback.use-heuristic:true}")
    private boolean use_heuristic_fallback;
    
    /**
     * Génère des propositions de workflows via LLM.
     */
    public Mono<WorkflowProposals> generateWorkflows(ApiStructure api_structure, GenerationHints hints) {
        log.info(" Starting workflow generation with LLM");
        log.debug("API Structure: {} endpoints", api_structure.getEndpoints().size());
        log.debug("Hints: service_name={}, complexity={}", 
                 hints.getService_name(), hints.getComplexity());
        
        String system_prompt = prompt_builder.getSystemPrompt();
        String user_prompt = prompt_builder.buildWorkflowGenerationPrompt(api_structure, hints);
        
        log.debug(" System prompt length: {} chars", system_prompt.length());
        log.debug(" User prompt length: {} chars", user_prompt.length());
        log.debug(" Total prompt length: {} chars", system_prompt.length() + user_prompt.length());
        
        // Log un extrait du prompt pour debugging
        log.debug("📄 User prompt preview (first 500 chars):\n{}", 
                 user_prompt.substring(0, Math.min(500, user_prompt.length())));
        
        return llm_client.generateJson(system_prompt, user_prompt)
            .doOnNext(jsonResponse -> {
                log.info("✅ Received LLM response");
                log.debug("📦 Raw JSON response length: {} chars", jsonResponse.length());
                log.debug("📦 JSON response preview (first 1000 chars):\n{}", 
                         jsonResponse.substring(0, Math.min(1000, jsonResponse.length())));
            })
            .flatMap(this::parseWorkflowProposals)
            .doOnNext(proposals -> {
                log.info("✅ Successfully parsed {} workflow proposals", 
                        proposals.getProposals() != null ? proposals.getProposals().size() : 0);
                if (proposals.getProposals() != null && !proposals.getProposals().isEmpty()) {
                    proposals.getProposals().forEach(p -> 
                        log.debug("   - Proposal: {} ({} states)", 
                                 p.getName(), 
                                 p.getStates() != null ? p.getStates().size() : 0)
                    );
                }
            })
            .retryWhen(Retry.backoff(max_retries, Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof LlmApiException)
                .doBeforeRetry(signal -> 
                    log.warn("⚠️ Retry LLM call, attempt {} - Error: {}", 
                            signal.totalRetries() + 1, 
                            signal.failure().getMessage())
                ))
            .onErrorResume(error -> {
                log.error("❌ LLM failed after {} retries: {}", max_retries, error.getMessage());
                log.error("❌ Error details: ", error);
                
                if (use_heuristic_fallback) {
                    log.info("🔄 Falling back to heuristic generation");
                    return heuristic_generator.generateBasicWorkflow(api_structure, hints);
                }
                return Mono.error(error);
            });
    }
    
    private Mono<WorkflowProposals> parseWorkflowProposals(String json_response) {
        return Mono.fromCallable(() -> {
            try {
                log.debug("🔍 Parsing JSON response to WorkflowProposals...");
                
                // Vérifier que le JSON n'est pas vide
                if (json_response == null || json_response.trim().isEmpty()) {
                    log.error("❌ JSON response is null or empty!");
                    throw new LlmApiException("Réponse LLM vide");
                }
                
                // Log la structure du JSON pour debugging
                log.debug("📋 JSON structure preview:\n{}", 
                         json_response.substring(0, Math.min(500, json_response.length())));
                
                WorkflowProposals proposals = object_mapper.readValue(json_response, WorkflowProposals.class);
                
                // Vérification détaillée
                if (proposals == null) {
                    log.error("❌ Parsed proposals is null!");
                    throw new LlmApiException("Proposals object is null after parsing");
                }
                
                log.info("✅ Successfully parsed WorkflowProposals");
                log.debug("   - service_name: {}", proposals.getService_name());
                log.debug("   - proposals count: {}", 
                         proposals.getProposals() != null ? proposals.getProposals().size() : 0);
                
                if (proposals.getProposals() == null || proposals.getProposals().isEmpty()) {
                    log.warn("⚠️ WARNING: Proposals list is empty or null!");
                    log.debug("Full parsed object: {}", object_mapper.writeValueAsString(proposals));
                }
                
                return proposals;
            } catch (com.fasterxml.jackson.core.JsonParseException e) {
                log.error("❌ JSON Parse Error at location: line={}, column={}", 
                         e.getLocation().getLineNr(), 
                         e.getLocation().getColumnNr());
                log.error("❌ Parse error message: {}", e.getOriginalMessage());
                log.error("❌ Problematic JSON snippet:\n{}", 
                         json_response.substring(
                             Math.max(0, (int)e.getLocation().getCharOffset() - 100),
                             Math.min(json_response.length(), (int)e.getLocation().getCharOffset() + 100)
                         ));
                throw new LlmApiException("JSON invalide: " + e.getMessage(), e);
            } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
                log.error("❌ JSON Mapping Error: {}", e.getMessage());
                log.error("❌ Path: {}", e.getPathReference());
                throw new LlmApiException("Mapping JSON invalide: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("❌ Error parsing LLM JSON response", e);
                log.error("❌ Response that failed to parse:\n{}", json_response);
                throw new LlmApiException("Réponse LLM invalide: " + e.getMessage(), e);
            }
        });
    }
}