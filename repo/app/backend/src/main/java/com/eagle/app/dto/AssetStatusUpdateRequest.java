package com.eagle.app.dto;

import com.eagle.app.model.AssetLifecycleState;
import com.eagle.app.model.AssetOperationalStatus;
import jakarta.validation.constraints.NotNull;

public record AssetStatusUpdateRequest(@NotNull AssetOperationalStatus status, AssetLifecycleState lifecycleState) {}
