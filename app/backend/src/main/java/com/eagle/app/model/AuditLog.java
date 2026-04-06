package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends SyncableEntity {
    @Column(nullable = false)
    public Instant eventAt = Instant.now();

    @Column(nullable = false)
    public String actorUsername;

    @Column(nullable = false)
    public String action;

    @Column(nullable = false)
    public String resourceType;

    @Column(nullable = false)
    public String resourceId;

    @Column(nullable = false, length = 4000)
    public String details;

    @Column(nullable = false, length = 128)
    public String previousHash;

    @Column(nullable = false, length = 128, unique = true)
    public String recordHash;
}
