package com.network.projet.ussd.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.repository.GenericStorageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.network.projet.ussd.domain.model.GenericStorage;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenericStorageService {

    private final GenericStorageRepository repository;
    private final ObjectMapper objectMapper;

    public Mono<Void> save(String phoneNumber, String serviceCode, String key, Object value) {
        return repository
                .findByPhoneNumberAndServiceCodeAndStorageKey(phoneNumber, serviceCode, key)
                .defaultIfEmpty(new GenericStorage())
                .flatMap(storage -> {
                    storage.setPhoneNumber(phoneNumber);
                    storage.setServiceCode(serviceCode);
                    storage.setStorageKey(key);

                    // Conversion Object → JSON String
                    try {
                        String jsonValue = objectMapper.writeValueAsString(value);
                        storage.setStorageValue(jsonValue);
                    } catch (Exception e) {
                        log.error("Failed to serialize value to JSON", e);
                        return Mono.error(new RuntimeException("Failed to serialize value", e));
                    }

                    storage.setUpdatedAt(LocalDateTime.now());
                    if (storage.getId() == null) {
                        storage.setCreatedAt(LocalDateTime.now());
                    }
                    return repository.save(storage);
                })
                .then();
    }

    public Mono<Object> load(String phoneNumber, String serviceCode, String key) {
        return repository
                .findByPhoneNumberAndServiceCodeAndStorageKey(phoneNumber, serviceCode, key)
                .mapNotNull(storage -> {
                    try {
                        return objectMapper.readValue(storage.getStorageValue(), Object.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize JSON value", e);
                        return null;
                    }
                });
    }

    public Mono<Map<String, Object>> loadMap(String phoneNumber, String serviceCode, String key) {
        return repository
                .findByPhoneNumberAndServiceCodeAndStorageKey(phoneNumber, serviceCode, key)
                .mapNotNull(storage -> {
                    try {
                        return objectMapper.readValue(storage.getStorageValue(), Map.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize JSON to Map", e);
                        return null;
                    }
                });
    }

    public Mono<List<Object>> loadList(String phoneNumber, String serviceCode, String key) {
        return repository
                .findByPhoneNumberAndServiceCodeAndStorageKey(phoneNumber, serviceCode, key)
                .mapNotNull(storage -> {
                    try {
                        return objectMapper.readValue(storage.getStorageValue(), List.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize JSON to List", e);
                        return null;
                    }
                });
    }

    public Mono<Void> append(String phoneNumber, String serviceCode, String key, Object item) {
        return loadList(phoneNumber, serviceCode, key)
                .defaultIfEmpty(List.of())
                .flatMap(list -> {
                    List<Object> mutableList = new java.util.ArrayList<>(list);
                    mutableList.add(item);
                    return save(phoneNumber, serviceCode, key, mutableList);
                });
    }

    public Mono<Void> delete(String phoneNumber, String serviceCode, String key) {
        return repository
                .findByPhoneNumberAndServiceCodeAndStorageKey(phoneNumber, serviceCode, key)
                .flatMap(repository::delete);
    }
}