package com.eagle.app.dto;

import com.eagle.app.model.OfflineOrder;
import com.eagle.app.model.OfflineOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OfflineOrderResponse(Long id,
                                   String orderNumber,
                                   String requesterUsername,
                                   BigDecimal amount,
                                   OfflineOrderStatus status,
                                   String externalReference,
                                   Instant paidAt,
                                   Instant refundedAt) {
    public static OfflineOrderResponse from(OfflineOrder order) {
        return new OfflineOrderResponse(
                order.id,
                order.orderNumber,
                order.requester == null ? "" : order.requester.username,
                order.amount,
                order.status,
                order.externalReference,
                order.paidAt,
                order.refundedAt
        );
    }
}
