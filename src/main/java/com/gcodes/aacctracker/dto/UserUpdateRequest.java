package com.gcodes.aacctracker.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class UserUpdateRequest {
    @Email(message = "Invalid email format")
    private String email;

    private String username;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password; // Opsiyonel - boşsa değiştirmez

    private Boolean isActive;
}