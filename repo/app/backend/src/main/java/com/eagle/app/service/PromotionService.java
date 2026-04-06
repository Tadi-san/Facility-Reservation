package com.eagle.app.service;

import com.eagle.app.model.Promotion;
import com.eagle.app.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class PromotionService {
    private final PromotionRepository promotions;

    public PromotionService(PromotionRepository promotions) {
        this.promotions = promotions;
    }

    public BigDecimal applyDiscount(String code, BigDecimal amount, Instant when) {
        Promotion p = promotions.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Invalid promo code"));
        if (!p.active || when.isBefore(p.startsAt) || when.isAfter(p.endsAt)) {
            throw new IllegalArgumentException("Promotion is not active for this order time");
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(p.percentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
