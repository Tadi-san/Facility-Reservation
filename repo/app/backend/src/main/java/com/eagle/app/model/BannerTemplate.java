package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "banner_templates")
public class BannerTemplate extends SyncableEntity {
    @Column(nullable = false, unique = true)
    public String templateKey;

    @Column(nullable = false)
    public Integer minutesBefore;

    @Column(nullable = false, length = 512)
    public String message;

    @Column(nullable = false)
    public boolean active = true;
}
