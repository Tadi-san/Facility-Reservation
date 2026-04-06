package com.eagle.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "offline_orders")
public class OfflineOrder extends SyncableEntity {
    @Column(nullable = false, unique = true)
    public String orderNumber;

    @ManyToOne(optional = false)
    public User requester;

    @ManyToOne
    public Reservation reservation;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OfflineOrderStatus status = OfflineOrderStatus.UNPAID;

    @Column(nullable = false, unique = true)
    public String idempotencyKey;

    @Column(unique = true)
    public String paymentIdempotencyKey;

    @Column(unique = true)
    public String refundIdempotencyKey;

    public String externalReference;
    public Instant paidAt;
    public Instant refundedAt;
}
