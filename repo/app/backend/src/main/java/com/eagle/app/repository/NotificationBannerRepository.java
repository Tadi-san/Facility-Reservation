package com.eagle.app.repository;

import com.eagle.app.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface NotificationBannerRepository extends JpaRepository<NotificationBanner, Long> {
    boolean existsByReservation_IdAndTemplateKey(Long reservationId, String templateKey);
    List<NotificationBanner> findByStatusAndScheduledForLessThanEqual(BannerStatus status, Instant time);
    List<NotificationBanner> findByStatusInOrderByScheduledForDesc(List<BannerStatus> statuses);
}
