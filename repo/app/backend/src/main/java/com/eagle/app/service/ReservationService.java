package com.eagle.app.service;

import com.eagle.app.model.*;
import com.eagle.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final RoomRepository rooms;

    public ReservationService(ReservationRepository reservations, RoomRepository rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    @Transactional
    public Reservation createReservation(Reservation reservation) {
        validateRoomAvailability(reservation.room.id);
        validateNoConflict(reservation);
        return reservations.save(reservation);
    }

    public void validateNoConflict(Reservation reservation) {
        long count = reservations.countConflicts(reservation.room.id, reservation.startTime, reservation.endTime);
        if (count > 0) {
            throw new IllegalArgumentException("Room has a conflicting reservation in the selected minute range");
        }
    }

    public void validateRoomAvailability(Long roomId) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (!room.reservable) {
            throw new IllegalArgumentException(room.reservationWarning == null ? "Room is non-reservable" : room.reservationWarning);
        }
    }
}
