package com.eagle.app.controller;

import com.eagle.app.dto.MaintenanceTicketCloseRequest;
import com.eagle.app.dto.MaintenanceTicketCreateRequest;
import com.eagle.app.dto.MaintenanceTicketUpdateRequest;
import com.eagle.app.model.Inspection;
import com.eagle.app.model.MaintenanceTicket;
import com.eagle.app.model.MaintenanceTicketStatus;
import com.eagle.app.repository.InspectionRepository;
import com.eagle.app.repository.MaintenanceTicketRepository;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import com.eagle.app.service.MaintenanceSlaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/maintenance")
@PreAuthorize("hasAnyRole('TECH','ADMIN')")
public class MaintenanceController {
    private final InspectionRepository inspections;
    private final MaintenanceTicketRepository tickets;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final MaintenanceSlaService maintenanceSlaService;

    public MaintenanceController(InspectionRepository inspections,
                                 MaintenanceTicketRepository tickets,
                                 CurrentUserService currentUserService,
                                 AuditLogService auditLogService,
                                 MaintenanceSlaService maintenanceSlaService) {
        this.inspections = inspections;
        this.tickets = tickets;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.maintenanceSlaService = maintenanceSlaService;
    }

    @GetMapping("/inspections")
    public Page<Inspection> listInspections(Pageable pageable) {
        return inspections.findAll(pageable);
    }

    @GetMapping("/tickets")
    public Page<MaintenanceTicket> listTickets(Pageable pageable) {
        return tickets.findAll(pageable);
    }

    @PostMapping("/tickets")
    public MaintenanceTicket openTicket(@Valid @RequestBody MaintenanceTicketCreateRequest req) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.title = req.title();
        ticket.roomNumber = req.roomNumber();
        ticket.description = req.description();
        ticket.status = MaintenanceTicketStatus.OPEN;
        ticket.initialResponseAt = Instant.now();
        ticket.slaDueAt = maintenanceSlaService.calculateDueAt(ticket.initialResponseAt);
        ticket.overdue = false;
        ticket.slaBreached = false;
        MaintenanceTicket saved = tickets.save(ticket);
        auditLogService.log(currentUserService.requireCurrentUser(), "TICKET_OPENED", "MaintenanceTicket", String.valueOf(saved.id), "Opened ticket");
        return saved;
    }

    @PatchMapping("/tickets/{id}")
    public MaintenanceTicket updateTicket(@PathVariable Long id, @Valid @RequestBody MaintenanceTicketUpdateRequest req) {
        MaintenanceTicket ticket = tickets.findById(id).orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        if (ticket.status == MaintenanceTicketStatus.CLOSED) {
            throw new IllegalArgumentException("Cannot update a closed ticket");
        }
        ticket.status = req.status();
        if (req.partsCost() != null) ticket.partsCost = req.partsCost();
        if (req.laborHours() != null) ticket.laborHours = req.laborHours();
        if (ticket.slaDueAt != null && Instant.now().isAfter(ticket.slaDueAt) && ticket.status != MaintenanceTicketStatus.CLOSED) {
            ticket.overdue = true;
            ticket.slaBreached = true;
        }
        MaintenanceTicket saved = tickets.save(ticket);
        auditLogService.log(currentUserService.requireCurrentUser(), "TICKET_UPDATED", "MaintenanceTicket", String.valueOf(saved.id), "Updated ticket status to " + saved.status);
        return saved;
    }

    @PostMapping("/tickets/{id}/close")
    public MaintenanceTicket closeTicket(@PathVariable Long id, @Valid @RequestBody MaintenanceTicketCloseRequest req) {
        MaintenanceTicket ticket = tickets.findById(id).orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        ticket.status = MaintenanceTicketStatus.CLOSED;
        ticket.closureOutcome = req.closureOutcome();
        ticket.closedAt = Instant.now();
        if (ticket.slaDueAt != null && ticket.closedAt.isAfter(ticket.slaDueAt)) {
            ticket.overdue = true;
            ticket.slaBreached = true;
        }
        MaintenanceTicket saved = tickets.save(ticket);
        auditLogService.log(currentUserService.requireCurrentUser(), "TICKET_CLOSED", "MaintenanceTicket", String.valueOf(saved.id), "Closed ticket with outcome " + saved.closureOutcome);
        return saved;
    }
}
