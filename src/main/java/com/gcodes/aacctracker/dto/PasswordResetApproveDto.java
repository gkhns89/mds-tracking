package com.gcodes.aacctracker.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetApproveDto {

    @Size(max = 500, message = "Admin notes must be less than 500 characters")
    private String adminNotes;
}