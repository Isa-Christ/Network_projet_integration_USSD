package com.network.projet.ussd.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity pour table generation_history.
 * 
 * @author Network Project Team
 * @since 2025-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("generation_history")
public class GenerationHistoryEntity {
    
    @Id
    @Column("history_id")
    private UUID history_id;
    
    @Column("config_id")
    private UUID config_id;
    
    @Column("admin_user")
    private String admin_user;
    
    @Column("action")
    private String action;
    
    @Column("input_tokens")
    private Integer input_tokens;
    
    @Column("output_tokens")
    private Integer output_tokens;
    
    @Column("processing_time_ms")
    private Long processing_time_ms;
    
    @Column("error_message")
    private String error_message;
    
    @Column("created_at")
    private LocalDateTime created_at;
}