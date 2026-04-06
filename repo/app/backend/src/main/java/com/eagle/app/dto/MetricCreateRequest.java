package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record MetricCreateRequest(@NotBlank String metricKey,
                                  @NotBlank String effectiveFrom,
                                  String effectiveTo,
                                  @NotBlank String definition,
                                  @NotEmpty Map<String, Integer> weightedDimensions) {}
