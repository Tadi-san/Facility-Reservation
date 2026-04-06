package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record OfflineReconciliationRequest(@NotBlank String csvPayload) {}
