package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record MaintenanceTicketCloseRequest(@NotBlank String closureOutcome) {}
