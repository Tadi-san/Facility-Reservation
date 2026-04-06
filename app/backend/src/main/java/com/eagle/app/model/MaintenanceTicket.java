package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "maintenance_tickets")
public class MaintenanceTicket extends SyncableEntity {
    @Column(nullable = false)
    public String title;

    @Column(nullable = false)
    public String roomNumber;

    @Column(nullable = false, length = 2000)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MaintenanceTicketStatus status = MaintenanceTicketStatus.OPEN;

    @Column(nullable = false)
    public boolean slaBreached;

    public Instant slaDueAt;
    public boolean overdue;

    public BigDecimal partsCost;
    public BigDecimal laborHours;
    public Instant initialResponseAt;
    public Instant closedAt;
    public String closureOutcome;
}
