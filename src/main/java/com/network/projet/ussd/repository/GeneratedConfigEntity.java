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
 * Entity pour table generated_configs.
 * 
 * @author Network Project Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("generated_configs")
public class GeneratedConfigEntity {
    
    @Id
    @Column("config_id")
    private UUID config_id;
    
    @Column("source_type")
    private String source_type;
    
    @Column("source_url")
    private String source_url;
    
    @Column("api_structure")
    private String api_structure;  // JSON string
    
    @Column("selected_proposal_index")
    private Integer selected_proposal_index;
    
    @Column("generated_config")
    private String generated_config;  // JSON string
    
    @Column("validation_report")
    private String validation_report;  // JSON string
    
    @Column("status")
    private String status;
    
    @Column("created_at")
    private LocalDateTime created_at;
    
    @Column("updated_at")
    private LocalDateTime updated_at;
}
