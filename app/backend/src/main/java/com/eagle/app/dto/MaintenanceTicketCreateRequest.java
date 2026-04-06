package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record MaintenanceTicketCreateRequest(@NotBlank String title,
                                             @NotBlank String roomNumber,
                                             @NotBlank String description) {}
