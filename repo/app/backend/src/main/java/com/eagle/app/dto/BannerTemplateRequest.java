package com.eagle.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BannerTemplateRequest(@NotBlank String templateKey,
                                    @NotNull @Min(1) @Max(720) Integer minutesBefore,
                                    @NotBlank String message,
                                    boolean active) {}
