package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SubscriptionPlanCreateRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Max broker users is required")
    @Positive(message = "Max broker users must be positive")
    private Integer maxBrokerUsers;

    @NotNull(message = "Max client companies is required")
    @Positive(message = "Max client companies must be positive")
    private Integer maxClientCompanies;

    @Positive(message = "Monthly price must be positive")
    private BigDecimal monthlyPrice;

    @Positive(message = "Yearly price must be positive")
    private BigDecimal yearlyPrice;

    private String features; // JSON string: ["Feature 1", "Feature 2"]
}