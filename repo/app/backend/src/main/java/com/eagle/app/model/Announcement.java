package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "announcements")
public class Announcement extends SyncableEntity {
    @Column(nullable = false)
    public String title;

    @Column(nullable = false, length = 2000)
    public String message;

    @Column(nullable = false)
    public boolean published;
}
