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
	public Mono<ServiceInfoResponse> registerService(
			@RequestBody ServiceRegistrationRequest request,
			@RequestHeader(value = "X-Admin-Id", required = false) Long adminId) {
		log.info("Registering new service from JSON config for admin: {}", adminId);

		return Mono.fromCallable(() -> objectMapper.readValue(request.getJsonConfig(), AutomatonDefinition.class))
				.flatMap(automaton -> serviceRepository.findByCode(automaton.getServiceCode())
						.flatMap(existing -> Mono.<AutomatonDefinition>error(
								new com.network.projet.ussd.exception.ServiceAlreadyExistsException(
										"Le service avec le code '" + automaton.getServiceCode() + "' existe déjà.")))
						.switchIfEmpty(Mono.just(automaton)))
				.flatMap(automaton -> shortCodeGenerator.generateNext()
						.map(generatedShortCode -> UssdService.builder()
								.code(automaton.getServiceCode())
								.name(automaton.getServiceName())
								.shortCode(generatedShortCode)
								.jsonConfig(request.getJsonConfig())
								.apiBaseUrl(
										automaton.getApiConfig() != null ? automaton.getApiConfig().getBaseUrl() : null)
								.isActive(true)
								.adminId(adminId)
								.createdAt(LocalDateTime.now())
								.updatedAt(LocalDateTime.now())
								.build()))
				.flatMap(serviceRepository::save)
				.map(this::toResponse)
				.doOnSuccess(s -> log.info("Service registered: {}", s.getCode()))
				.onErrorResume(e -> {
					if (e instanceof com.network.projet.ussd.exception.ServiceAlreadyExistsException) {
						return Mono.error(e);
					}
					log.error("Failed to register service", e);
					return Mono.error(new ResponseStatusException(
							HttpStatus.BAD_REQUEST,
							"Configuration JSON invalide : " + e.getMessage()));
				});
	}

	/**
	 * List all services
	 */
	@GetMapping
	public Flux<ServiceInfoResponse> listServices(@RequestHeader(value = "X-Admin-Id", required = false) Long adminId) {
		if (adminId != null) {
			return serviceRepository.findByAdminId(adminId)
					.map(this::toResponse);
		}
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
		log.info("Toggling status for service: {}", code);
		return serviceRepository.findByCode(code)
				.flatMap(service -> {
					// Protection null-safe
					boolean currentStatus = service.getIsActive() != null ? service.getIsActive() : true;
					boolean newStatus = !currentStatus;
					service.setIsActive(newStatus);
					service.setUpdatedAt(LocalDateTime.now());
					log.info("Service {} status changed: {} -> {}", code, currentStatus, newStatus);
					return serviceRepository.save(service);
				})
				.doOnSuccess(s -> serviceRegistry.invalidateCache(code))
				.map(this::toResponse)
				.doOnError(e -> log.error("Error toggling status for service {}: {}", code, e.getMessage()));
	}

	private ServiceInfoResponse toResponse(UssdService service) {
		return ServiceInfoResponse.builder()
				.id(service.getId())
				.code(service.getCode())
				.name(service.getName())
				.shortCode(service.getShortCode())
				.apiBaseUrl(service.getApiBaseUrl())
				.jsonConfig(service.getJsonConfig())
				.isActive(service.getIsActive())
				.createdAt(service.getCreatedAt())
				.build();
	}
}