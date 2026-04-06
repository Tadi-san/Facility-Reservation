package com.eagle.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionApplyRequest(@NotBlank String code,
                                    @NotNull @DecimalMin("0.01") BigDecimal orderAmount,
                                    @NotNull Instant orderTime) {}
