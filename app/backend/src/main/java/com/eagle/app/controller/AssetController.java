package com.eagle.app.controller;

import com.eagle.app.dto.AssetResponse;
import com.eagle.app.dto.AssetStatusUpdateRequest;
import com.eagle.app.repository.AssetRepository;
import com.eagle.app.service.AssetService;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
@PreAuthorize("hasAnyRole('TECH','ADMIN')")
public class AssetController {
    private final AssetRepository assets;
    private final AssetService service;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AssetController(AssetRepository assets, AssetService service, CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.assets = assets;
        this.service = service;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Page<AssetResponse> list(Pageable pageable) {
        return assets.findAll(pageable).map(AssetResponse::from);
    }

    @PatchMapping("/{id}/status")
    public AssetResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AssetStatusUpdateRequest req) {
        var updated = service.updateOperationalStatus(id, req.status());
        if (req.lifecycleState() != null) updated = service.transitionLifecycle(id, req.lifecycleState());
        auditLogService.log(currentUserService.requireCurrentUser(), "ASSET_STATUS_UPDATED", "Asset", String.valueOf(id), "Updated status to " + req.status() + " lifecycle " + req.lifecycleState());
        return AssetResponse.from(updated);
    }
}
