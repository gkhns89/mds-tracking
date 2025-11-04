package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgencyAgreementCreateRequest {

    @NotNull(message = "Broker company ID is required")
    private Long brokerCompanyId;

    @NotNull(message = "Client company ID is required")
    private Long clientCompanyId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String notes;
}