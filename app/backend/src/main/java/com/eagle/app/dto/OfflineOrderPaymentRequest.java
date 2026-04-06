package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record OfflineOrderPaymentRequest(@NotBlank String idempotencyKey, String externalReference) {}
