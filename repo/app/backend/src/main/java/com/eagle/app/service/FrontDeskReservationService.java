package com.eagle.app.service;

import com.eagle.app.model.*;
import com.eagle.app.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FrontDeskReservationService {
    private final ReservationRepository reservations;

    public FrontDeskReservationService(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Transactional
    public Reservation confirmArrival(Long id) {
        Reservation r = reservations.findById(id).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (r.status != ReservationStatus.PENDING && r.status != ReservationStatus.APPROVED) {
            throw new IllegalArgumentException("Only pending or approved reservations can be checked in");
        }
        r.status = ReservationStatus.CHECKED_IN;
        r.checkedInAt = Instant.now();
        return reservations.save(r);
    }

    @Transactional
    public Reservation processCheckout(Long id) {
        Reservation r = reservations.findById(id).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (r.status != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Only checked-in reservations can be completed");
        }
        r.status = ReservationStatus.COMPLETED;
        r.checkedOutAt = Instant.now();
        return reservations.save(r);
    }
}
