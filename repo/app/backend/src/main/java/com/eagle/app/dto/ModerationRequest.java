package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerationRequest(@NotBlank String fileName,
                                @NotBlank String contentType,
                                @NotBlank String status,
                                String reason,
                                Long uploadedById) {}
