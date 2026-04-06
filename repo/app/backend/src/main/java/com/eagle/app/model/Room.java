package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room extends SyncableEntity {
    @ManyToOne(optional = false)
    public Location location;

    @ManyToOne(optional = false)
    public RoomType roomType;

    @Column(nullable = false)
    public String number;

    public Integer floorNumber;

    @Column(nullable = false)
    public Integer capacity = 1;

    @Column(nullable = false)
    public boolean reservable = true;

    @Column(length = 512)
    public String reservationWarning;
}
