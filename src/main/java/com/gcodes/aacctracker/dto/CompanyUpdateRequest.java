package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyUpdateRequest {

    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
}