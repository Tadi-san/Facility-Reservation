package com.eagle.app.dto;

import com.eagle.app.model.ReservationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReservationRequest(Long requesterId,
                                 @NotNull Long roomId,
                                 @NotNull Instant startTime,
                                 @NotNull Instant endTime,
                                 ReservationStatus status) {}
