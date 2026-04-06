package com.eagle.app.dto;

import jakarta.validation.constraints.*;

public record RoomCreateRequest(@NotBlank String locationCode,
                                @NotBlank String locationName,
                                String locationAddress,
                                @NotBlank String roomTypeName,
                                @NotBlank String roomNumber,
                                Integer floorNumber,
                                @NotNull @Min(1) @Max(1000) Integer capacity,
                                boolean includeProjector,
                                boolean includeHvac) {}
