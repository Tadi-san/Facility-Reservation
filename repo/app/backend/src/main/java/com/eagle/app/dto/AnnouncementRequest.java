package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;

public record AnnouncementRequest(@NotBlank String title, @NotBlank String message, boolean published) {}
