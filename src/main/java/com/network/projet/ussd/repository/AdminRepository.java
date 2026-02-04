package com.network.projet.ussd.repository;

import com.network.projet.ussd.domain.model.Admin;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository réactif pour la gestion des administrateurs.
 */
@Repository
public interface AdminRepository extends ReactiveCrudRepository<Admin, Long> {

    /**
     * Recherche un administrateur par son nom d'utilisateur.
     */
    Mono<Admin> findByUsername(String username);

    /**
     * Recherche un administrateur par son email.
     */
    Mono<Admin> findByEmail(String email);
}
