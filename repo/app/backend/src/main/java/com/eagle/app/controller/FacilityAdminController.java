package com.eagle.app.controller;

import com.eagle.app.dto.RoomBrowserResponse;
import com.eagle.app.dto.RoomCreateRequest;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import com.eagle.app.service.FacilityAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/facilities")
@PreAuthorize("hasRole('ADMIN')")
public class FacilityAdminController {
    private final FacilityAdminService facilities;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public FacilityAdminController(FacilityAdminService facilities, CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.facilities = facilities;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/rooms")
    public RoomBrowserResponse create(@Valid @RequestBody RoomCreateRequest req) {
        var created = facilities.createRoom(req);
        auditLogService.log(currentUserService.requireCurrentUser(), "ROOM_CREATED", "Room", String.valueOf(created.id()), "Created room " + created.roomNumber());
        return created;
    }
}
