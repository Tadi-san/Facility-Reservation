package com.eagle.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OfflineOrderCreateRequest(Long requesterId,
                                        Long reservationId,
                                        @NotNull @DecimalMin("0.01") BigDecimal amount,
                                        @NotBlank String idempotencyKey) {}
