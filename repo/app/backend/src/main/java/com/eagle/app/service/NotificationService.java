package com.eagle.app.service;

import com.eagle.app.model.*;
import com.eagle.app.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {
    private final ReservationRepository reservations;
    private final NotificationBannerRepository banners;
    private final BannerTemplateRepository templates;

    public NotificationService(ReservationRepository reservations, NotificationBannerRepository banners, BannerTemplateRepository templates) {
        this.reservations = reservations;
        this.banners = banners;
        this.templates = templates;
    }

    public int queueTemplateBanners() {
        Instant now = Instant.now();
        BannerTemplate arrival = templates.findByTemplateKeyAndActiveTrue("ARRIVAL_30M").orElseThrow(() -> new IllegalStateException("Missing active banner template: ARRIVAL_30M"));
        BannerTemplate checkout = templates.findByTemplateKeyAndActiveTrue("CHECKOUT_10M").orElseThrow(() -> new IllegalStateException("Missing active banner template: CHECKOUT_10M"));
        int created = 0;

        for (Reservation r : reservations.findArrivalsBetween(now, now.plus(arrival.minutesBefore, ChronoUnit.MINUTES))) {
            created += queueIfMissing(r, arrival.templateKey, arrival.message, r.startTime.minus(arrival.minutesBefore, ChronoUnit.MINUTES));
        }
        for (Reservation r : reservations.findCheckoutsBetween(now, now.plus(checkout.minutesBefore, ChronoUnit.MINUTES))) {
            created += queueIfMissing(r, checkout.templateKey, checkout.message, r.endTime.minus(checkout.minutesBefore, ChronoUnit.MINUTES));
        }
        return created;
    }

    public int processPendingBanners() {
        List<NotificationBanner> due = banners.findByStatusAndScheduledForLessThanEqual(BannerStatus.PENDING, Instant.now());
        due.forEach(b -> b.status = BannerStatus.PROCESSED);
        banners.saveAll(due);
        return due.size();
    }

    public List<NotificationBanner> activeAndHistoricalBanners() {
        return banners.findByStatusInOrderByScheduledForDesc(List.of(BannerStatus.PROCESSED, BannerStatus.DISMISSED));
    }

    public NotificationBanner dismiss(Long id) {
        NotificationBanner b = banners.findById(id).orElseThrow(() -> new IllegalArgumentException("Notification banner not found"));
        b.status = BannerStatus.DISMISSED;
        b.dismissedAt = Instant.now();
        return banners.save(b);
    }

    private int queueIfMissing(Reservation r, String key, String msg, Instant at) {
        if (banners.existsByReservation_IdAndTemplateKey(r.id, key)) return 0;
        NotificationBanner n = new NotificationBanner();
        n.reservation = r;
        n.targetUser = r.requester;
        n.templateKey = key;
        n.message = msg;
        n.scheduledFor = at;
        n.status = BannerStatus.PENDING;
        banners.save(n);
        return 1;
    }
}
