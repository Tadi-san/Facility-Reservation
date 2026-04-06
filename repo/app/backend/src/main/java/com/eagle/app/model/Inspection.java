package com.eagle.app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inspections")
public class Inspection extends SyncableEntity {
    @Column(nullable = false)
    public String roomNumber;

    @Column(nullable = false)
    public Instant inspectionTime;

    @Column(nullable = false)
    public String outcome;
}
