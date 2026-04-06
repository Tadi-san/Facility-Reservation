package com.eagle.app.service;

import com.eagle.app.dto.OfflineOrderCreateRequest;
import com.eagle.app.dto.OfflineOrderPaymentRequest;
import com.eagle.app.dto.OfflineOrderRefundRequest;
import com.eagle.app.model.OfflineOrder;
import com.eagle.app.model.OfflineOrderStatus;
import com.eagle.app.model.ReconciliationRun;
import com.eagle.app.model.RoleName;
import com.eagle.app.model.User;
import com.eagle.app.repository.OfflineOrderRepository;
import com.eagle.app.repository.ReconciliationRunRepository;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OfflineOrderService {
    private final OfflineOrderRepository orders;
    private final UserRepository users;
    private final ReservationRepository reservations;
    private final ReconciliationRunRepository runs;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public OfflineOrderService(OfflineOrderRepository orders,
                               UserRepository users,
                               ReservationRepository reservations,
                               ReconciliationRunRepository runs,
                               CurrentUserService currentUserService,
                               AuditLogService auditLogService) {
        this.orders = orders;
        this.users = users;
        this.reservations = reservations;
        this.runs = runs;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OfflineOrder create(OfflineOrderCreateRequest req) {
        Optional<OfflineOrder> existing = orders.findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) return existing.get();

        User actor = currentUserService.requireCurrentUser();
        User requester;
        if (req.requesterId() != null && currentUserService.hasAnyRole(actor, RoleName.OPS, RoleName.ADMIN)) {
            requester = users.findById(req.requesterId()).orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        } else {
            requester = actor;
        }

        OfflineOrder order = new OfflineOrder();
        order.orderNumber = "OFF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.requester = requester;
        order.amount = req.amount();
        order.status = OfflineOrderStatus.UNPAID;
        order.idempotencyKey = req.idempotencyKey();
        if (req.reservationId() != null) {
            var reservation = reservations.findById(req.reservationId()).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
            boolean privileged = currentUserService.hasAnyRole(actor, RoleName.OPS, RoleName.ADMIN);
            if (!privileged && (reservation.requester == null || !reservation.requester.id.equals(actor.id))) {
                throw new IllegalArgumentException("You can only attach orders to your own reservations");
            }
            order.reservation = reservation;
        }
        OfflineOrder saved = orders.save(order);
        auditLogService.log(actor, "OFFLINE_ORDER_CREATED", "OfflineOrder", String.valueOf(saved.id), "Created offline order " + saved.orderNumber);
        return saved;
    }

    @Transactional
    public OfflineOrder markPaid(Long id, OfflineOrderPaymentRequest req) {
        OfflineOrder existingByKey = orders.findByPaymentIdempotencyKey(req.idempotencyKey()).orElse(null);
        if (existingByKey != null && existingByKey.status == OfflineOrderStatus.PAID) {
            return existingByKey;
        }
        OfflineOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.status == OfflineOrderStatus.REFUNDED) {
            throw new IllegalArgumentException("Cannot mark refunded order as paid");
        }
        order.status = OfflineOrderStatus.PAID;
        order.paidAt = Instant.now();
        order.externalReference = req.externalReference();
        order.paymentIdempotencyKey = req.idempotencyKey();
        OfflineOrder saved = orders.save(order);
        auditLogService.log(currentUserService.requireCurrentUser(), "OFFLINE_ORDER_PAID", "OfflineOrder", String.valueOf(saved.id), "Order marked paid");
        return saved;
    }

    @Transactional
    public OfflineOrder refund(Long id, OfflineOrderRefundRequest req) {
        OfflineOrder existingByKey = orders.findByRefundIdempotencyKey(req.idempotencyKey()).orElse(null);
        if (existingByKey != null && existingByKey.status == OfflineOrderStatus.REFUNDED) {
            return existingByKey;
        }
        OfflineOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.status != OfflineOrderStatus.PAID) {
            throw new IllegalArgumentException("Only paid orders can be refunded");
        }
        order.status = OfflineOrderStatus.REFUNDED;
        order.refundedAt = Instant.now();
        order.refundIdempotencyKey = req.idempotencyKey();
        OfflineOrder saved = orders.save(order);
        auditLogService.log(currentUserService.requireCurrentUser(), "OFFLINE_ORDER_REFUNDED", "OfflineOrder", String.valueOf(saved.id), "Order refunded");
        return saved;
    }

    @Transactional
    public Map<String, Object> reconcile(String csvPayload) {
        String checksum = checksum(csvPayload);
        if (runs.findByChecksum(checksum).isPresent()) {
            return Map.of("checksum", checksum, "duplicate", true, "processed", 0, "matched", 0);
        }

        ReconciliationRun run = new ReconciliationRun();
        run.checksum = checksum;
        int processed = 0;
        int matched = 0;
        String[] lines = csvPayload.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.toLowerCase().startsWith("ordernumber,")) continue;
            processed++;
            String[] cols = trimmed.split(",");
            if (cols.length < 3) continue;
            String orderNumber = cols[0].trim();
            String status = cols[2].trim().toUpperCase();
            OfflineOrder order = orders.findByOrderNumber(orderNumber).orElse(null);
            if (order == null) continue;
            matched++;
            if ("PAID".equals(status)) {
                order.status = OfflineOrderStatus.PAID;
                order.paidAt = order.paidAt == null ? Instant.now() : order.paidAt;
            } else if ("REFUNDED".equals(status)) {
                order.status = OfflineOrderStatus.REFUNDED;
                order.refundedAt = order.refundedAt == null ? Instant.now() : order.refundedAt;
            } else {
                order.status = OfflineOrderStatus.UNPAID;
            }
            orders.save(order);
        }

        run.duplicatePayload = false;
        run.rowsProcessed = processed;
        run.matchedRows = matched;
        runs.save(run);
        auditLogService.log(currentUserService.requireCurrentUser(), "OFFLINE_RECONCILIATION_RUN", "ReconciliationRun", String.valueOf(run.id), "Processed " + processed + " rows");
        Map<String, Object> result = new HashMap<>();
        result.put("checksum", checksum);
        result.put("duplicate", false);
        result.put("processed", processed);
        result.put("matched", matched);
        return result;
    }

    private String checksum(String csvPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(csvPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to checksum reconciliation payload", ex);
        }
    }
}
