package com.network.projet.ussd.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UssdSession - Entity représentant une session USSD utilisateur
 * 
 * @author Network Projet Team
 * @version 2.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ussd_sessions")
public class UssdSession {

    /**
     * Identifiant technique base de données (auto-increment)
     */
    @Id
    private Long id;

    /**
     * Identifiant unique de la session (UUID)
     * Généré par le client/opérateur et persisté côté client
     */
    @Column("session_id")
    private String sessionId;

    /**
     * Numéro de téléphone de l'utilisateur
     * Format: international sans '+' (ex: 237690123456)
     */
    @Column("phone_number")
    private String phoneNumber;

    /**
     * Code du service (ex: "todo-manager", "weather-service")
     * Utilisé pour charger l'automate depuis ServiceRegistry
     */
    @Column("service_code")
    private String serviceCode;

    /**
     * ID de l'état actuel dans l'automate
     * Représente la position de l'utilisateur dans le flow
     */
    @Column("current_state_id")
    private String currentStateId;

    /**
     * Données collectées durant la session (format JSON)
     * Stocke les inputs utilisateur et résultats d'API
     * Structure: {"key": "value", "taskTitle": "Acheter lait", ...}
     */
    @Column("session_data")
    private String sessionData;

    /**
     * Indicateur de session active
     */
    @Column("is_active")
    private Boolean isActive;

    /**
     * Date/heure de création de la session
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Date/heure de dernière mise à jour
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Date/heure d'expiration calculée
     * = createdAt + timeout configuré (ex: 5 minutes)
     */
    @Column("expires_at")
    private LocalDateTime expiresAt;

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Vérifie si la session a expiré
     */
    @Transient
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Calcule la durée de la session depuis sa création
     */
    @Transient
    public Duration getSessionDuration() {
        if (createdAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(createdAt, LocalDateTime.now());
    }

    /**
     * Marque la session comme active et prolonge l'expiration
     */
    public void touch(Duration timeout) {
        this.updatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plus(timeout);
        if (this.isActive == null || !this.isActive) {
            this.isActive = true;
        }
    }

    /**
     * Termine la session (marque comme inactive)
     */
    public void terminate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Vérifie si la session est active
     */
    @Transient
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Prépare la session avant insertion
     */
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.expiresAt == null) {
            this.expiresAt = now.plusMinutes(5); // 5 minutes par défaut
        }
        
        if (this.isActive == null) {
            this.isActive = true;
        }
        
        if (this.sessionData == null) {
            this.sessionData = "{}";
        }
    }

    /**
     * Prépare la session avant mise à jour
     */
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Valide que la session a les champs obligatoires
     */
    @Transient
    public boolean isValid() {
        return sessionId != null && !sessionId.isEmpty()
            && phoneNumber != null && !phoneNumber.isEmpty()
            && currentStateId != null && !currentStateId.isEmpty()
            && sessionData != null;
    }

    @Override
    public String toString() {
        return String.format(
            "UssdSession{id=%d, sessionId='%s', phone='%s', serviceCode='%s', " +
            "state='%s', active=%s, expiresAt=%s}",
            id, sessionId, phoneNumber, serviceCode, currentStateId, 
            isActive, expiresAt
        );
    }
}