package com.network.projet.ussd.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import com.network.projet.ussd.domain.model.UssdService;
import com.network.projet.ussd.domain.model.automaton.AutomatonDefinition;
import com.network.projet.ussd.dto.request.ServiceRegistrationRequest;
import com.network.projet.ussd.dto.response.ServiceInfoResponse;
import com.network.projet.ussd.repository.UssdServiceRepository;
import com.network.projet.ussd.service.core.ServiceRegistry;
import com.network.projet.ussd.util.ShortCodeGenerator;

@Slf4j
@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
public class ServiceAdminController {

	private final UssdServiceRepository serviceRepository;
	private final ServiceRegistry serviceRegistry;
	private final ObjectMapper objectMapper;
	private final ShortCodeGenerator shortCodeGenerator;

	/**
	 * Register a new service
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<ServiceInfoResponse> registerService(@RequestBody ServiceRegistrationRequest request) {
		log.info("Registering new service from JSON config");

		return Mono.fromCallable(() -> {
			// Parse and validate JSON
			AutomatonDefinition automaton = objectMapper.readValue(
					request.getJsonConfig(),
					AutomatonDefinition.class);
			return automaton;
		})
				.flatMap(automaton ->
				// Generate short code reactively
				shortCodeGenerator.generateNext()
						.map(generatedShortCode -> UssdService.builder()
								.code(automaton.getServiceCode())
								.name(automaton.getServiceName())
								.shortCode(generatedShortCode) // ← Ici c'est maintenant un String
								.jsonConfig(request.getJsonConfig())
								.apiBaseUrl(automaton.getApiConfig().getBaseUrl())
								.isActive(true)
								.createdAt(LocalDateTime.now())
								.updatedAt(LocalDateTime.now())
								.build()))
				.flatMap(serviceRepository::save)
				.map(this::toResponse)
				.doOnSuccess(s -> log.info("Service registered: {}", s.getCode()))
				.onErrorResume(e -> {
					log.error("Failed to register service", e);
					return Mono.error(new ResponseStatusException(
							HttpStatus.BAD_REQUEST,
							"Invalid service JSON config: " + e.getMessage()));
				});
	}

	/**
	 * List all services
	 */
	@GetMapping
	public Flux<ServiceInfoResponse> listServices() {
		return serviceRepository.findAll()
				.map(this::toResponse);
	}

	/**
	 * Get service by code
	 */
	@GetMapping("/{code}")
	public Mono<ServiceInfoResponse> getService(@PathVariable String code) {
		return serviceRepository.findByCode(code)
				.map(this::toResponse);
	}

	/**
	 * Update service
	 */
	@PutMapping("/{code}")
	public Mono<ServiceInfoResponse> updateService(
			@PathVariable String code,
			@RequestBody ServiceRegistrationRequest request) {
		return serviceRepository.findByCode(code)
				.flatMap(existing -> {
					existing.setJsonConfig(request.getJsonConfig());
					existing.setUpdatedAt(LocalDateTime.now());
					return serviceRepository.save(existing);
				})
				.doOnSuccess(s -> serviceRegistry.invalidateCache(code))
				.map(this::toResponse);
	}

	/**
	 * Delete service
	 */
	@DeleteMapping("/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> deleteService(@PathVariable String code) {
		return serviceRepository.findByCode(code)
				.flatMap(serviceRepository::delete)
				.doOnSuccess(v -> serviceRegistry.invalidateCache(code));
	}

	/**
	 * Activate/Deactivate service
	 */
	@PatchMapping("/{code}/status")
	public Mono<ServiceInfoResponse> toggleStatus(@PathVariable String code) {
		return serviceRepository.findByCode(code)
				.flatMap(service -> {
					service.setIsActive(!service.getIsActive());
					service.setUpdatedAt(LocalDateTime.now());
					return serviceRepository.save(service);
				})
				.map(this::toResponse);
	}

	private ServiceInfoResponse toResponse(UssdService service) {
		return ServiceInfoResponse.builder()
				.id(service.getId())
				.code(service.getCode())
				.name(service.getName())
				.shortCode(service.getShortCode())
				.apiBaseUrl(service.getApiBaseUrl())
				.isActive(service.getIsActive())
				.createdAt(service.getCreatedAt())
				.build();
	}
}