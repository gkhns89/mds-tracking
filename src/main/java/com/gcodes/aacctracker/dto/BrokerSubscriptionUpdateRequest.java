package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BrokerSubscriptionUpdateRequest {

    private Long newPlanId; // Yeni plan seçimi (opsiyonel)

    private LocalDateTime newEndDate; // Yeni bitiş tarihi (opsiyonel)

    @Positive(message = "Custom max broker users must be positive")
    private Integer customMaxBrokerUsers; // Özel kullanıcı limiti

    @Positive(message = "Custom max client companies must be positive")
    private Integer customMaxClientCompanies; // Özel müşteri limiti

    private String notes;
}