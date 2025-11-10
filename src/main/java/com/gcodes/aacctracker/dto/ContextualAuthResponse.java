package com.gcodes.aacctracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ContextualAuthResponse {
    private String token;
    private Map<String, Object> user;
    private List<Map<String, Object>> availableBrokers; // Erişebildiği tüm broker'lar
    private Map<String, Object> selectedBroker; // Seçili broker (varsa)
    private Boolean status;
}