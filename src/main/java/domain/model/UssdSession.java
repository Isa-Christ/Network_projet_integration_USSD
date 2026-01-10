package domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a USSD session
 * @author Ton Nom
 * @date 06/01/2026
 */
@Table("ussd_sessions")
public class UssdSession {

    @Id
    private UUID id;

    private String phone_number; // snake_case selon la charte
    private String session_id;
    private String current_state;
    private String user_input;
    private LocalDateTime created_at;
    private LocalDateTime expires_at;
    private boolean is_active;

    // Constructeurs
    public UssdSession() {
        this.id = UUID.randomUUID();
        this.created_at = LocalDateTime.now();
        this.expires_at = created_at.plusSeconds(30);
        this.is_active = true;
    }

    // Getters et Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phone_number;
    }

    public void setPhoneNumber(String phone_number) {
        this.phone_number = phone_number;
    }
    /**
     * Check if session has expired
     * @return true if expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expires_at);
    }
}
