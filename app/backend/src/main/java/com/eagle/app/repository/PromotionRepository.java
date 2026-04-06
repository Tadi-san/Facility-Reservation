package com.eagle.app.repository;

import com.eagle.app.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);
}
