package com.network.projet.ussd.service.aigeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.domain.enums.GenerationStatus;
import com.network.projet.ussd.domain.enums.SourceType;
import com.network.projet.ussd.domain.model.aigeneration.*;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.dto.request.*;
import com.network.projet.ussd.dto.response.*;
import com.network.projet.ussd.exception.AiGenerationException;
import com.network.projet.ussd.external.SwaggerFetcher;
import com.network.projet.ussd.repository.GeneratedConfigEntity;
import com.network.projet.ussd.repository.GeneratedConfigRepository;
import com.network.projet.ussd.repository.GenerationHistoryEntity;
import com.network.projet.ussd.repository.GenerationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service principal de génération IA.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorService {
    
    private final SwaggerFetcher swagger_fetcher;
    private final SwaggerParser swagger_parser;
    private final ApiSchemaAnalyzer schema_analyzer;
    private final LlmOrchestrator llm_orchestrator;
    private final UssdConfigGenerator config_generator;
    private final ConfigValidator config_validator;
    private final ConfigOptimizer config_optimizer;
    private final CostEstimator cost_estimator;
    private final ApiStructureCleaner api_structure_cleaner;
    
    private final GeneratedConfigRepository config_repository;
    private final GenerationHistoryRepository history_repository;
    
    private final ObjectMapper object_mapper;
    
    /**
     * Étape 1: Analyser la source API.
     */
    public Mono<ApiAnalysisResult> analyzeApiSource(ApiSourceRequest request) {
        log.info("Analyzing API source: type={}", request.getSource_type());
        long start_time = System.currentTimeMillis();
        
        return fetchSwaggerContent(request)
            .flatMap(swagger_parser::parse)
            .flatMap(schema_analyzer::analyze)
            .flatMap(api_structure -> {
                CostEstimate estimate = cost_estimator.estimate(api_structure);
                
                return saveHistory("ANALYZE", null, System.currentTimeMillis() - start_time, null)
                    .thenReturn(ApiAnalysisResult.success(api_structure, estimate));
            })
            .onErrorResume(error -> {
                log.error("API analysis failed", error);
                return saveHistory("ANALYZE", null, System.currentTimeMillis() - start_time, error.getMessage())
                    .thenReturn(ApiAnalysisResult.failure(error.getMessage()));
            });
    }
    
    /**
     * Étape 2: Générer propositions de workflows.
     */
    public Mono<WorkflowProposals> generateWorkflowProposals(GenerateProposalsRequest request) {
        log.info("Generating workflow proposals");
        long start_time = System.currentTimeMillis();
        
        // return llm_orchestrator.generateWorkflows(request.getApi_structure(), request.getHints())
        // Nettoyer l'ApiStructure pour réduire le bruit au LLM
        ApiStructure cleaned_api = api_structure_cleaner.cleanForLlm(request.getApi_structure());
        long original_size = api_structure_cleaner.estimateSize(request.getApi_structure());
        long cleaned_size = api_structure_cleaner.estimateSize(cleaned_api);
        log.info("API Structure cleaned: {} chars → {} chars (reduction: {}%)",
            original_size, cleaned_size, 
            original_size > 0 ? (int)((original_size - cleaned_size) * 100 / original_size) : 0);
        
        // Créer une copie de la request avec l'ApiStructure nettoyée
        GenerateProposalsRequest cleaned_request = GenerateProposalsRequest.builder()
            .api_structure(cleaned_api)
            .hints(request.getHints())
            .build();
        
        return llm_orchestrator.generateWorkflows(cleaned_request.getApi_structure(), cleaned_request.getHints())
            .doOnSuccess(proposals -> 
                saveHistory("GENERATE_PROPOSALS", null, System.currentTimeMillis() - start_time, null)
                    .subscribe()
            )
            .doOnError(error ->
                saveHistory("GENERATE_PROPOSALS", null, System.currentTimeMillis() - start_time, error.getMessage())
                    .subscribe()
            );
    }
    
    /**
     * Étape 3: Générer configuration finale.
     */
    public Mono<GenerationResult> generateFinalConfig(GenerateConfigRequest request) {
        log.info("Generating final config");
        long start_time = System.currentTimeMillis();
        
        return config_generator.generateConfig(
                request.getApi_structure(),
                request.getWorkflow_proposals(),
                request.getSelected_proposal_index()
            )
            .flatMap(config_optimizer::optimize)
            .flatMap(config -> config_validator.validate(config)
                .flatMap(report -> {
                    long processing_time = System.currentTimeMillis() - start_time;
                    
                    return saveGeneratedConfig(config, report, request.getApi_structure())
                        .flatMap(saved_entity ->
                            saveHistory("GENERATE_CONFIG", saved_entity.getConfig_id(), processing_time, null)
                                .thenReturn(GenerationResult.success(config, report, processing_time))
                        );
                })
            )
            .onErrorResume(error -> {
                log.error("Config generation failed", error);
                long processing_time = System.currentTimeMillis() - start_time;
                return saveHistory("GENERATE_CONFIG", null, processing_time, error.getMessage())
                    .thenReturn(GenerationResult.failure(error.getMessage()));
            });
    }
    
    /**
     * Étape 4: Valider configuration.
     */
    public Mono<ValidationReportResponse> validateConfiguration(AutomatonDefinition config) {
        return config_validator.validate(config)
            .map(ValidationReportResponse::fromReport);
    }
    
    /**
     * ONE-SHOT: Génération complète automatique.
     */
    public Mono<AutoGenerationResult> autoGenerate(AutoGenerateRequest request) {
        log.info("Starting auto-generation");
        long start_time = System.currentTimeMillis();
        
        ApiSourceRequest source_request = ApiSourceRequest.builder()
            .source_type(request.getSource_type())
            .source_url(request.getSource_url())
            .file_content(request.getFile_content())
            .build();
        
        return analyzeApiSource(source_request)
            .flatMap(analysis_result -> {
                if (!analysis_result.isSuccess()) {
                    return Mono.just(AutoGenerationResult.builder()
                        .success(false)
                        .error_message(analysis_result.getError_message())
                        .build());
                }
                
                GenerateProposalsRequest proposals_request = GenerateProposalsRequest.builder()
                    .api_structure(analysis_result.getApi_structure())
                    .hints(request.getHints())
                    .build();
                
                return generateWorkflowProposals(proposals_request)
                    .flatMap(proposals -> {
                        GenerateConfigRequest config_request = GenerateConfigRequest.builder()
                            .api_structure(analysis_result.getApi_structure())
                            .workflow_proposals(proposals)
                            .selected_proposal_index(request.getSelected_proposal_index())
                            .build();
                        
                        return generateFinalConfig(config_request)
                            .map(gen_result -> {
                                long total_time = System.currentTimeMillis() - start_time;
                                
                                return AutoGenerationResult.builder()
                                    .success(gen_result.isSuccess())
                                    .api_structure(analysis_result.getApi_structure())
                                    .workflow_proposals(proposals)
                                    .generated_config(gen_result.getGenerated_config())
                                    .validation_report(gen_result.getValidation_report())
                                    .total_processing_time_ms(total_time)
                                    .error_message(gen_result.getError_message())
                                    .build();
                            });
                    });
            })
            .onErrorResume(error -> {
                log.error("Auto-generation failed", error);
                long total_time = System.currentTimeMillis() - start_time;
                return Mono.just(AutoGenerationResult.builder()
                    .success(false)
                    .error_message(error.getMessage())
                    .total_processing_time_ms(total_time)
                    .build());
            });
    }
    
    // ========== MÉTHODES PRIVÉES ==========
    
    private Mono<String> fetchSwaggerContent(ApiSourceRequest request) {
        return switch (request.getSource_type()) {
            case SWAGGER_URL -> swagger_fetcher.fetchSwagger(request.getSource_url());
            case SWAGGER_FILE -> Mono.just(request.getFile_content());
            case POSTMAN -> Mono.error(new AiGenerationException("Postman non supporté dans cette version"));
        };
    }
    
    private Mono<GeneratedConfigEntity> saveGeneratedConfig(
        AutomatonDefinition config,
        ValidationReport report,
        ApiStructure api_structure
    ) {
        return Mono.fromCallable(() -> {
            try {
                GeneratedConfigEntity entity = GeneratedConfigEntity.builder()
                    //.config_id(UUID.randomUUID())
                    .source_type(SourceType.SWAGGER_URL.name())
                    .api_structure(object_mapper.writeValueAsString(api_structure))
                    .generated_config(object_mapper.writeValueAsString(config))
                    .validation_report(object_mapper.writeValueAsString(report))
                    .status(report.is_valid() ? GenerationStatus.COMPLETED.name() : GenerationStatus.FAILED.name())
                    .created_at(LocalDateTime.now())
                    .updated_at(LocalDateTime.now())
                    .build();
                
                return entity;
            } catch (Exception e) {
                throw new AiGenerationException("Erreur sauvegarde config", e);
            }
        })
        .flatMap(config_repository::save);
    }
    
    private Mono<Void> saveHistory(String action, UUID config_id, long processing_time_ms, String error_message) {
        return history_repository.insertHistory(
            config_id,
            "system",
            action,
            processing_time_ms,
            error_message,
            LocalDateTime.now()
        ).then();
    }

    
}