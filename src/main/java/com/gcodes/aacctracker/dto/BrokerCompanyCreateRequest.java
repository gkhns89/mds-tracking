package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BrokerCompanyCreateRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String description;

    // ===== ABONELİK BİLGİLERİ =====

    @NotNull(message = "Subscription plan ID is required")
    private Long subscriptionPlanId;

    private LocalDateTime startDate; // Null ise şu an

    private LocalDateTime endDate; // Null ise süresiz

    // ===== ÖZEL LİMİTLER (Opsiyonel) =====

    @Positive(message = "Custom max broker users must be positive")
    private Integer customMaxBrokerUsers;

    @Positive(message = "Custom max client companies must be positive")
    private Integer customMaxClientCompanies;

    private String notes;
}