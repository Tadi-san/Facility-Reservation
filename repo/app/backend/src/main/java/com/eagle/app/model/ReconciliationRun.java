package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun extends SyncableEntity {
    @Column(nullable = false, unique = true, length = 128)
    public String checksum;

    @Column(nullable = false)
    public int rowsProcessed;

    @Column(nullable = false)
    public int matchedRows;

    @Column(nullable = false)
    public boolean duplicatePayload;
}
