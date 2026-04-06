package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotions")
public class Promotion extends SyncableEntity {
    @NotBlank
    @Column(nullable = false, unique = true)
    public String code;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("100.0")
    @Column(nullable = false)
    public BigDecimal percentage;

    @NotBlank
    @Column(nullable = false)
    public String promotionType;

    @NotNull
    @Column(nullable = false)
    public Instant startsAt;

    @NotNull
    @Column(nullable = false)
    public Instant endsAt;

    @Column(nullable = false)
    public boolean active = true;
}
