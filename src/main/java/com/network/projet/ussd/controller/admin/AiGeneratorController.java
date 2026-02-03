package com.network.projet.ussd.controller.admin;

import com.network.projet.ussd.domain.model.aigeneration.WorkflowProposals;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.dto.request.*;
import com.network.projet.ussd.dto.response.*;
import com.network.projet.ussd.service.aigeneration.AiGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller REST pour la génération automatique de configurations USSD.
 * 
 * @author Your Name
 * @since 2025-01-26
 */
@RestController
@RequestMapping("/api/admin/ai-generator")
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorController {
    
    private final AiGeneratorService ai_generator_service;
    
    /**
     * Étape 1: Analyser une source API.
     */
    @PostMapping("/analyze")
    public Mono<ResponseEntity<ApiAnalysisResult>> analyzeApi(
        @Valid @RequestBody ApiSourceRequest request
    ) {
        log.info("Received API analysis request: type={}", request.getSource_type());
        
        return ai_generator_service.analyzeApiSource(request)
            .map(result -> result.isSuccess() 
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
            )
            .onErrorResume(error -> {
                log.error("Analysis endpoint error", error);
                ApiAnalysisResult failure_result = ApiAnalysisResult.failure(error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(failure_result)
                );
            });
    }
    
    /**
     * Étape 2: Générer des propositions de workflows.
     */
    @PostMapping("/generate-proposals")
    public Mono<ResponseEntity<WorkflowProposals>> generateProposals(
        @Valid @RequestBody GenerateProposalsRequest request
    ) {
        log.info("Received workflow generation request");
        
        return ai_generator_service.generateWorkflowProposals(request)
            .map(proposals -> ResponseEntity.ok(proposals))
            .onErrorResume(error -> {
                log.error("Proposals endpoint error", error);
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .<WorkflowProposals>body(null)  // TYPAGE EXPLICITE
                );
            });
    }
    
    /**
     * Étape 3: Générer la configuration finale.
     */
    @PostMapping("/generate-config")
    public Mono<ResponseEntity<GenerationResult>> generateConfig(
        @Valid @RequestBody GenerateConfigRequest request
    ) {
        log.info("Received config generation request: proposal={}", request.getSelected_proposal_index());
        
        return ai_generator_service.generateFinalConfig(request)
            .map(result -> result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
            )
            .onErrorResume(error -> {
                log.error("Config generation endpoint error", error);
                GenerationResult failure_result = GenerationResult.failure(error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(failure_result)
                );
            });
    }
    
    /**
     * Étape 4: Valider une configuration.
     */
    @PostMapping("/validate-config")
    public Mono<ResponseEntity<ValidationReportResponse>> validateConfig(
        @Valid @RequestBody AutomatonDefinition config
    ) {
        log.info("Received config validation request: {}", config.getServiceCode());
        
        return ai_generator_service.validateConfiguration(config)
            .map(report -> ResponseEntity.ok(report))
            .onErrorResume(error -> {
                log.error("Validation endpoint error", error);
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .<ValidationReportResponse>body(null)  // TYPAGE EXPLICITE
                );
            });
    }
    
    /**
     * ONE-SHOT: Génération automatique complète.
     */
    @PostMapping("/auto-generate")
    public Mono<ResponseEntity<AutoGenerationResult>> autoGenerate(
        @Valid @RequestBody AutoGenerateRequest request
    ) {
        log.info("Received auto-generation request");
        
        return ai_generator_service.autoGenerate(request)
            .map(result -> result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
            )
            .onErrorResume(error -> {
                log.error("Auto-generation endpoint error", error);
                AutoGenerationResult failure_result = AutoGenerationResult.builder()
                    .success(false)
                    .message("Échec de la génération automatique")  // CORRECTION ICI
                    .message(error.getMessage())
                    .total_processing_time_ms(0)
                    .build();
                
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(failure_result)
                );
            });
    }
    
    /**
     * Health check du générateur IA.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "AI Generator",
            "llm", "HuggingFace Inference API (LLaMA distant)",
            "timestamp", System.currentTimeMillis()
        )));
    }
}