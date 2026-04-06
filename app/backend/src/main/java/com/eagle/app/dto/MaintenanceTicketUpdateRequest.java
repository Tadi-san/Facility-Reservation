package com.eagle.app.dto;

import com.eagle.app.model.MaintenanceTicketStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MaintenanceTicketUpdateRequest(@NotNull MaintenanceTicketStatus status,
                                             BigDecimal partsCost,
                                             BigDecimal laborHours) {}
