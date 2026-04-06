package com.eagle.app.dto;

import com.eagle.app.model.NotificationBanner;
import java.time.Instant;

public record NotificationBannerResponse(Long id, Long reservationId, String roomNumber, String requesterUsername,
                                         String templateKey, String message, Instant scheduledFor, String status,
                                         Instant dismissedAt) {
    public static NotificationBannerResponse from(NotificationBanner n) {
        return new NotificationBannerResponse(n.id, n.reservation.id, n.reservation.room.number, n.targetUser.username,
                n.templateKey, n.message, n.scheduledFor, n.status.name(), n.dismissedAt);
    }
}
