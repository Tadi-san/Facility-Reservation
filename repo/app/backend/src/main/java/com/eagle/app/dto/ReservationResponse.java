package com.eagle.app.dto;

import com.eagle.app.model.Reservation;
import com.eagle.app.model.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(Long id, Long requesterId, String requesterUsername, Long roomId, String roomNumber,
                                  Instant startTime, Instant endTime, Instant checkedInAt, Instant checkedOutAt,
                                  ReservationStatus status, boolean roomReservable, String roomWarning) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.id, r.requester.id, r.requester.username, r.room.id, r.room.number,
                r.startTime, r.endTime, r.checkedInAt, r.checkedOutAt, r.status, r.room.reservable, r.room.reservationWarning);
    }
}
