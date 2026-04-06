package com.eagle.app.controller;

import com.eagle.app.dto.OfflineOrderCreateRequest;
import com.eagle.app.dto.OfflineOrderPaymentRequest;
import com.eagle.app.dto.OfflineOrderRefundRequest;
import com.eagle.app.dto.OfflineOrderResponse;
import com.eagle.app.dto.OfflineReconciliationRequest;
import com.eagle.app.dto.PromotionApplyRequest;
import com.eagle.app.model.OfflineOrder;
import com.eagle.app.repository.OfflineOrderRepository;
import com.eagle.app.service.OfflineOrderService;
import com.eagle.app.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {
    private final PromotionService promotions;
    private final OfflineOrderService offlineOrders;
    private final OfflineOrderRepository offlineOrderRepository;

    public FinanceController(PromotionService promotions, OfflineOrderService offlineOrders, OfflineOrderRepository offlineOrderRepository) {
        this.promotions = promotions;
        this.offlineOrders = offlineOrders;
        this.offlineOrderRepository = offlineOrderRepository;
    }

    @PostMapping("/promotions/apply")
    public Map<String, BigDecimal> apply(@Valid @RequestBody PromotionApplyRequest req) {
        return Map.of("finalAmount", promotions.applyDiscount(req.code(), req.orderAmount(), req.orderTime()));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('OPS','ADMIN')")
    public Page<OfflineOrderResponse> listOrders(Pageable pageable) {
        return offlineOrderRepository.findAll(pageable).map(OfflineOrderResponse::from);
    }

    @PostMapping("/orders")
    public OfflineOrderResponse createOrder(@Valid @RequestBody OfflineOrderCreateRequest req) {
        OfflineOrder saved = offlineOrders.create(req);
        return OfflineOrderResponse.from(saved);
    }

    @PostMapping("/orders/{id}/pay")
    @PreAuthorize("hasAnyRole('OPS','ADMIN')")
    public OfflineOrderResponse markPaid(@PathVariable Long id, @Valid @RequestBody OfflineOrderPaymentRequest req) {
        return OfflineOrderResponse.from(offlineOrders.markPaid(id, req));
    }

    @PostMapping("/orders/{id}/refund")
    @PreAuthorize("hasAnyRole('OPS','ADMIN')")
    public OfflineOrderResponse refund(@PathVariable Long id, @Valid @RequestBody OfflineOrderRefundRequest req) {
        return OfflineOrderResponse.from(offlineOrders.refund(id, req));
    }

    @PostMapping("/orders/reconcile")
    @PreAuthorize("hasAnyRole('OPS','ADMIN')")
    public Map<String, Object> reconcile(@Valid @RequestBody OfflineReconciliationRequest req) {
        return offlineOrders.reconcile(req.csvPayload());
    }
}
