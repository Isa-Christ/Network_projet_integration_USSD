package com.network.projet.ussd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("generic_storage")
public class GenericStorage {

    @Id
    private Long id;

    @Column("phone_number")
    private String phoneNumber;

    @Column("service_code")
    private String serviceCode;

    @Column("storage_key")
    private String storageKey;

    @Column("storage_value")
    private String storageValue;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
