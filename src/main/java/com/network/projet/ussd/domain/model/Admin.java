package com.network.projet.ussd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entité représentant un administrateur du système.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("admins")
public class Admin {

    @Id
    private Long id;

    private String username;

    private String email;

    private String password;

    private String role;

    private LocalDateTime createdAt;
}
