package com.eagle.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(@NotBlank String username,
                                     @Email @NotBlank String email,
                                     @NotBlank @Size(min = 12) String password,
                                     @NotNull String role,
                                     String staffContactInfo) {}
