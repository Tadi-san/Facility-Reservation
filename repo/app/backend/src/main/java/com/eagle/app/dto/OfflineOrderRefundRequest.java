package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record OfflineOrderRefundRequest(@NotBlank String idempotencyKey, String reason) {}
