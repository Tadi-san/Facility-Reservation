package com.eagle.app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_banners")
public class NotificationBanner extends SyncableEntity {
    @ManyToOne(optional = false)
    public Reservation reservation;

    @ManyToOne(optional = false)
    public User targetUser;

    @Column(nullable = false)
    public String templateKey;

    @Column(nullable = false, length = 512)
    public String message;

    @Column(nullable = false)
    public Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public BannerStatus status = BannerStatus.PENDING;

    public Instant dismissedAt;
}
