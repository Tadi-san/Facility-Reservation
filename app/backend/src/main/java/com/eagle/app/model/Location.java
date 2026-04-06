package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "locations")
public class Location extends SyncableEntity {
    @Column(nullable = false, unique = true)
    public String code;

    @Column(nullable = false)
    public String name;

    public String address;
}
