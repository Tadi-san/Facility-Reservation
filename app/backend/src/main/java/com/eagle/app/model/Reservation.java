package com.eagle.app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation extends SyncableEntity {
    @ManyToOne(optional = false)
    public User requester;

    @ManyToOne(optional = false)
    public Room room;

    @Column(nullable = false)
    public Instant startTime;

    @Column(nullable = false)
    public Instant endTime;

    public Instant checkedInAt;
    public Instant checkedOutAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ReservationStatus status = ReservationStatus.PENDING;
}
