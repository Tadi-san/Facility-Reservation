package com.eagle.app.repository;

import com.eagle.app.model.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query(value = """
            SELECT COUNT(*)
            FROM reservations r
            WHERE r.room_id = :roomId
              AND r.status IN ('PENDING','APPROVED')
              AND date_trunc('minute', r.start_time) < date_trunc('minute', CAST(:endTime AS timestamptz))
              AND date_trunc('minute', r.end_time) > date_trunc('minute', CAST(:startTime AS timestamptz))
            """, nativeQuery = true)
    long countConflicts(@Param("roomId") Long roomId,
                        @Param("startTime") Instant startTime,
                        @Param("endTime") Instant endTime);

    List<Reservation> findByRequester_UsernameOrderByStartTimeDesc(String username);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status IN ('PENDING','APPROVED')
          AND r.startTime BETWEEN :from AND :to
    """)
    List<Reservation> findArrivalsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status IN ('PENDING','APPROVED','CHECKED_IN')
          AND r.endTime BETWEEN :from AND :to
    """)
    List<Reservation> findCheckoutsBetween(@Param("from") Instant from, @Param("to") Instant to);
}
