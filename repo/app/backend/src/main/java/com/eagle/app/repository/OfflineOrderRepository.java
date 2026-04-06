package com.eagle.app.repository;

import com.eagle.app.model.OfflineOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfflineOrderRepository extends JpaRepository<OfflineOrder, Long> {
    Optional<OfflineOrder> findByOrderNumber(String orderNumber);
    Optional<OfflineOrder> findByIdempotencyKey(String idempotencyKey);
    Optional<OfflineOrder> findByPaymentIdempotencyKey(String idempotencyKey);
    Optional<OfflineOrder> findByRefundIdempotencyKey(String idempotencyKey);
}
