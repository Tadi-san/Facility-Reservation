package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notification_jobs")
public class NotificationJob extends SyncableEntity {
    @Column(nullable = false)
    public String jobType;

    @Column(nullable = false, length = 4000)
    public String payloadJson = "{}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public NotificationJobStatus status = NotificationJobStatus.PENDING;

    @Column(nullable = false)
    public int attempts = 0;

    public Instant nextAttemptAt = Instant.now();
    public String lastError;
}
