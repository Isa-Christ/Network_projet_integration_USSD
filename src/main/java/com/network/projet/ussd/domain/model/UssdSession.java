package com.network.projet.ussd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("ussd_session")
public class UssdSession {

    @Id
    private String id;

    @Column("phone_number")
    private String phoneNumber;

    @Column("service_id")
    private Long serviceId;

    @Column("current_state_id")
    private String currentStateId;

    @Column("collected_data")
    private String collectedData;

    @Column("last_activity")
    private LocalDateTime lastActivity;

    @Column("is_active")
    private Boolean isActive;

    @Column("created_at")
    private LocalDateTime createdAt;
}