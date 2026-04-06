package com.eagle.app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shift_handoffs")
public class ShiftHandoff extends SyncableEntity {
    @ManyToOne(optional = false)
    public User fromUser;

    @ManyToOne(optional = false)
    public User toUser;

    @Column(nullable = false)
    public Instant handoffTime;

    @Column(nullable = false, length = 2000)
    public String summary;

    @Column(length = 4000)
    public String pendingTasks;
}
