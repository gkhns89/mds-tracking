package com.gcodes.aacctracker.dto;

import com.gcodes.aacctracker.model.AgreementStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgencyAgreementUpdateRequest {

    private AgreementStatus status;
    private LocalDateTime endDate;
    private String notes;
}