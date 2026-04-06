package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "metric_templates")
public class MetricTemplate extends SyncableEntity {
    @Column(nullable = false)
    public String metricKey;

    @Column(nullable = false)
    public Integer version;

    @Column(nullable = false, length = 4000)
    public String definition;

    @Column(nullable = false)
    public LocalDate effectiveFrom;

    public LocalDate effectiveTo;

    @Column(nullable = false, length = 4000)
    public String weightedDimensionsJson;
}
