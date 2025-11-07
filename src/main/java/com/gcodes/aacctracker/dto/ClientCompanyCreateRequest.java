package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientCompanyCreateRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String description;

    @NotNull(message = "Parent broker ID is required")
    private Long parentBrokerId;
}