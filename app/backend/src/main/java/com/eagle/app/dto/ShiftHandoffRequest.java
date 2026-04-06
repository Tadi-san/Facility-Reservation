package com.eagle.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ShiftHandoffRequest(@NotNull Long fromUserId,
                                  @NotNull Long toUserId,
                                  @NotNull Instant handoffTime,
                                  @NotBlank String summary,
                                  String pendingTasks) {}
