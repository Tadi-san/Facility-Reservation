package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_types")
public class RoomType extends SyncableEntity {
    @Column(nullable = false, unique = true)
    public String name;

    public String description;
}
