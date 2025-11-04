package com.gcodes.aacctracker.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TransactionUpdateRequest {

    private String fileNo;
    private String recipientName;
    private String customsWarehouse;
    private String gate;
    private BigDecimal weight;
    private BigDecimal tax;
    private String senderName;
    private LocalDate warehouseArrivalDate;
    private LocalDate registrationDate;
    private String declarationNumber;
    private LocalDate lineClosureDate;
    private Integer importProcessingTime;
    private LocalDate withdrawalDate;
    private String description;
    private String delayReason;
}