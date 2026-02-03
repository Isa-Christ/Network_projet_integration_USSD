package com.network.projet.ussd.repository;

import com.network.projet.ussd.domain.model.GenericStorage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface GenericStorageRepository extends ReactiveCrudRepository<GenericStorage, Long> {
    
    Mono<GenericStorage> findByPhoneNumberAndServiceCodeAndStorageKey(
        String phoneNumber,
        String serviceCode,
        String storageKey
    );
    
    @Query("DELETE FROM generic_storage WHERE phone_number = :phoneNumber AND service_code = :serviceCode AND storage_key = :storageKey")
    Mono<Void> deleteByPhoneNumberAndServiceCodeAndStorageKey(
        String phoneNumber,
        String serviceCode,
        String storageKey
    );
}